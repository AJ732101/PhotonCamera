import os
import re
import json
import http.server
import socketserver
import threading
import subprocess
import urllib.parse
import webview
from webview import FileDialog

PORT = 8080
server_httpd = None


def start_local_server(directory):
    global server_httpd
    
    class CustomHDRHandler(http.server.SimpleHTTPRequestHandler):
        def __init__(self, *args, **kwargs):
            super().__init__(*args, directory=directory, **kwargs)
            
        def log_message(self, format, *args):
            pass

        def do_GET(self):
            parsed_url = urllib.parse.urlparse(self.path)
            clean_path = parsed_url.path
            
            # Server-side route to extract the Gain Map on-the-fly
            if clean_path.startswith("/get_gainmap/"):
                filename = urllib.parse.unquote(clean_path[13:])
                img_path = os.path.join(directory, filename)
                
                if os.path.exists(img_path):
                    try:
                        startupinfo = subprocess.STARTUPINFO()
                        startupinfo.dwFlags |= subprocess.STARTF_USESHOWWINDOW
                        
                        # 1. Try native Android 14/15 Gain Map tag
                        cmd = ["exiftool", "-b", "-GainMapImage", img_path]
                        res = subprocess.run(cmd, capture_output=True, startupinfo=startupinfo)
                        
                        # Fallback for MPF / MPF2 tags
                        if not res.stdout or len(res.stdout) < 100:
                            cmd = ["exiftool", "-b", "-mpf2", img_path]
                            res = subprocess.run(cmd, capture_output=True, startupinfo=startupinfo)
                            
                        if res.stdout and len(res.stdout) > 100:
                            self.send_response(200)
                            self.send_header("Content-Type", "image/jpeg")
                            self.send_header("Content-Length", str(len(res.stdout)))
                            self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
                            self.end_headers()
                            self.wfile.write(res.stdout)
                            return
                    except Exception as e:
                        print(f"Server extraction error for {filename}: {e}")
                
                self.send_error(404, "Gain Map not found or not extractable")
                return
                
            return super().do_GET()

    socketserver.TCPServer.allow_reuse_address = True
    server_httpd = socketserver.TCPServer(("127.0.0.1", PORT), CustomHDRHandler)
    server_httpd.serve_forever()


def get_all_exif_data(directory, files):
    exif_dict = {}
    if not files:
        return exif_dict

    try:
        startupinfo = subprocess.STARTUPINFO()
        startupinfo.dwFlags |= subprocess.STARTF_USESHOWWINDOW
        
        # New Baseline tags + check tags for Gain Map presence
        cmd = ["exiftool", "-j", "-Make", "-Model", "-ISO", "-FNumber", "-Aperture", 
               "-ExposureTime", "-ShutterSpeed", "-FocalLengthIn35mmFilm", "-FocalLength", "-ScaleFactor35efl",
               "-GainMapImage", "-mpf2"]
        
        paths = [os.path.join(directory, f) for f in files]
        cmd.extend(paths)
        
        result = subprocess.run(cmd, capture_output=True, text=True, startupinfo=startupinfo)
        if not result.stdout.strip():
            return exif_dict
            
        raw_entries = json.loads(result.stdout)
        
        for d in raw_entries:
            src_path = d.get("SourceFile", "")
            fname = os.path.basename(src_path) if src_path else ""
            if not fname:
                continue

            # Check if either native GainMapImage or MPF2 fallback tags contain data
            has_gainmap = "True" if (d.get("GainMapImage") or d.get("mpf2")) else "False"

            aperture_val = d.get("FNumber") or d.get("Aperture")
            if aperture_val:
                aperture_str = str(aperture_val)
                if not aperture_str.lower().startswith("F/"):
                    aperture_str = f"F/{aperture_str}"
            else:
                aperture_str = "-"
            
            shutter_str = "-"
            exp_time = d.get("ExposureTime")
            shut_speed = d.get("ShutterSpeed")
            
            if exp_time is not None:
                try:
                    val = float(eval(str(exp_time)))
                    if val > 0:
                        if val < 1.0:
                            den = round(1.0 / val)
                            shutter_str = f"1/{den}s"
                        else:
                            shutter_str = f"{round(val, 1)}s"
                except:
                    shutter_str = str(exp_time) + "s"
            elif shut_speed is not None:
                shutter_str = str(shut_speed)
                if not shutter_str.endswith("s"):
                    shutter_str += "s"

            focal_len = d.get("FocalLength")
            focal_35mm = d.get("FocalLengthIn35mmFilm")
            scale_factor = d.get("ScaleFactor35efl")
            
            focal_len_val = None
            if focal_len:
                try:
                    focal_len_val = float(str(focal_len).replace("mm", "").strip())
                except:
                    pass

            f35_val = None
            if focal_35mm:
                try:
                    f35_val = int(float(str(focal_35mm).replace("mm", "").strip()))
                except:
                    pass
            elif focal_len_val and scale_factor:
                try:
                    f35_val = int(round(focal_len_val * float(scale_factor)))
                except:
                    pass

            focal_str = f"{focal_len_val:.1f}mm" if focal_len_val else "-"
            focal_35mm_str = f"{f35_val}mm" if f35_val else "-"

            exif_dict[fname] = {
                "filename": fname,
                "manufacturer": d.get("Make", "-").strip(),
                "model": d.get("Model", "-").strip(),
                "iso": str(d.get("ISO", "-")),
                "aperture": aperture_str,
                "shutter": shutter_str,
                "focal_length": focal_str,
                "focal_35mm": focal_35mm_str,
                "ultra_hdr": has_gainmap
            }
            
    except Exception as e:
        print(f"Metadata scanning error: {e}")
        
    return exif_dict


def main():
    def open_file_and_show():
        file_types = ["Image files (*.jpg;*.jpeg)"]

        selected_files = window.create_file_dialog(
            FileDialog.OPEN, allow_multiple=False, file_types=file_types
        )

        if selected_files and len(selected_files) > 0:
            file_path = os.path.abspath(selected_files[0])
            file_dir = os.path.dirname(file_path)
            current_file_name = os.path.basename(file_path)

            all_files = os.listdir(file_dir)
            valid_extensions = (".jpg", ".jpeg")
            image_files = [
                f for f in all_files if f.lower().endswith(valid_extensions)
            ]
            
            image_files.sort(key=lambda var: [int(x) if x.isdigit() else x.lower() for x in re.split(r'(\d+)', var)])

            exif_dict = get_all_exif_data(file_dir, image_files)

            try:
                start_index = image_files.index(current_file_name)
            except ValueError:
                image_files.insert(0, current_file_name)
                start_index = 0
                exif_dict[current_file_name] = get_all_exif_data(file_dir, [current_file_name]).get(current_file_name, {})

            images_json = json.dumps(image_files)
            exif_json = json.dumps(exif_dict)

            html_content = f"""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    * {{ margin: 0; padding: 0; box-sizing: border-box; user-select: none; }}
                    body {{ background-color: #121212; display: flex; justify-content: center; align-items: center; min-height: 100vh; overflow: hidden; font-family: sans-serif; }}
                    
                    .viewer-container {{ position: relative; width: 100vw; height: 100vh; display: flex; justify-content: center; align-items: center; overflow: hidden; }}
                    
                    img {{ 
                        max-width: 100%; 
                        max-height: 100vh; 
                        object-fit: contain; 
                        color-profile: display-p3; 
                        image-rendering: high-quality; 
                        dynamic-range-limit: high;
                        cursor: zoom-in;
                        transition: transform 0.15s ease-out;
                    }}
                    
                    img.zoomed {{
                        cursor: zoom-out;
                    }}
                    
                    img.sdr-mode {{ 
                        dynamic-range-limit: standard !important; 
                    }}
                    img.gain-map-mode {{ 
                        dynamic-range-limit: standard !important; 
                        filter: grayscale(1) !important; 
                    }}
                    
                    .nav-btn {{ position: absolute; top: 50%; transform: translateY(-50%); width: 60px; height: 60px; background-color: rgba(30, 30, 30, 0.4); color: #ffffff; border: 1px solid rgba(255, 255, 255, 0.2); border-radius: 50%; font-size: 24px; cursor: pointer; display: flex; justify-content: center; align-items: center; transition: all 0.2s ease; backdrop-filter: blur(5px); z-index: 10; }}
                    .nav-btn:hover {{ background-color: rgba(60, 60, 60, 0.8); border-color: rgba(255, 255, 255, 0.6); scale: 1.05; }}
                    .nav-btn:active {{ scale: 0.95; }}
                    .prev-btn {{ left: 20px; }}
                    .next-btn {{ right: 20px; }}
                    
                    .controls-panel {{
                        position: absolute;
                        top: 20px;
                        left: 20px;
                        display: flex;
                        flex-direction: column;
                        gap: 10px;
                        z-index: 20;
                    }}
                    
                    .toggle-btn {{ 
                        height: 40px; 
                        padding: 0 20px; 
                        background-color: rgba(30, 30, 30, 0.4); 
                        color: #ffffff; 
                        border: 1px solid rgba(255, 255, 255, 0.2); 
                        border-radius: 20px; 
                        font-size: 14px; 
                        font-weight: bold; 
                        cursor: pointer; 
                        display: flex; 
                        justify-content: center; 
                        align-items: center; 
                        transition: all 0.2s ease; 
                        backdrop-filter: blur(5px); 
                    }}
                    .toggle-btn:hover {{ background-color: rgba(60, 60, 60, 0.8); border-color: rgba(255, 255, 255, 0.6); }}
                    .toggle-btn.active {{ background-color: rgba(230, 90, 10, 0.6); border-color: rgba(255, 255, 255, 0.4); }}
                    .toggle-btn.gain-map-active {{ background-color: rgba(0, 160, 230, 0.6); border-color: rgba(255, 255, 255, 0.4); }}
                    
                    .info-panel {{ position: absolute; top: 20px; right: 20px; background-color: rgba(20, 20, 20, 0.6); color: #e0e0e0; border: 1px solid rgba(255, 255, 255, 0.15); border-radius: 10px; padding: 12px 16px; font-size: 13px; line-height: 1.5; font-family: monospace; backdrop-filter: blur(6px); z-index: 20; min-width: 320px; pointer-events: none; }}
                    .info-title {{ font-weight: bold; color: #ffffff; margin-bottom: 6px; font-family: sans-serif; word-break: break-all; border-bottom: 1px solid rgba(255, 255, 255, 0.1); padding-bottom: 4px; }}
                    .info-row {{ display: flex; justify-content: space-between; margin-top: 2px; }}
                    .info-label {{ color: #888888; margin-right: 15px; }}
                    .info-val {{ color: #ffffff; text-align: right; }}
                    .info-val.hdr-true {{ color: #00ff66; font-weight: bold; }}
                    .info-val.hdr-false {{ color: #ff3333; }}
                </style>
            </head>
            <body>
                <div class="viewer-container" id="container">
                    <div class="controls-panel">
                        <button id="mode-toggle" class="toggle-btn active" onclick="toggleMode()">HDR Mode</button>
                        <button id="gain-map-toggle" class="toggle-btn" onclick="toggleGainMapMode()">Gain Map</button>
                    </div>
                    
                    <button class="nav-btn prev-btn" onclick="prevImage()">&#10094;</button>
                    <img id="hdr-image" src="" alt="Ultra HDR Image">
                    <button class="nav-btn next-btn" onclick="nextImage()">&#10095;</button>
                    
                    <div class="info-panel">
                        <div id="meta-filename" class="info-title">-</div>
                        <div class="info-row"><span class="info-label">Ultra HDR:</span><span id="meta-uhdr" class="info-val">-</span></div>
                        <div class="info-row"><span class="info-label">Camera:</span><span id="meta-cam" class="info-val">-</span></div>
                        <div class="info-row"><span class="info-label">ISO:</span><span id="meta-iso" class="info-val">-</span></div>
                        <div class="info-row"><span class="info-label">Aperture:</span><span id="meta-aperture" class="info-val">-</span></div>
                        <div class="info-row"><span class="info-label">Shutter Speed:</span><span id="meta-shutter" class="info-val">-</span></div>
                        <div class="info-row"><span class="info-label">Focal Length:</span><span id="meta-focal" class="info-val">-</span></div>
                        <div class="info-row"><span class="info-label">Focal Length (35mm):</span><span id="meta-focal35" class="info-val">-</span></div>
                    </div>
                </div>
                <script>
                    const images = {images_json};
                    const exifData = {exif_json};
                    let currentIndex = {start_index};
                    let isHDR = true;
                    let isGainMapMode = false;
                    let isZoomed = false;

                    const img = document.getElementById('hdr-image');
                    const hdrBtn = document.getElementById('mode-toggle');
                    const gmBtn = document.getElementById('gain-map-toggle');

                    // Native 1:1 Zoom-Logik
                    img.addEventListener('dblclick', function(e) {{
                        if (!isZoomed) {{
                            const rect = img.getBoundingClientRect();
                            const x = ((e.clientX - rect.left) / rect.width) * 100;
                            const y = ((e.clientY - rect.top) / rect.height) * 100;
                            
                            img.style.transformOrigin = `${{x}}% ${{y}}%`;
                            img.style.transform = 'scale(3)';
                            img.classList.add('zoomed');
                            isZoomed = true;
                        }} else {{
                            resetZoom();
                        }}
                    }});

                    function resetZoom() {{
                        img.style.transform = 'scale(1)';
                        img.style.transformOrigin = 'center center';
                        img.classList.remove('zoomed');
                        isZoomed = false;
                    }}

                    function updateViewer() {{
                        if (images.length > 0 && currentIndex >= 0 && currentIndex < images.length) {{
                            isGainMapMode = false; 
                            isHDR = true;
                            resetZoom();

                            const fileName = images[currentIndex];
                            img.src = 'http://127.0.0.1:' + window.location.port + '/' + encodeURIComponent(fileName);

                            document.title = "Ultra HDR Viewer - " + fileName;
                            
                            const meta = exifData[fileName] || {{}};
                            document.getElementById('meta-filename').innerText = meta.filename || fileName;
                            
                            const uhdrEl = document.getElementById('meta-uhdr');
                            uhdrEl.innerText = meta.ultra_hdr || "False";
                            uhdrEl.className = "info-val " + (meta.ultra_hdr === "True" ? "hdr-true" : "hdr-false");

                            let make = meta.manufacturer || "-";
                            let model = meta.model || "-";
                            if (model.toLowerCase().startsWith(make.toLowerCase())) {{
                                document.getElementById('meta-cam').innerText = model;
                            }} else {{
                                document.getElementById('meta-cam').innerText = (make !== "-" || model !== "-") ? make + " " + model : "-";
                            }}
                            
                            document.getElementById('meta-iso').innerText = meta.iso || "-";
                            document.getElementById('meta-aperture').innerText = meta.aperture || "-";
                            document.getElementById('meta-shutter').innerText = meta.shutter || "-";
                            document.getElementById('meta-focal').innerText = meta.focal_length || "-";
                            document.getElementById('meta-focal35').innerText = meta.focal_35mm || "-";
                            
                            img.classList.remove('sdr-mode', 'gain-map-mode');
                            updateButtons();
                        }}
                    }}

                    function toggleMode() {{
                        const fileName = images[currentIndex];
                        if (isGainMapMode) {{
                            isGainMapMode = false;
                            isHDR = true;
                        }} else {{
                            isHDR = !isHDR;
                        }}
                        
                        img.classList.remove('sdr-mode', 'gain-map-mode');
                        if (!isHDR) img.classList.add('sdr-mode');
                        
                        img.src = 'http://127.0.0.1:' + window.location.port + '/' + encodeURIComponent(fileName) + '?t=' + new Date().getTime();
                        updateButtons();
                    }}
                    
                    function toggleGainMapMode() {{
                        const fileName = images[currentIndex];
                        
                        if (!isGainMapMode) {{
                            isGainMapMode = true;
                            isHDR = false;
                            
                            img.classList.remove('sdr-mode');
                            img.classList.add('gain-map-mode');
                            
                            img.src = 'http://127.0.0.1:' + window.location.port + '/get_gainmap/' + encodeURIComponent(fileName) + '?t=' + new Date().getTime();
                        }} else {{
                            isGainMapMode = false;
                            isHDR = true;
                            img.classList.remove('sdr-mode', 'gain-map-mode');
                            img.src = 'http://127.0.0.1:' + window.location.port + '/' + encodeURIComponent(fileName) + '?t=' + new Date().getTime();
                        }}
                        updateButtons();
                    }}
                    
                    function updateButtons() {{
                        hdrBtn.classList.remove('active');
                        gmBtn.classList.remove('gain-map-active');
                        
                        if (isGainMapMode) {{
                            gmBtn.classList.add('gain-map-active');
                            hdrBtn.innerText = "SDR Mode";
                        }} else if (isHDR) {{
                            hdrBtn.classList.add('active');
                            hdrBtn.innerText = "HDR Mode";
                        }} else {{
                            hdrBtn.innerText = "SDR Mode";
                        }}
                    }}

                    function nextImage() {{
                        if (currentIndex < images.length - 1) {{
                            currentIndex++;
                            updateViewer();
                        }}
                    }}

                    function prevImage() {{
                        if (currentIndex > 0) {{
                            currentIndex--;
                            updateViewer();
                        }}
                    }}

                    window.addEventListener('keydown', function(e) {{
                        if (e.key === "ArrowRight") nextImage();
                        if (e.key === "ArrowLeft") prevImage();
                    }});

                    updateViewer();
                </script>
            </body>
            </html>
            """

            temp_html_path = os.path.join(file_dir, "index_hdr_temp.html")
            with open(temp_html_path, "w", encoding="utf-8") as f:
                f.write(html_content)

            server_thread = threading.Thread(target=start_local_server, args=(file_dir,), daemon=True)
            server_thread.start()

            window.load_url(f"http://127.0.0.1:{PORT}/index_hdr_temp.html")
            window.resize(1200, 800)
            window.move(100, 100)
            window.show()
            
            window.events.closing += lambda: clean_up(temp_html_path)
        else:
            window.destroy()

    def clean_up(temp_file):
        if os.path.exists(temp_file):
            try:
                os.remove(temp_file)
            except:
                pass
        if server_httpd:
            threading.Thread(target=server_httpd.shutdown, daemon=True).start()

    window = webview.create_window("Ultra HDR JPEG Viewer (Gain Map Based)", width=1, height=1, hidden=True)
    webview.start(open_file_and_show)


if __name__ == "__main__":
    main()
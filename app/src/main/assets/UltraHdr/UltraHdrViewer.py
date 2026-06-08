import os
import re
import json
import http.server
import socketserver
import threading
import webview
from webview import FileDialog

PORT = 8080
server_httpd = None


def start_local_server(directory):
    global server_httpd
    class Handler(http.server.SimpleHTTPRequestHandler):
        def __init__(self, *args, **kwargs):
            super().__init__(*args, directory=directory, **kwargs)
        def log_message(self, format, *args):
            pass

    socketserver.TCPServer.allow_reuse_address = True
    server_httpd = socketserver.TCPServer(("127.0.0.1", PORT), Handler)
    server_httpd.serve_forever()


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

            try:
                start_index = image_files.index(current_file_name)
            except ValueError:
                image_files.insert(0, current_file_name)
                start_index = 0

            images_json = json.dumps(image_files)

            html_content = f"""
            <!DOCTYPE html>
            <html lang="de">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    * {{ margin: 0; padding: 0; box-sizing: border-box; user-select: none; }}
                    body {{ background-color: #121212; display: flex; justify-content: center; align-items: center; min-height: 100vh; overflow: hidden; font-family: sans-serif; }}
                    .viewer-container {{ position: relative; width: 100vw; height: 100vh; display: flex; justify-content: center; align-items: center; }}
                    img {{ max-width: 100%; max-height: 100vh; object-fit: contain; color-profile: display-p3; image-rendering: high-quality; }}
                    .nav-btn {{ position: absolute; top: 50%; transform: translateY(-50%); width: 60px; height: 60px; background-color: rgba(30, 30, 30, 0.4); color: #ffffff; border: 1px solid rgba(255, 255, 255, 0.2); border-radius: 50%; font-size: 24px; cursor: pointer; display: flex; justify-content: center; align-items: center; transition: all 0.2s ease; backdrop-filter: blur(5px); z-index: 10; }}
                    .nav-btn:hover {{ background-color: rgba(60, 60, 60, 0.8); border-color: rgba(255, 255, 255, 0.6); scale: 1.05; }}
                    .nav-btn:active {{ scale: 0.95; }}
                    .prev-btn {{ left: 20px; }}
                    .next-btn {{ right: 20px; }}
                </style>
            </head>
            <body>
                <div class="viewer-container">
                    <button class="nav-btn prev-btn" onclick="prevImage()">&#10094;</button>
                    <img id="hdr-image" src="" alt="Ultra HDR Image">
                    <button class="nav-btn next-btn" onclick="nextImage()">&#10095;</button>
                </div>
                <script>
                    const images = {images_json};
                    let currentIndex = {start_index};

                    function updateViewer() {{
                        if (images.length > 0 && currentIndex >= 0 && currentIndex < images.length) {{
                            const fileName = images[currentIndex];
                            document.getElementById('hdr-image').src = 'http://127.0.0.1:' + window.location.port + '/' + encodeURIComponent(fileName);
                            document.title = "Ultra HDR Viewer - " + fileName;
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
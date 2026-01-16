import os
import sys
import glob
import subprocess
import re
import tkinter as tk
from tkinter import ttk, messagebox, filedialog
import tkinter.font as tkfont

class YuvRawConverterGui:
    def __init__(self, root):
        self.root = root
        self.root.title("YUV Raw Converter & Viewer")
        
        if getattr(sys, 'frozen', False):
            self.script_dir = os.path.dirname(sys.executable)
        else:
            self.script_dir = os.path.dirname(os.path.abspath(__file__))
            
        self.files_metadata = {}
        self.default_font = tkfont.nametofont("TkDefaultFont")
        
        self.setup_styles()
        self.setup_ui()
        
        if not self.scan_for_files():
            self.ask_for_directory()
        else:
            self.refresh_list()

    def setup_styles(self):
        style = ttk.Style()
        style.theme_use("clam")
        style.configure("Treeview.Heading", font=(self.default_font.actual("family"), 10, "bold"))
        style.configure("Treeview", rowheight=25, background="white", fieldbackground="white")
        style.map("Treeview", background=[('selected', '#347083')])

    def setup_ui(self):
        self.main_container = tk.Frame(self.root)
        self.main_container.pack(padx=10, pady=10, fill="both", expand=True)
        
        self.table_frame = tk.Frame(self.main_container)
        self.table_frame.pack(side="top", fill="both", expand=True)
        
        self.columns = ("name", "bit", "res", "iso", "exp", "focal", "focal35")
        self.headers = {
            "name": "Filename", "bit": "Bit", "res": "Resolution",
            "iso": "ISO", "exp": "Exposure", "focal": "Focal", "focal35": "Focal 35mm"
        }
        
        self.tree = ttk.Treeview(self.table_frame, columns=self.columns, show="headings", selectmode="extended")
        self.tree.tag_configure('oddrow', background='#f0f0f0')
        self.tree.tag_configure('evenrow', background='white')
        
        for col in self.columns:
            self.tree.heading(col, text=self.headers[col])
            self.tree.column(col, anchor="center", stretch=False)
        
        y_scroll = ttk.Scrollbar(self.table_frame, orient="vertical", command=self.tree.yview)
        self.tree.configure(yscroll=y_scroll.set)
        
        self.tree.pack(side="left", fill="both", expand=True)
        y_scroll.pack(side="right", fill="y")
        
        self.options_frame = tk.LabelFrame(self.main_container, text="Encoding Options", padx=10, pady=10)
        self.options_frame.pack(side="top", fill="x", pady=10)
        
        self.var_avif = tk.BooleanVar(value=True)
        self.var_jxl = tk.BooleanVar(value=False)
        self.var_jpg = tk.BooleanVar(value=False)
        self.var_same_dir = tk.BooleanVar(value=True)
        self.var_overwrite = tk.BooleanVar(value=False) # Neue Variable für Overwrite
        
        self.cb_avif = tk.Checkbutton(self.options_frame, text="AVIF", variable=self.var_avif)
        self.cb_avif.pack(side="left")
        self.cb_jxl = tk.Checkbutton(self.options_frame, text="JPEG-XL", variable=self.var_jxl)
        self.cb_jxl.pack(side="left")
        self.cb_jpg = tk.Checkbutton(self.options_frame, text="JPEG", variable=self.var_jpg)
        self.cb_jpg.pack(side="left")
        
        tk.Label(self.options_frame, text=" | ").pack(side="left", padx=5)
        self.cb_same_dir = tk.Checkbutton(self.options_frame, text="Save in same directory", variable=self.var_same_dir)
        self.cb_same_dir.pack(side="left")

        # Overwrite Checkbox
        tk.Label(self.options_frame, text=" | ").pack(side="left", padx=5)
        self.cb_overwrite = tk.Checkbutton(self.options_frame, text="Overwrite existing", variable=self.var_overwrite)
        self.cb_overwrite.pack(side="left")

        self.tree.bind("<<TreeviewSelect>>", self.on_select)
        self.tree.bind("<Double-1>", lambda e: self.start_preview())
        
        self.btn_frame = tk.Frame(self.main_container)
        self.btn_frame.pack(side="top", pady=5)
        
        self.preview_btn = tk.Button(self.btn_frame, text="Preview (FFPLAY)", command=self.start_preview,
                                    bg="#2196F3", fg="white", font=(self.default_font.actual("family"), 10, "bold"))
        self.preview_btn.pack(side="left", padx=10)
        
        self.encode_btn = tk.Button(self.btn_frame, text="Encode Selected", command=self.start_encoding, 
                                   bg="#4CAF50", fg="white", font=(self.default_font.actual("family"), 10, "bold"))
        self.encode_btn.pack(side="left", padx=10)
        
        self.refresh_btn = tk.Button(self.btn_frame, text="Refresh List", command=self.refresh_list)
        self.refresh_btn.pack(side="left", padx=5)
        
        self.dir_btn = tk.Button(self.btn_frame, text="Change Directory", command=self.ask_for_directory)
        self.dir_btn.pack(side="left", padx=5)

    def scan_for_files(self):
        pattern = os.path.join(self.script_dir, "*.raw")
        files = glob.glob(pattern)
        return any(os.path.exists(f.replace(".raw", ".txt")) for f in files)

    def ask_for_directory(self):
        selected_dir = filedialog.askdirectory(initialdir=self.script_dir, title="Select Directory")
        if selected_dir:
            self.script_dir = selected_dir
            self.refresh_list()
        elif not self.files_metadata:
            self.root.destroy()

    def parse_metadata(self, content):
        meta = {"bit": "-", "res": "-", "iso": "-", "exp": "-", "focal": "-", "focal35": "-", 
                "avif_cmd": None, "jxl_cmd": None, "jpg_cmd": None, "ffplay_cmd": None, "full_content": content}
        
        res_match = re.search(r"-video_size\s+(\d+x\d+)", content)
        if res_match: meta["res"] = res_match.group(1)
        iso_match = re.search(r"ISO: (\d+)", content)
        if iso_match: meta["iso"] = iso_match.group(1)
        exp_match = re.search(r"Exposure Time: ([\d/]+)", content)
        if exp_match: meta["exp"] = exp_match.group(1)
        foc_match = re.search(r"Focal Length: ([\d.]+)", content)
        if foc_match: meta["focal"] = foc_match.group(1) + " mm"
        foc35_match = re.search(r"Focal Length 35mm: (\d+)", content)
        if foc35_match: meta["focal35"] = foc35_match.group(1) + " mm"

        fmt_match = re.search(r"-pixel_format\s+(\S+)", content)
        if fmt_match:
            meta["bit"] = "10 Bit" if "10" in fmt_match.group(1) else "8 Bit"

        lines = content.splitlines()
        for i, line in enumerate(lines):
            line_s = line.strip()
            if "ENCODING AVIF" in line_s:
                for next_line in lines[i+1:i+5]:
                    if next_line.strip().startswith("ffmpeg"):
                        meta["avif_cmd"] = next_line.strip()
                        break
            elif "ENCODING JPEG-XL" in line_s:
                for next_line in lines[i+1:i+5]:
                    if next_line.strip().startswith("ffmpeg"):
                        meta["jxl_cmd"] = next_line.strip()
                        break
            elif "ENCODING JPEG" in line_s:
                for next_line in lines[i+1:i+5]:
                    if next_line.strip().startswith("ffmpeg"):
                        meta["jpg_cmd"] = next_line.strip()
                        break
            elif "VIEWING" in line_s:
                for next_line in lines[i+1:i+5]:
                    if next_line.strip().startswith("ffplay"):
                        meta["ffplay_cmd"] = next_line.strip()
                        break
        return meta

    def on_select(self, event):
        selected_items = self.tree.selection()
        if not selected_items: return
        meta = self.files_metadata.get(selected_items[0])
        if not meta: return
        
        self.cb_avif.config(state="normal" if meta["avif_cmd"] else "disabled")
        self.cb_jxl.config(state="normal" if meta["jxl_cmd"] else "disabled")
        self.cb_jpg.config(state="normal" if meta["jpg_cmd"] else "disabled")
        self.preview_btn.config(state="normal" if (len(selected_items) == 1 and meta["ffplay_cmd"]) else "disabled")

    def start_preview(self):
        selected_items = self.tree.selection()
        if len(selected_items) != 1: return
        meta = self.files_metadata.get(selected_items[0])
        if meta and meta["ffplay_cmd"]:
            subprocess.Popen(meta["ffplay_cmd"], shell=True, cwd=self.script_dir)

    def start_encoding(self):
        selected_items = self.tree.selection()
        if not selected_items:
            messagebox.showinfo("Selection", "Please select one or more files first.")
            return
            
        target_dir = self.script_dir
        if not self.var_same_dir.get():
            target_dir = filedialog.askdirectory(title="Select Output Directory")
            if not target_dir: return

        wanted_formats = []
        if self.var_avif.get(): wanted_formats.append(("avif_cmd", ".avif"))
        if self.var_jxl.get(): wanted_formats.append(("jxl_cmd", ".jxl"))
        if self.var_jpg.get(): wanted_formats.append(("jpg_cmd", ".jpg"))

        if not wanted_formats:
            messagebox.showwarning("No Format", "Please select at least one output format.")
            return

        success_count = 0
        skip_count = 0
        
        for item_id in selected_items:
            meta = self.files_metadata.get(item_id)
            if not meta: continue

            for cmd_key, ext in wanted_formats:
                cmd = meta.get(cmd_key)
                if not cmd: continue

                output_path = os.path.join(target_dir, f"{item_id}{ext}")
                
                # Checke auf Existenz, falls Overwrite nicht gewünscht ist
                if not self.var_overwrite.get() and os.path.exists(output_path):
                    skip_count += 1
                    continue

                # Füge -y (globaler Overwrite) zum FFmpeg Befehl hinzu
                parts = cmd.rsplit('"', 2)
                if len(parts) >= 3:
                    new_cmd = f'ffmpeg -y {parts[0].replace("ffmpeg ", "", 1)}"{output_path}"{parts[2]}'
                else:
                    new_cmd = f'{cmd.replace("ffmpeg ", "ffmpeg -y ", 1).rsplit(" ", 1)[0]} "{output_path}"'
                
                try:
                    subprocess.run(new_cmd, shell=True, check=True, cwd=self.script_dir)
                    self.apply_exif(output_path, meta["full_content"])
                    success_count += 1
                except Exception as e:
                    print(f"Failed to encode {item_id} to {ext}: {e}")
        
        msg = f"Encoding finished. {success_count} files created."
        if skip_count > 0:
            msg += f"\n({skip_count} files skipped because they already existed)."
        messagebox.showinfo("Done", msg)

    def apply_exif(self, file_path, content):
        exif_cmd = ["exiftool", "-overwrite_original"]
        mappings = {
            r"(PhotonVidCam|PhotonCamera) Version: (.*)": "-Software=",
            r"Brand: (.*)": "-Make=",
            r"Model: (.*)": "-Model=",
            r"Aperture: F([\d.]+)": "-FNumber=",
            r"ISO: (\d+)": "-ISO=",
            r"Exposure Time: ([\d/]+)": "-ExposureTime=",
            r"Focal Length: ([\d.]+)": "-FocalLength=",
            r"Focal Length 35mm: (\d+)": "-FocalLengthIn35mmFormat="
        }
        for pattern, tag in mappings.items():
            m = re.search(pattern, content)
            if m:
                val = m.group(2) if "Version" in pattern else m.group(1).strip()
                exif_cmd.append(f"{tag}{val}")
                exif_cmd.append(f"-XMP{tag}{val}")
        
        exif_cmd.append(file_path)
        try:
            subprocess.run(exif_cmd, check=True)
        except:
            pass

    def refresh_list(self):
        for i in self.tree.get_children(): self.tree.delete(i)
        self.files_metadata.clear()
        raw_files = sorted(glob.glob(os.path.join(self.script_dir, "*.raw")))
        count = 0
        for raw_path in raw_files:
            base_name = os.path.splitext(os.path.basename(raw_path))[0]
            txt_path = os.path.join(self.script_dir, f"{base_name}.txt")
            if os.path.exists(txt_path):
                try:
                    with open(txt_path, 'r', encoding='utf-8') as f: content = f.read()
                    m = self.parse_metadata(content)
                    tag = 'evenrow' if count % 2 == 0 else 'oddrow'
                    self.tree.insert("", "end", iid=base_name, values=(
                        base_name, m["bit"], m["res"], m["iso"], m["exp"], m["focal"], m["focal35"]
                    ), tags=(tag,))
                    self.files_metadata[base_name] = m
                    count += 1
                except: pass
        self.adjust_column_widths()
        self.adjust_window_size()

    def adjust_column_widths(self):
        header_font = tkfont.Font(family=self.default_font.actual("family"), size=10, weight="bold")
        for col in self.columns:
            max_w = header_font.measure(self.headers[col]) + 25
            for item in self.tree.get_children():
                val = str(self.tree.item(item)['values'][self.columns.index(col)])
                w = self.default_font.measure(val) + 25
                if w > max_w: max_w = w
            self.tree.column(col, width=max_w)

    def adjust_window_size(self):
        self.root.update_idletasks()
        content_width = self.container_width()
        window_height = self.root.winfo_reqheight()
        self.root.geometry(f"{content_width}x{window_height}")

    def container_width(self):
        total_width = 0
        for col in self.columns:
            total_width += self.tree.column(col, 'width')
        return total_width + 60

if __name__ == "__main__":
    root = tk.Tk()
    app = YuvRawConverterGui(root)
    root.mainloop()
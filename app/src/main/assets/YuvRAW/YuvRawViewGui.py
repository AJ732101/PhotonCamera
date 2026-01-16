import os
import sys
import glob
import subprocess
import re
import tkinter as tk
from tkinter import ttk, messagebox, filedialog
import tkinter.font as tkfont

class YuvRawGui:
    def __init__(self, root):
        self.root = root
        self.root.title("YUV Raw Viewer")
        
        if getattr(sys, 'frozen', False):
            self.script_dir = os.path.dirname(sys.executable)
            bundle_dir = getattr(sys, '_MEIPASS', self.script_dir)
        else:
            self.script_dir = os.path.dirname(os.path.abspath(__file__))
            bundle_dir = self.script_dir

        icon_path = os.path.join(bundle_dir, "icon.ico")
        if os.path.exists(icon_path):
            try:
                self.root.iconbitmap(icon_path)
            except:
                pass
            
        self.files_dict = {}
        self.default_font = tkfont.nametofont("TkDefaultFont")
        
        self.setup_styles()
        self.setup_ui()
        
        if not self.scan_for_files():
            self.ask_for_directory()
        else:
            self.refresh_list()
            self.adjust_window_size()

    def setup_styles(self):
        style = ttk.Style()
        style.theme_use("clam")
        style.configure("Treeview.Heading", font=(self.default_font.actual("family"), 10, "bold"))
        style.configure("Treeview", rowheight=25, background="white", fieldbackground="white")
        style.map("Treeview", background=[('selected', '#347083')])

    def setup_ui(self):
        self.container = tk.Frame(self.root)
        self.container.pack(padx=10, pady=10)
        
        self.columns = ("name", "bit", "res", "iso", "exp", "focal", "focal35")
        self.headers = {
            "name": "Dateiname", "bit": "Bit", "res": "Resolution",
            "iso": "ISO", "exp": "Exposure", "focal": "Focal", "focal35": "Focal 35mm"
        }
        
        self.tree = ttk.Treeview(self.container, columns=self.columns, show="headings")
        self.tree.tag_configure('oddrow', background='#f0f0f0')
        self.tree.tag_configure('evenrow', background='white')
        
        for col in self.columns:
            self.tree.heading(col, text=self.headers[col])
            self.tree.column(col, anchor="center", stretch=False)
        
        y_scroll = ttk.Scrollbar(self.container, orient="vertical", command=self.tree.yview)
        self.tree.configure(yscroll=y_scroll.set)
        
        self.tree.pack(side="left")
        y_scroll.pack(side="right", fill="y")
        
        self.tree.bind('<Double-1>', self.on_double_click)
        
        self.btn_frame = tk.Frame(self.root)
        self.btn_frame.pack(pady=(0, 10))
        
        self.refresh_btn = tk.Button(self.btn_frame, text="Aktualisieren", command=self.refresh_list)
        self.refresh_btn.pack(side="left", padx=5)
        
        self.dir_btn = tk.Button(self.btn_frame, text="Ordner wechseln", command=self.ask_for_directory)
        self.dir_btn.pack(side="left", padx=5)

    def scan_for_files(self):
        pattern = os.path.join(self.script_dir, "*.raw")
        files = glob.glob(pattern)
        valid_found = False
        for f in files:
            if os.path.exists(f.replace(".raw", ".txt")):
                valid_found = True
                break
        return valid_found

    def ask_for_directory(self):
        selected_dir = filedialog.askdirectory(initialdir=self.script_dir, title="Verzeichnis wählen")
        if selected_dir:
            self.script_dir = selected_dir
            self.refresh_list()
            self.adjust_window_size()
        elif not self.files_dict:
            self.root.destroy()

    def adjust_column_widths(self):
        header_font = tkfont.Font(family=self.default_font.actual("family"), size=10, weight="bold")
        for col in self.columns:
            header_text = self.headers[col]
            max_width = header_font.measure(header_text) + 20
            for item in self.tree.get_children():
                col_index = self.columns.index(col)
                cell_value = str(self.tree.item(item)['values'][col_index])
                text_width = self.default_font.measure(cell_value) + 20
                if text_width > max_width:
                    max_width = text_width
            self.tree.column(col, width=max_width)

    def adjust_window_size(self):
        self.root.update_idletasks()
        width = self.container.winfo_reqwidth() + 40
        height = self.root.winfo_reqheight()
        self.root.geometry(f"{width}x{height}")

    def parse_metadata(self, content):
        meta = {"bit": "-", "res": "-", "iso": "-", "exp": "-", "focal": "-", "focal35": "-", "cmd": None}
        fmt_match = re.search(r"-pixel_format\s+(\S+)", content)
        if fmt_match:
            meta["bit"] = "10 Bit" if "10" in fmt_match.group(1) else "8 Bit"
        res_match = re.search(r"-video_size\s+(\d+x\d+)", content)
        if res_match: meta["res"] = res_match.group(1)
        iso_match = re.search(r"ISO\s*:\s*(\d+)", content)
        if iso_match: meta["iso"] = iso_match.group(1)
        exp_match = re.search(r"Exposure Time\s*:\s*(\S+)", content)
        if exp_match: meta["exp"] = exp_match.group(1)
        foc_match = re.search(r"Focal Length\s*:\s*([\d.]+ mm)", content)
        if foc_match: meta["focal"] = foc_match.group(1)
        foc35_match = re.search(r"Focal Length In 35mm Format\s*:\s*([\d.]+ mm)", content)
        if foc35_match: meta["focal35"] = foc35_match.group(1)
        for line in content.splitlines():
            if line.strip().startswith("ffplay"):
                meta["cmd"] = line.strip()
                break
        return meta

    def refresh_list(self):
        for i in self.tree.get_children():
            self.tree.delete(i)
        self.files_dict.clear()
        search_pattern = os.path.join(self.script_dir, "*.raw")
        raw_files = sorted(glob.glob(search_pattern))
        count = 0
        for raw_path in raw_files:
            base_name = os.path.splitext(os.path.basename(raw_path))[0]
            txt_path = os.path.join(self.script_dir, f"{base_name}.txt")
            if os.path.exists(txt_path):
                try:
                    with open(txt_path, 'r', encoding='utf-8') as f:
                        content = f.read()
                    m = self.parse_metadata(content)
                    if m["cmd"]:
                        tag = 'evenrow' if count % 2 == 0 else 'oddrow'
                        self.tree.insert("", "end", iid=base_name, values=(
                            base_name, m["bit"], m["res"], m["iso"], 
                            m["exp"], m["focal"], m["focal35"]
                        ), tags=(tag,))
                        self.files_dict[base_name] = m["cmd"]
                        count += 1
                except Exception:
                    pass
        self.adjust_column_widths()

    def on_double_click(self, event):
        item_id = self.tree.focus()
        if item_id:
            cmd = self.files_dict.get(item_id)
            if cmd:
                subprocess.Popen(cmd, shell=True, cwd=self.script_dir)

if __name__ == "__main__":
    root = tk.Tk()
    app = YuvRawGui(root)
    root.mainloop()
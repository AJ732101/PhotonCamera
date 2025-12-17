import numpy as np
import png
import os
import tkinter as tk
from tkinter import messagebox, filedialog, ttk, colorchooser

def hex_to_rgb_float(hex_str):
    """Konvertiert Hex-Strings sicher in 0.0-1.0 Float-Arrays."""
    clean_hex = hex_str.lstrip('#').lower().replace('0x', '')
    return np.array([
        int(clean_hex[0:2], 16) / 255.0,
        int(clean_hex[2:4], 16) / 255.0,
        int(clean_hex[4:6], 16) / 255.0
    ], dtype=np.float32)

def calculate_lut_core(sat_pct, contrast_pct, highlight_hex, shadow_hex, temp_val, size):
    """Die mathematische Kern-Logik für die Farbtransformation."""
    res = np.linspace(0, 1, size)
    # Standard 3D Mesh
    b, g, r = np.meshgrid(res, res, res, indexing='ij')
    rgb = np.stack([r, g, b], axis=-1).astype(np.float32)

    # 1. Contrast
    pivot = 0.5
    rgb = pivot + (rgb - pivot) * (contrast_pct / 100.0)

    # 2. Temperature (MMidtones)
    luma = 0.2126 * rgb[..., 0] + 0.7152 * rgb[..., 1] + 0.0722 * rgb[..., 2]
    mid_mask = 4 * luma * (1.0 - luma) 
    shift = temp_val * 0.015
    rgb[..., 0] += shift * mid_mask
    rgb[..., 2] -= shift * mid_mask

    # 3. Saturation
    luma_final = 0.2126 * rgb[..., 0] + 0.7152 * rgb[..., 1] + 0.0722 * rgb[..., 2]
    luma_stack = np.stack([luma_final] * 3, axis=-1)
    rgb = luma_stack + (rgb - luma_stack) * (sat_pct / 100.0)

    # 4. Tints
    h_tint = hex_to_rgb_float(highlight_hex)
    s_tint = hex_to_rgb_float(shadow_hex)
    safe_rgb = np.clip(rgb, 0, 1)
    rgb += (h_tint - 0.5) * (safe_rgb**2) * 0.5
    rgb += (s_tint - 0.5) * ((1.0 - safe_rgb)**2) * 0.5

    return np.clip(rgb, 0, 1)

def write_cube_file(output_path, rgb_data):
    size = rgb_data.shape[0]
    with open(output_path, 'w') as f:
        f.write(f'TITLE "Generated_LUT"\nLUT_3D_SIZE {size}\nDOMAIN_MIN 0.0 0.0 0.0\nDOMAIN_MAX 1.0 1.0 1.0\n')
        for b in range(size):
            for g in range(size):
                for r in range(size):
                    val = rgb_data[b, g, r]
                    f.write(f"{val[0]:.6f} {val[1]:.6f} {val[2]:.6f}\n")

class LUTGeneratorGUI:
    def __init__(self, root):
        self.root = root
        self.root.title("PhotonVidCam LUT Creator")
        self.root.attributes('-toolwindow', True)

        # Defaults
        default_path = r"C:\LUTs"
        if not os.path.exists(default_path):
            try: os.makedirs(default_path)
            except: default_path = os.getcwd()
        
        self.output_dir = tk.StringVar(value=default_path)
        self.highlight_hex = tk.StringVar(value="#FFCC99")
        self.shadow_hex = tk.StringVar(value="#334433")
        self.temp_val = tk.DoubleVar(value=0.0)
        
        main_frame = ttk.Frame(root, padding="15")
        main_frame.pack(fill="both", expand=True)

        # UI
        ttk.Label(main_frame, text="Saturation (%)").pack(anchor="w")
        self.ent_sat = ttk.Entry(main_frame); self.ent_sat.insert(0, "70"); self.ent_sat.pack(fill="x", pady=(0, 10))
        
        ttk.Label(main_frame, text="Contrast (%)").pack(anchor="w")
        self.ent_con = ttk.Entry(main_frame); self.ent_con.insert(0, "85"); self.ent_con.pack(fill="x", pady=(0, 10))
        
        t_frame = ttk.Frame(main_frame); t_frame.pack(fill="x")
        ttk.Label(t_frame, text="Temperature Offset:").pack(side="left")
        self.t_disp = ttk.Label(t_frame, text="0.0"); self.t_disp.pack(side="left", padx=5)
        self.sld_temp = ttk.Scale(main_frame, from_=-3.0, to=3.0, variable=self.temp_val, orient="horizontal", command=self.upd_t)
        self.sld_temp.pack(fill="x", pady=(0, 15))

        ttk.Label(main_frame, text="Highlight Tint:").pack(anchor="w", pady=(10, 0))
        self.btn_high = tk.Button(main_frame, textvariable=self.highlight_hex, bg=self.highlight_hex.get(), command=self.p_high)
        self.btn_high.pack(fill="x", pady=5)
        ttk.Label(main_frame, text="Shadow Tint:").pack(anchor="w", pady=(10, 0))
        self.btn_shad = tk.Button(main_frame, textvariable=self.shadow_hex, bg=self.shadow_hex.get(), fg="white", command=self.p_shad)
        self.btn_shad.pack(fill="x", pady=5)

        ttk.Label(main_frame, text="CUBE Export Size:").pack(anchor="w", pady=(10, 0))
        self.cube_sizes = {"17 (Small)": 17, "33 (Standard)": 33, "64 (High-Res)": 64}
        self.cube_sel = ttk.Combobox(main_frame, values=list(self.cube_sizes.keys()), state="readonly")
        self.cube_sel.set("33 (Standard)"); self.cube_sel.pack(fill="x", pady=(0, 10))

        ttk.Label(main_frame, text="Export Location:").pack(anchor="w", pady=(10, 0))
        f_frame = ttk.Frame(main_frame)
        f_frame.pack(fill="x", pady=(2, 15))
        ttk.Entry(f_frame, textvariable=self.output_dir).pack(side="left", fill="x", expand=True)
        ttk.Button(f_frame, text="...", width=3, command=self.br_f).pack(side="right")

        b_frame = ttk.Frame(main_frame); b_frame.pack(fill="x", pady=10)
        ttk.Button(b_frame, text="Export PNG", command=self.gen_png).pack(side="left", fill="x", expand=True, padx=(0,5))
        ttk.Button(b_frame, text="Export CUBE", command=self.gen_cube).pack(side="right", fill="x", expand=True, padx=(5,0))

        # Dynamisches Fenster
        self.root.update_idletasks()
        self.root.geometry(f'{self.root.winfo_reqwidth()}x{self.root.winfo_reqheight()}')
        self.root.resizable(False, False)

    def upd_t(self, e): self.t_disp.config(text=f"{self.temp_val.get():.1f}")
    def p_high(self):
        c = colorchooser.askcolor(initialcolor=self.highlight_hex.get())[1]
        if c: self.highlight_hex.set(c.upper()); self.btn_high.config(bg=c)
    def p_shad(self):
        c = colorchooser.askcolor(initialcolor=self.shadow_hex.get())[1]
        if c: self.shadow_hex.set(c.upper()); self.btn_shad.config(bg=c)
    def br_f(self):
        f = filedialog.askdirectory(initialdir=self.output_dir.get())
        if f: self.output_dir.set(f)

    def get_filename_base(self):
        h = self.highlight_hex.get().replace('#','0x')
        s = self.shadow_hex.get().replace('#','0x')
        return f"look_s{self.ent_sat.get()}_c{self.ent_con.get()}_t{round(self.temp_val.get(),1)}_{h}_{s}"

    def gen_png(self):
        """Reiner PNG Pfad - Fest auf 64er Gitter für 512x512 Hald."""
        try:
            rgb = calculate_lut_core(float(self.ent_sat.get()), float(self.ent_con.get()), 
                                     self.highlight_hex.get(), self.shadow_hex.get(), 
                                     self.temp_val.get(), 64)
            
            path = os.path.join(self.output_dir.get(), self.get_filename_base() + "_lut.png")
            data_8bit = (rgb * 255).astype(np.uint8)
            hald_img = np.zeros((512, 512, 3), dtype=np.uint8)
            idx = 0
            for r in range(8):
                for c in range(8):
                    hald_img[r*64:(r+1)*64, c*64:(c+1)*64] = data_8bit[idx]; idx += 1
            
            flat_data = hald_img.reshape(-1, 512 * 3).tolist()
            with open(path, 'wb') as f:
                png.Writer(width=512, height=512, bitdepth=8, greyscale=False, compression=9).write(f, flat_data)
            messagebox.showinfo("Success", f"PNG gespeichert:\n{path}")
        except Exception as e: messagebox.showerror("PNG Fehler", str(e))

    def gen_cube(self):
        """Reiner CUBE Pfad - Variable Gittergröße."""
        try:
            size = self.cube_sizes[self.cube_sel.get()]
            rgb = calculate_lut_core(float(self.ent_sat.get()), float(self.ent_con.get()), 
                                     self.highlight_hex.get(), self.shadow_hex.get(), 
                                     self.temp_val.get(), size)
            
            path = os.path.join(self.output_dir.get(), self.get_filename_base() + f"_grid{size}.cube")
            write_cube_file(path, rgb)
            messagebox.showinfo("Success", f"CUBE gespeichert:\n{path}")
        except Exception as e: messagebox.showerror("CUBE Fehler", str(e))

if __name__ == "__main__":
    root = tk.Tk(); app = LUTGeneratorGUI(root); root.mainloop()
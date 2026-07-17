import numpy as np
import png
import os
import sys
import tkinter as tk
from tkinter import messagebox, filedialog, ttk, colorchooser
import configparser
from PIL import Image, ImageTk

def get_script_paths():
    if getattr(sys, 'frozen', False):
        script_path = sys.executable
    else:
        script_path = os.path.abspath(__file__)
    
    script_dir = os.path.dirname(script_path)
    base_name = os.path.splitext(os.path.basename(script_path))[0]
    config_path = os.path.join(script_dir, f"{base_name}.ini")
    return script_dir, config_path

SCRIPT_DIR, CONFIG_FILE = get_script_paths()

def hex_to_rgb_float(hex_str):
    clean_hex = hex_str.lstrip('#').lower().replace('0x', '')
    return np.array([
        int(clean_hex[0:2], 16) / 255.0,
        int(clean_hex[2:4], 16) / 255.0,
        int(clean_hex[4:6], 16) / 255.0
    ], dtype=np.float32)

def apply_log_to_linear(rgb, space):
    if space == "Linear RGB":
        return rgb
    elif space == "S-Log2":
        rgb = np.clip(rgb, 0.03, 1.0)
        return (10.0 ** ((rgb - 0.59) / 0.3541) - 0.0416) / 2.222222
    elif space == "S-Log3":
        rgb = np.clip(rgb, 0.01, 1.0)
        mask = rgb >= 0.171260341829144
        return np.where(
            mask,
            10.0 ** ((rgb - 0.421290936) / 0.2615) * (0.18 + 0.01) - 0.01,
            (rgb - 0.092809) / 4.167394 * (0.18 + 0.01) - 0.01
        )
    else:
        mask = rgb <= 0.04045
        return np.where(mask, rgb / 12.92, ((rgb + 0.055) / 1.055) ** 2.4)

def apply_linear_to_log(rgb, space):
    if space == "Linear RGB":
        return rgb
    elif space == "S-Log2":
        rgb_clipped = np.clip(rgb * 2.222222 + 0.0416, 1e-5, None)
        return 0.3541 * np.log10(rgb_clipped) + 0.59
    elif space == "S-Log3":
        rgb_clipped = np.clip(rgb, 1e-5, None)
        mask = rgb_clipped >= 0.01125000
        return np.where(
            mask,
            0.2615 * np.log10((rgb_clipped + 0.01) / 0.19) + 0.421290936,
            4.167394 * (rgb_clipped + 0.01) / 0.19 + 0.092809
        )
    else:
        rgb_clipped = np.clip(rgb, 0.0, 1.0)
        mask = rgb_clipped <= 0.0031308
        return np.where(mask, rgb_clipped * 12.92, 1.055 * (rgb_clipped ** (1.0 / 2.4)) - 0.055)

def apply_lut_math(rgb, sat_pct, contrast_pct, highlight_hex, shadow_hex, midtone_hex, temp_val, exposure_val, input_space):
    rgb = apply_log_to_linear(rgb, input_space)
    rgb = rgb * (2.0 ** exposure_val)

    c_factor = 1.0 + (contrast_pct / 100.0)
    if c_factor < 0: c_factor = 0.0
    rgb = 0.18 + (rgb - 0.18) * c_factor
    rgb = np.clip(rgb, 0.0, 10.0)

    luma = 0.2126 * rgb[..., 0] + 0.7152 * rgb[..., 1] + 0.0722 * rgb[..., 2]
    mid_mask = 4.0 * np.clip(luma, 0, 1) * (1.0 - np.clip(luma, 0, 1)) 
    shift = temp_val * 0.05
    rgb[..., 0] += shift * mid_mask
    rgb[..., 2] -= shift * mid_mask

    h_tint = hex_to_rgb_float(highlight_hex) - 0.5
    s_tint = hex_to_rgb_float(shadow_hex) - 0.5
    m_tint = hex_to_rgb_float(midtone_hex) - 0.5
    
    luma_clamped = np.clip(luma, 0.0, 1.0)
    rgb += h_tint * ((luma_clamped ** 2)[..., np.newaxis]) * 0.3
    rgb += s_tint * (((1.0 - luma_clamped) ** 2)[..., np.newaxis]) * 0.3
    m_mask = np.exp(-((luma_clamped - 0.5) ** 2) / (2 * (0.25 ** 2)))
    rgb += m_tint * m_mask[..., np.newaxis] * 0.3

    luma_final = 0.2126 * rgb[..., 0] + 0.7152 * rgb[..., 1] + 0.0722 * rgb[..., 2]
    luma_stack = np.stack([luma_final] * 3, axis=-1)
    rgb = luma_stack + (rgb - luma_stack) * (sat_pct / 100.0)

    return np.clip(apply_linear_to_log(rgb, input_space), 0.0, 1.0)

def calculate_lut_core(sat_pct, contrast_pct, highlight_hex, shadow_hex, midtone_hex, temp_val, exposure_val, input_space, size):
    res = np.linspace(0, 1, size)
    b, g, r = np.meshgrid(res, res, res, indexing='ij')
    rgb = np.stack([r, g, b], axis=-1).astype(np.float32)
    return apply_lut_math(rgb, sat_pct, contrast_pct, highlight_hex, shadow_hex, midtone_hex, temp_val, exposure_val, input_space)

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
        self.root.title("PhotonVidCam LUT Generator Pro")
        self.root.attributes('-toolwindow', True)
        self.root.protocol("WM_DELETE_WINDOW", self.on_close)

        self.default_dir_path = r"C:\LUTs"
        if not os.path.exists(self.default_dir_path):
            try: os.makedirs(self.default_dir_path)
            except: self.default_dir_path = os.getcwd()
        
        self.output_dir = tk.StringVar(value=self.default_dir_path)
        self.highlight_hex = tk.StringVar(value="#808080")
        self.shadow_hex = tk.StringVar(value="#808080")
        self.midtone_hex = tk.StringVar(value="#808080")
        
        self.sat_val = tk.DoubleVar(value=100.0)
        self.con_val = tk.DoubleVar(value=0.0)
        self.temp_val = tk.DoubleVar(value=0.0)
        self.exp_val = tk.DoubleVar(value=0.0)
        
        self.lut_enabled = tk.BooleanVar(value=True)

        self.preview_synthetic = np.zeros((450, 300, 3), dtype=np.float32)
        for y in range(450):
            for x in range(300):
                if y < 150:
                    stops = -4.0 + (x / 299.0) * 8.0
                    self.preview_synthetic[y, x] = np.tile(0.18 * (2.0 ** stops), 3)
                elif y < 300:
                    step_idx = int(x / 50)
                    stops = -3.0 + step_idx * 1.0
                    self.preview_synthetic[y, x] = np.tile(0.18 * (2.0 ** stops), 3)
                else:
                    fx = x / 299.0
                    fy = (y - 300) / 149.0
                    self.preview_synthetic[y, x] = [fx * 1.5, fy * 1.5, (1.0 - fx) * 1.5]

        self.preview_linear_reference = np.copy(self.preview_synthetic)

        ui_container = ttk.Frame(root, padding="15")
        ui_container.pack(fill="both", expand=True)
        
        main_split = ttk.Frame(ui_container)
        main_split.pack(fill="both", expand=True)
        
        left_frame = ttk.Frame(main_split)
        left_frame.pack(side="left", fill="both", expand=True, padx=(0, 15))
        
        right_frame = ttk.Frame(main_split)
        right_frame.pack(side="right", fill="y", anchor="n")

        ttk.Label(right_frame, text="Live Matrix Preview:").pack(anchor="w", pady=(0, 5))
        self.preview_canvas = tk.Canvas(right_frame, width=300, height=450, bg="black", highlightthickness=1, highlightbackground="gray")
        self.preview_canvas.pack(pady=5)

        self.btn_toggle_lut = tk.Button(right_frame, text="LUT: ACTIVE", bg="#44CC44", fg="black", font=("Arial", 9, "bold"), command=self.toggle_lut_view)
        self.btn_toggle_lut.pack(fill="x", pady=(2, 5))

        img_ctrl_frame = ttk.Frame(right_frame)
        img_ctrl_frame.pack(fill="x", pady=2)
        
        self.btn_load_img = ttk.Button(img_ctrl_frame, text="Load Image", command=self.load_custom_image)
        self.btn_load_img.pack(side="left", fill="x", expand=True, padx=(0, 2))
        
        self.btn_reset_img = ttk.Button(img_ctrl_frame, text="Reset Pattern", command=self.reset_to_synthetic)
        self.btn_reset_img.pack(side="right", fill="x", expand=True, padx=(2, 0))

        ttk.Label(left_frame, text="Input Gamma Space:").pack(anchor="w")
        self.space_sel = ttk.Combobox(left_frame, values=["sRGB", "Linear RGB", "S-Log2", "S-Log3"], state="readonly")
        self.space_sel.set("sRGB")
        self.space_sel.pack(fill="x", pady=(0, 15))
        self.space_sel.bind("<<ComboboxSelected>>", lambda e: self.update_preview())

        e_frame = ttk.Frame(left_frame); e_frame.pack(fill="x")
        ttk.Label(e_frame, text="Exposure (EV):").pack(side="left")
        self.e_disp = ttk.Label(e_frame, text="0.0"); self.e_disp.pack(side="left", padx=5)
        self.scale_exp = ttk.Scale(left_frame, from_=-2.0, to=2.0, variable=self.exp_val, orient="horizontal", command=self.on_param_change)
        self.scale_exp.pack(fill="x", pady=(0, 10))

        sat_frame = ttk.Frame(left_frame); sat_frame.pack(fill="x")
        ttk.Label(sat_frame, text="Saturation (%):").pack(side="left")
        self.sat_disp = ttk.Label(sat_frame, text="100"); self.sat_disp.pack(side="left", padx=5)
        self.scale_sat = ttk.Scale(left_frame, from_=0.0, to=200.0, variable=self.sat_val, orient="horizontal", command=self.on_param_change)
        self.scale_sat.pack(fill="x", pady=(0, 10))
        
        con_frame = ttk.Frame(left_frame); con_frame.pack(fill="x")
        ttk.Label(con_frame, text="Contrast (%):").pack(side="left")
        self.con_disp = ttk.Label(con_frame, text="0"); self.con_disp.pack(side="left", padx=5)
        self.scale_con = ttk.Scale(left_frame, from_=-100.0, to=100.0, variable=self.con_val, orient="horizontal", command=self.on_param_change)
        self.scale_con.pack(fill="x", pady=(0, 10))
        
        t_frame = ttk.Frame(left_frame); t_frame.pack(fill="x")
        ttk.Label(t_frame, text="Temperature Offset:").pack(side="left")
        self.t_disp = ttk.Label(t_frame, text="0.0"); self.t_disp.pack(side="left", padx=5)
        self.scale_temp = ttk.Scale(left_frame, from_=-3.0, to=3.0, variable=self.temp_val, orient="horizontal", command=self.on_param_change)
        self.scale_temp.pack(fill="x", pady=(0, 15))

        h_lbl_frame = ttk.Frame(left_frame); h_lbl_frame.pack(fill="x", pady=(5, 0))
        ttk.Label(h_lbl_frame, text="Highlight Tint:").pack(side="left")
        self.lbl_high_hex = ttk.Label(h_lbl_frame, text=self.highlight_hex.get())
        self.lbl_high_hex.pack(side="left", padx=5)
        self.btn_high = tk.Button(left_frame, bg=self.highlight_hex.get(), relief="groove", height=1)
        self.btn_high.pack(fill="x", pady=(2, 10))
        self.btn_high.config(command=self.p_high)
        
        m_lbl_frame = ttk.Frame(left_frame); m_lbl_frame.pack(fill="x", pady=(5, 0))
        ttk.Label(m_lbl_frame, text="Midtone Tint:").pack(side="left")
        self.lbl_mid_hex = ttk.Label(m_lbl_frame, text=self.midtone_hex.get())
        self.lbl_mid_hex.pack(side="left", padx=5)
        self.btn_mid = tk.Button(left_frame, bg=self.midtone_hex.get(), relief="groove", height=1)
        self.btn_mid.pack(fill="x", pady=(2, 10))
        self.btn_mid.config(command=self.p_mid)

        s_lbl_frame = ttk.Frame(left_frame); s_lbl_frame.pack(fill="x", pady=(5, 0))
        ttk.Label(s_lbl_frame, text="Shadow Tint:").pack(side="left")
        self.lbl_shad_hex = ttk.Label(s_lbl_frame, text=self.shadow_hex.get())
        self.lbl_shad_hex.pack(side="left", padx=5)
        self.btn_shad = tk.Button(left_frame, bg=self.shadow_hex.get(), relief="groove", height=1)
        self.btn_shad.pack(fill="x", pady=(2, 10))
        self.btn_shad.config(command=self.p_shad)

        ttk.Label(left_frame, text="CUBE Export Size:").pack(anchor="w", pady=(10, 0))
        self.cube_sizes = {"17 (Small)": 17, "33 (Standard)": 33, "64 (High-Res)": 64}
        self.cube_sel = ttk.Combobox(left_frame, values=list(self.cube_sizes.keys()), state="readonly")
        self.cube_sel.set("33 (Standard)"); self.cube_sel.pack(fill="x", pady=(0, 10))

        ttk.Label(left_frame, text="Export Location:").pack(anchor="w", pady=(5, 0))
        f_frame = ttk.Frame(left_frame); f_frame.pack(fill="x", pady=(2, 15))
        ttk.Entry(f_frame, textvariable=self.output_dir).pack(side="left", fill="x", expand=True)
        ttk.Button(f_frame, text="...", width=3, command=self.br_f).pack(side="right")

        b_frame = ttk.Frame(ui_container)
        b_frame.pack(fill="x", pady=(15, 0))
        
        center_button_container = ttk.Frame(b_frame)
        center_button_container.pack(anchor="center")
        
        ttk.Button(center_button_container, text="Reset Parameters", command=self.reset_all_params).pack(side="left", padx=10)
        ttk.Button(center_button_container, text="Export PNG", command=self.gen_png).pack(side="left", padx=10)
        ttk.Button(center_button_container, text="Export CUBE", command=self.gen_cube).pack(side="left", padx=10)

        self.load_settings()
        self.update_preview()

        self.root.update_idletasks()
        self.root.geometry(f'{self.root.winfo_reqwidth()}x{self.root.winfo_reqheight()}')
        self.root.resizable(False, False)

    def on_param_change(self, e):
        self.e_disp.config(text=f"{self.exp_val.get():.1f}")
        self.sat_disp.config(text=f"{int(self.sat_val.get())}")
        self.con_disp.config(text=f"{int(self.con_val.get())}")
        self.t_disp.config(text=f"{self.temp_val.get():.1f}")
        self.update_preview()

    def reset_all_params(self):
        self.space_sel.set("sRGB")
        self.exp_val.set(0.0)
        self.sat_val.set(100.0)
        self.con_val.set(0.0)
        self.temp_val.set(0.0)
        
        self.highlight_hex.set("#808080")
        self.midtone_hex.set("#808080")
        self.shadow_hex.set("#808080")
        
        self.lbl_high_hex.config(text="#808080")
        self.lbl_mid_hex.config(text="#808080")
        self.lbl_shad_hex.config(text="#808080")
        
        self.btn_high.config(bg="#808080", activebackground="#808080")
        self.btn_mid.config(bg="#808080", activebackground="#808080")
        self.btn_shad.config(bg="#808080", activebackground="#808080")
        
        self.cube_sel.set("33 (Standard)")
        self.output_dir.set(self.default_dir_path)
        
        self.on_param_change(None)
        self.save_settings()

    def toggle_lut_view(self):
        self.lut_enabled.set(not self.lut_enabled.get())
        if self.lut_enabled.get():
            self.btn_toggle_lut.config(text="LUT: ACTIVE", bg="#44CC44")
        else:
            self.btn_toggle_lut.config(text="BYPASS (NO LUT)", bg="#FF4444")
        self.update_preview()

    def load_custom_image(self):
        f_types = [("Image Files", "*.png *.jpg *.jpeg *.webp *.gif"), ("All Files", "*.*")]
        file_path = filedialog.askopenfilename(filetypes=f_types)
        if not file_path:
            return
        try:
            with Image.open(file_path) as img:
                img = img.convert("RGB")
                orig_w, orig_h = img.size
                
                scale = min(300 / orig_w, 450 / orig_h)
                target_w = max(1, int(orig_w * scale))
                target_h = max(1, int(orig_h * scale))
                
                img_resized = img.resize((target_w, target_h), Image.Resampling.LANCZOS)
                img_np = np.array(img_resized, dtype=np.float32) / 255.0
                
                temp_reference = np.zeros((450, 300, 3), dtype=np.float32)
                start_x = (300 - target_w) // 2
                start_y = (450 - target_h) // 2
                
                temp_reference[start_y:start_y+target_h, start_x:start_x+target_w] = img_np
            
            self.preview_linear_reference = temp_reference
            self.update_preview()
        except Exception as e:
            messagebox.showerror("Image Load Error", f"Could not process preview context:\n{str(e)}")

    def reset_to_synthetic(self):
        self.preview_linear_reference = np.copy(self.preview_synthetic)
        self.update_preview()

    def update_preview(self):
        current_space = self.space_sel.get()
        base_log_image = apply_linear_to_log(np.copy(self.preview_linear_reference), current_space)
        
        if self.lut_enabled.get():
            rgb_transformed = apply_lut_math(
                base_log_image,
                self.sat_val.get(), self.con_val.get(),
                self.highlight_hex.get(), self.shadow_hex.get(), self.midtone_hex.get(),
                self.temp_val.get(), self.exp_val.get(), current_space
            )
        else:
            rgb_transformed = np.clip(base_log_image, 0, 1)

        rgb_8bit = (rgb_transformed * 255).astype(np.uint8)
        self.preview_canvas.delete("all")
        self.preview_img = tk.PhotoImage(width=300, height=450)
        
        img_data = " ".join(
            "{" + " ".join(f"#{r:02x}{g:02x}{b:02x}" for r, g, b in row) + "}"
            for row in rgb_8bit
        )
        self.preview_img.put(img_data)
        self.preview_canvas.create_image(0, 0, anchor="nw", image=self.preview_img)

    def p_high(self):
        c = colorchooser.askcolor(initialcolor=self.highlight_hex.get())[1]
        if c:
            c_upper = c.upper()
            self.highlight_hex.set(c_upper)
            self.lbl_high_hex.config(text=c_upper)
            self.btn_high.config(bg=c, activebackground=c)
            self.update_preview()
            self.save_settings()

    def p_mid(self):
        c = colorchooser.askcolor(initialcolor=self.midtone_hex.get())[1]
        if c:
            c_upper = c.upper()
            self.midtone_hex.set(c_upper)
            self.lbl_mid_hex.config(text=c_upper)
            self.btn_mid.config(bg=c, activebackground=c)
            self.update_preview()
            self.save_settings()

    def p_shad(self):
        c = colorchooser.askcolor(initialcolor=self.shadow_hex.get())[1]
        if c:
            c_upper = c.upper()
            self.shadow_hex.set(c_upper)
            self.lbl_shad_hex.config(text=c_upper)
            self.btn_shad.config(bg=c, activebackground=c)
            self.update_preview()
            self.save_settings()

    def br_f(self):
        f = filedialog.askdirectory(initialdir=self.output_dir.get())
        if f:
            self.output_dir.set(f)
            self.save_settings()

    def load_settings(self):
        if not os.path.exists(CONFIG_FILE):
            return
        try:
            config = configparser.ConfigParser()
            config.read(CONFIG_FILE, encoding='utf-8')
            if 'Settings' in config:
                sect = config['Settings']
                if 'input_gamma' in sect: self.space_sel.set(sect['input_gamma'])
                if 'exposure' in sect: self.exp_val.set(float(sect['exposure']))
                if 'saturation' in sect: self.sat_val.set(float(sect['saturation']))
                if 'contrast' in sect: self.con_val.set(float(sect['contrast']))
                if 'temperature' in sect: self.temp_val.set(float(sect['temperature']))
                
                if 'highlight_hex' in sect:
                    h = sect['highlight_hex'].upper()
                    self.highlight_hex.set(h); self.lbl_high_hex.config(text=h); self.btn_high.config(bg=h, activebackground=h)
                if 'midtone_hex' in sect:
                    m = sect['midtone_hex'].upper()
                    self.midtone_hex.set(m); self.lbl_mid_hex.config(text=m); self.btn_mid.config(bg=m, activebackground=m)
                if 'shadow_hex' in sect:
                    s = sect['shadow_hex'].upper()
                    self.shadow_hex.set(s); self.lbl_shad_hex.config(text=s); self.btn_shad.config(bg=s, activebackground=s)
                
                if 'cube_size' in sect: self.cube_sel.set(sect['cube_size'])
                if 'output_dir' in sect and os.path.exists(sect['output_dir']): self.output_dir.set(sect['output_dir'])
                
                self.on_param_change(None)
        except:
            pass

    def save_settings(self):
        try:
            config = configparser.ConfigParser()
            config['Settings'] = {
                'input_gamma': self.space_sel.get(),
                'exposure': str(self.exp_val.get()),
                'saturation': str(self.sat_val.get()),
                'contrast': str(self.con_val.get()),
                'temperature': str(self.temp_val.get()),
                'highlight_hex': self.highlight_hex.get(),
                'midtone_hex': self.midtone_hex.get(),
                'shadow_hex': self.shadow_hex.get(),
                'cube_size': self.cube_sel.get(),
                'output_dir': self.output_dir.get()
            }
            with open(CONFIG_FILE, 'w', encoding='utf-8') as f:
                config.write(f)
        except:
            pass

    def on_close(self):
        self.save_settings()
        self.root.destroy()

    def get_filename_base(self):
        h = self.highlight_hex.get().replace('#','0x')
        s = self.shadow_hex.get().replace('#','0x')
        m = self.midtone_hex.get().replace('#','0x')
        space_str = self.space_sel.get().lower().replace(' ', '')
        return f"look_ev{round(self.exp_val.get(),1)}_s{int(self.sat_val.get())}_c{int(self.con_val.get())}_t{round(self.temp_val.get(),1)}_{h}_{m}_{s}_{space_str}"

    def gen_png(self):
        try:
            self.save_settings()
            rgb = calculate_lut_core(self.sat_val.get(), self.con_val.get(), 
                                     self.highlight_hex.get(), self.shadow_hex.get(), self.midtone_hex.get(),
                                     self.temp_val.get(), self.exp_val.get(), self.space_sel.get(), 64)
            
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
            messagebox.showinfo("Success", f"PNG saved:\n{path}")
        except Exception as e: messagebox.showerror("PNG Error", str(e))

    def gen_cube(self):
        try:
            self.save_settings()
            size = self.cube_sizes[self.cube_sel.get()]
            rgb = calculate_lut_core(self.sat_val.get(), self.con_val.get(), 
                                     self.highlight_hex.get(), self.shadow_hex.get(), self.midtone_hex.get(),
                                     self.temp_val.get(), self.exp_val.get(), self.space_sel.get(), size)
            
            path = os.path.join(self.output_dir.get(), self.get_filename_base() + f"_grid{size}.cube")
            write_cube_file(path, rgb)
            messagebox.showinfo("Success", f"CUBE saved:\n{path}")
        except Exception as e: messagebox.showerror("CUBE Error", str(e))

if __name__ == "__main__":
    root = tk.Tk(); app = LUTGeneratorGUI(root); root.mainloop()
import numpy as np
from PIL import Image
import colour
import os
import tkinter as tk
from tkinter import filedialog, messagebox

def create_512_hald_clut(cube_path, output_path):
    try:
        lut = colour.read_LUT(cube_path)
        
        # Resample to 64 points
        if lut.table.shape[0] != 64:
            new_size = 64
            new_table = lut.apply(colour.LUT3D.linear_table(new_size))
            lut_data = new_table
        else:
            lut_data = lut.table

        # Scale to 8-bit
        data = np.clip(lut_data, 0, 1)
        data = (data * 255).astype(np.uint8)

        # Fix inverted colors (BGR to RGB)
        data = data[..., ::-1]

        # Create Hald-CLUT image
        hald_image = np.zeros((512, 512, 3), dtype=np.uint8)
        for r in range(8):
            for c in range(8):
                blue_idx = r * 8 + c
                y_start, y_end = r * 64, (r + 1) * 64
                x_start, x_end = c * 64, (c + 1) * 64
                hald_image[y_start:y_end, x_start:x_end] = data[blue_idx]

        img = Image.fromarray(hald_image, mode='RGB')
        img.save(output_path)
        return True
    except Exception as e:
        print(f"Error processing {cube_path}: {e}")
        return False

def select_folder_and_convert():
    # Setup hidden root window for folder picker
    root = tk.Tk()
    root.withdraw()
    root.attributes("-topmost", True)
    
    folder_path = filedialog.askdirectory(title="Select Folder containing .cube files")
    
    if not folder_path:
        return

    files = [f for f in os.listdir(folder_path) if f.lower().endswith('.cube')]
    
    if not files:
        messagebox.showwarning("No Files", "No .cube files found in the selected folder.")
        return

    count = 0
    for filename in files:
        cube_file = os.path.join(folder_path, filename)
        png_file = os.path.splitext(cube_file)[0] + "_lut.png"
        if create_512_hald_clut(cube_file, png_file):
            count += 1

    messagebox.showinfo("Success", f"Done! Processed {count} files.")
    root.destroy()

if __name__ == "__main__":
    select_folder_and_convert()
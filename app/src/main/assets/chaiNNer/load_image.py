from __future__ import annotations
import os
import subprocess
from pathlib import Path
import cv2
import numpy as np
from PIL import Image
import tifffile
import struct

from nodes.impl.dds.texconv import dds_to_png_texconv
from nodes.impl.image_formats import (
    get_available_image_formats,
    get_opencv_formats,
    get_pil_formats,
)
from nodes.properties.inputs import ImageFileInput
from nodes.properties.outputs import DirectoryOutput, FileNameOutput, LargeImageOutput
from nodes.utils.utils import get_h_w_c, split_file_path
from .. import io_group

def get_gainmaps_from_dng(dng_path):
    try:
        with tifffile.TiffFile(dng_path) as tif:
            opcode_data = None
            for page in tif.pages:
                for tag_id in [51009, 51008, 51010]:
                    if tag_id in page.tags:
                        opcode_data = page.tags[tag_id].value
                        break
                if opcode_data: break
            
            if opcode_data is None: return None

            found_channels = 0
            search_pos = 0
            gain_grids = []

            while search_pos < len(opcode_data) - 40 and found_channels < 4:
                vals = struct.unpack('>fff', opcode_data[search_pos:search_pos+12])
                
                if abs(vals[0] - 1.4166666) < 0.001 and abs(vals[2] - 1.375) < 0.001:
                    rows = struct.unpack('>I', opcode_data[search_pos-8:search_pos-4])[0]
                    cols = struct.unpack('>I', opcode_data[search_pos-4:search_pos])[0]
                    
                    if rows == 0 or cols == 0 or rows > 1000 or cols > 1000:
                        search_pos += 4
                        continue

                    num_f = rows * cols
                    check_pos = search_pos + 12
                    found_start = -1
                    
                    while check_pos < len(opcode_data) - 4:
                        val = struct.unpack('>f', opcode_data[check_pos:check_pos+4])[0]
                        if val != 0.0:
                            found_start = check_pos
                            break
                        check_pos += 4
                    
                    if found_start != -1:
                        data_start = found_start + 4
                        if data_start + (num_f * 4) <= len(opcode_data):
                            raw_f = struct.unpack(f'>{num_f}f', opcode_data[data_start:data_start + num_f*4])
                            grid = np.array(raw_f).reshape((rows, cols))
                            gain_grids.append(grid)
                            found_channels += 1
                            search_pos = data_start + (num_f * 4)
                            continue
                
                search_pos += 4

            return gain_grids if len(gain_grids) == 4 else None
    except:
        return None

def get_ext(path: Path | str) -> str:
    return split_file_path(path)[2].lower()
    
def apply_gainmap_to_raw(raw, dng_path):
    try:
        gm_list = get_gainmaps_from_dng(dng_path)
        if not gm_list: return
        gain = np.stack(gm_list, axis=-1)
        raw_img = raw.raw_image_visible.astype(np.float32)
        h_raw, w_raw = raw_img.shape
        target_size = (w_raw // 2, h_raw // 2)
        gain_full = cv2.resize(gain, target_size, interpolation=cv2.INTER_LINEAR)
        black = raw.black_level_per_channel
        for i, (ro, co) in enumerate([(0,0), (0,1), (1,0), (1,1)]):
            b_lvl = black[i]
            ch = raw_img[ro::2, co::2]
            raw_img[ro::2, co::2] = ((ch - b_lvl) * gain_full[:, :, i]) + b_lvl
            
        raw.raw_image_visible[:] = np.clip(raw_img, 0, raw.white_level).astype(np.uint16)
        print(f"DEBUG: GainMap applied (Shape: {gain.shape[1]}x{gain.shape[0]})")
    except Exception as e:
        print(f"DEBUG ERROR: {e}")

def _read_pvc_raw(path: Path) -> np.ndarray | None:
    if get_ext(path) != ".txt": return None
    raw_path = path.with_suffix(".raw")
    if not raw_path.exists(): return None
    try:
        content = path.read_text()
        width, height, pix_fmt = 4096, 3072, "nv12"
        for line in content.splitlines():
            if "Resolution:" in line:
                res_part = line.split(":")[1].strip()
                width, height = map(int, res_part.split("x"))
            elif "Format:" in line:
                pix_fmt = line.split(":")[1].strip().lower()
        
        with open(raw_path, "rb") as f:
            raw_data = f.read()
        
        if pix_fmt == "nv12":
            yuv = np.frombuffer(raw_data, dtype=np.uint8).reshape((height * 3 // 2, width))
            return cv2.cvtColor(yuv, cv2.COLOR_YUV2BGR_NV12).astype(np.float32) / 255.0
        return None
    except: return None

def _read_dng(path: Path) -> np.ndarray | None:
    if get_ext(path) != ".dng": return None
    try:
        import rawpy
        with rawpy.imread(str(path)) as raw:
            apply_gainmap_to_raw(raw, str(path)) 

            rgb = raw.postprocess(use_camera_wb=True, bright=1.0, output_bps=16)

            img = cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR).astype(np.float32) / 65535.0
            return img
    except Exception as e:
        print(f"DEBUG: DNG/rawpy Error: {e}")
        return None

def _read_cv(path: Path) -> np.ndarray | None:
    if get_ext(path) in [".dng", ".raw"]: return None
    if get_ext(path) not in get_opencv_formats(): return None
    try:
        img = cv2.imdecode(np.fromfile(path, dtype=np.uint8), cv2.IMREAD_UNCHANGED)
        if img is None: return None
        denom = 255.0 if img.dtype == np.uint8 else 65535.0
        return img.astype(np.float32) / denom
    except: return None

def _read_pil(path: Path) -> np.ndarray | None:
    if get_ext(path) not in get_pil_formats(): return None
    try:
        im = Image.open(path)
        if im.mode == "P": im = im.convert(im.palette.mode)
        img = np.array(im)
        _, _, c = get_h_w_c(img)
        if c == 3: img = cv2.cvtColor(img, cv2.COLOR_RGB2BGR)
        elif c == 4: img = cv2.cvtColor(img, cv2.COLOR_RGBA2BGRA)
        return img.astype(np.float32) / 255.0
    except: return None

_decoders = [
    ("pvc-raw", _read_pvc_raw),
    ("dng-raw", _read_dng),
    ("cv", _read_cv),
    ("pil", _read_pil),
]

@io_group.register(
    schema_id="chainner:image:load",
    name="Load Image",
    description="Full Image Loader (PVC/DNG/GainMap C:\\Temp)",
    icon="BsFillImageFill",
    inputs=[ImageFileInput(primary_input=True)],
    outputs=[LargeImageOutput().suggest(), DirectoryOutput("Directory", of_input=0), FileNameOutput("Name", of_input=0)],
    side_effects=True,
)
def load_image_node(path: Path) -> tuple[np.ndarray, Path, str]:
    path_obj = Path(path)
    dn, bn, _ = split_file_path(path_obj)
    
    if get_ext(path_obj) == ".dds":
        path_obj = dds_to_png_texconv(path_obj)

    for _, decoder in _decoders:
        try:
            img = decoder(path_obj)
            if img is not None:
                return img, Path(dn), bn
        except: continue
        
    raise RuntimeError(f"Could not load image: {path}")
from __future__ import annotations
import os
import platform
import subprocess
from collections.abc import Callable, Iterable
from pathlib import Path
import cv2
import numpy as np
from PIL import Image

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

_Decoder = Callable[[Path], np.ndarray | None]

def get_ext(path: Path | str) -> str:
    return split_file_path(path)[2].lower()

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
            if "Format:" in line:
                if "10-bit P010" in line: pix_fmt = "p010le"
                elif "8-bit YUV420" in line: pix_fmt = "nv12"
        
        cmd = ['ffmpeg', '-f', 'rawvideo', '-pixel_format', pix_fmt, '-video_size', f'{width}x{height}', '-i', str(raw_path), '-f', 'image2pipe', '-vcodec', 'rawvideo', '-pix_fmt', 'bgr24', '-']
        process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        out, _ = process.communicate()
        if process.returncode != 0: return None
        return np.frombuffer(out, dtype=np.uint8).reshape((height, width, 3)).astype(np.float32) / 255.0
    except Exception: return None

def _read_dng(path: Path) -> np.ndarray | None:
    if get_ext(path) != ".dng": return None
    try:
        import rawpy
        with rawpy.imread(str(path)) as raw:
            rgb = raw.postprocess(use_camera_wb=True, no_auto_bright=True, bright=1.0, output_bps=8)
            bgr = cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR)
            return bgr.astype(np.float32) / 255.0
    except Exception as e:
        return None

def _read_cv(path: Path) -> np.ndarray | None:
    if get_ext(path) not in get_opencv_formats(): return None
    try:
        img = cv2.imdecode(np.fromfile(path, dtype=np.uint8), cv2.IMREAD_UNCHANGED)
        return img if img is not None else cv2.imread(str(path), cv2.IMREAD_UNCHANGED)
    except: return None

def _read_pil(path: Path) -> np.ndarray | None:
    if get_ext(path) not in get_pil_formats(): return None
    im = Image.open(path)
    if im.mode == "P": im = im.convert(im.palette.mode)
    img = np.array(im)
    _, _, c = get_h_w_c(img)
    if c == 3: img = cv2.cvtColor(img, cv2.COLOR_RGB2BGR)
    elif c == 4: img = cv2.cvtColor(img, cv2.COLOR_RGBA2BGRA)
    return img

_decoders: list[tuple[str, _Decoder]] = [
    ("pvc-raw", _read_pvc_raw),
    ("dng-raw", _read_dng),
    ("cv", _read_cv),
    ("pil", _read_pil),
]

@io_group.register(
    schema_id="chainner:image:load",
    name="Load Image",
    description="Load image (PVC/DNG support)",
    icon="BsFillImageFill",
    inputs=[ImageFileInput(primary_input=True)],
    outputs=[LargeImageOutput().suggest(), DirectoryOutput("Directory", of_input=0), FileNameOutput("Name", of_input=0)],
    side_effects=True,
)
def load_image_node(path: Path) -> tuple[np.ndarray, Path, str]:
    dirname, basename, _ = split_file_path(path)
    for _, decoder in _decoders:
        try:
            img = decoder(Path(path))
            if img is not None: return img, dirname, basename
        except: continue
    raise RuntimeError(f"Could not load image: {path}")
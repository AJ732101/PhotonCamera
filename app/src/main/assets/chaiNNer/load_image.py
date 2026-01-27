from __future__ import annotations
import os
import platform
import subprocess
from collections.abc import Callable, Iterable
from pathlib import Path
import cv2
import numpy as np
import pillow_avif  # type: ignore # noqa: F401
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

def remove_unnecessary_alpha(img: np.ndarray) -> np.ndarray:
    if get_h_w_c(img)[2] != 4:
        return img
    unnecessary = (
        (img.dtype == np.uint8 and np.all(img[:, :, 3] == 255))
        or (img.dtype == np.uint16 and np.all(img[:, :, 3] == 65536))
        or (img.dtype == np.float32 and np.all(img[:, :, 3] == 1.0))
        or (img.dtype == np.float64 and np.all(img[:, :, 3] == 1.0))
    )
    if unnecessary:
        return img[:, :, :3]
    return img

def _read_pvc_raw(path: Path) -> np.ndarray | None:
    if get_ext(path) != ".txt":
        return None
    raw_path = path.with_suffix(".raw")
    if not raw_path.exists():
        return None
    try:
        content = path.read_text()
        width, height, pix_fmt = 4096, 3072, "nv12"
        for line in content.splitlines():
            if "Resolution:" in line:
                res_part = line.split(":")[1].strip()
                width, height = map(int, res_part.split("x"))
            if "Format:" in line:
                if "10-bit P010" in line:
                    pix_fmt = "p010le"
                elif "8-bit YUV420" in line:
                    pix_fmt = "nv12"

        cmd = [
            'ffmpeg', '-f', 'rawvideo', '-pixel_format', pix_fmt,
            '-video_size', f'{width}x{height}', '-i', str(raw_path),
            '-f', 'image2pipe', '-vcodec', 'rawvideo', '-pix_fmt', 'bgr24', '-'
        ]
        
        process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        out, _ = process.communicate()
        
        if process.returncode != 0:
            return None
            
        img = np.frombuffer(out, dtype=np.uint8).reshape((height, width, 3))
        return img.astype(np.float32) / 255.0
    except Exception:
        return None

def _read_dng(path: Path) -> np.ndarray | None:
    if get_ext(path) != ".dng":
        return None
    try:
        cmd = [
            'ffmpeg', '-i', str(path),
            '-f', 'image2pipe', '-vcodec', 'rawvideo', '-pix_fmt', 'bgr24', '-'
        ]
        process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        out, _ = process.communicate()
        if process.returncode != 0:
            return None

        img_temp = cv2.imread(str(path))
        if img_temp is None: return None
        h, w, _ = img_temp.shape
        
        img = np.frombuffer(out, dtype=np.uint8).reshape((h, w, 3))
        return img.astype(np.float32) / 255.0
    except Exception:
        return None

def _read_cv(path: Path) -> np.ndarray | None:
    if get_ext(path) not in get_opencv_formats():
        return None
    try:
        img = cv2.imdecode(np.fromfile(path, dtype=np.uint8), cv2.IMREAD_UNCHANGED)
        if img is None:
            img = cv2.imread(str(path), cv2.IMREAD_UNCHANGED)
        return img
    except Exception:
        return None

def _read_pil(path: Path) -> np.ndarray | None:
    if get_ext(path) not in get_pil_formats():
        return None
    im = Image.open(path)
    if im.mode == "P":
        im = im.convert(im.palette.mode)
    img = np.array(im)
    _, _, c = get_h_w_c(img)
    if c == 3:
        img = cv2.cvtColor(img, cv2.COLOR_RGB2BGR)
    elif c == 4:
        img = cv2.cvtColor(img, cv2.COLOR_RGBA2BGRA)
    return img

def _read_dds(path: Path) -> np.ndarray | None:
    if get_ext(path) != ".dds" or platform.system() != "Windows":
        return None
    png = dds_to_png_texconv(path)
    try:
        img = _read_cv(Path(png))
        return remove_unnecessary_alpha(img) if img is not None else None
    finally:
        if os.path.exists(png):
            os.remove(png)

def _for_ext(ext: str | Iterable[str], decoder: _Decoder) -> _Decoder:
    ext_set = {ext} if isinstance(ext, str) else set(ext)
    return lambda path: decoder(path) if get_ext(path) in ext_set else None

_decoders: list[tuple[str, _Decoder]] = [
    ("pvc-raw", _read_pvc_raw),
    ("dng-raw", _read_dng),
    ("pil-jpeg", _for_ext([".jpg", ".jpeg"], _read_pil)),
    ("cv", _read_cv),
    ("texconv-dds", _read_dds),
    ("pil", _read_pil),
]

@io_group.register(
    schema_id="chainner:image:load",
    name="Load Image",
    description="Load image including PVC RAW via FFmpeg",
    icon="BsFillImageFill",
    inputs=[ImageFileInput(primary_input=True)],
    outputs=[
        LargeImageOutput().suggest(),
        DirectoryOutput("Directory", of_input=0),
        FileNameOutput("Name", of_input=0)
    ],
    side_effects=True,
)
def load_image_node(path: Path) -> tuple[np.ndarray, Path, str]:
    dirname, basename, _ = split_file_path(path)
    img = None
    for _, decoder in _decoders:
        try:
            img = decoder(Path(path))
        except Exception:
            continue
        if img is not None:
            break
    if img is None:
        raise RuntimeError(f"Could not load image: {path}")
    return img, dirname, basename
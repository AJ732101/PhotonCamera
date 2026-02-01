from __future__ import annotations
import subprocess
from pathlib import Path
import cv2
import numpy as np
from PIL import Image

from nodes.impl.dds.texconv import dds_to_png_texconv
from nodes.properties.inputs import ImageFileInput
from nodes.properties.outputs import DirectoryOutput, FileNameOutput, LargeImageOutput
from nodes.utils.utils import get_h_w_c, split_file_path
from .. import io_group

def get_ext(path: Path | str) -> str:
    return split_file_path(path)[2].lower()

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

def _read_pvc_raw_opencv(path: Path) -> np.ndarray | None:
    if get_ext(path) != ".txt":
        return None
    raw_path = path.with_suffix(".raw")
    if not raw_path.exists():
        return None
    try:
        content = path.read_text()
        width, height, is_10bit = 4096, 3072, False
        for line in content.splitlines():
            if "Resolution:" in line:
                res_part = line.split(":")[1].strip()
                width, height = map(int, res_part.split("x"))
            if "Format:" in line:
                if "10-bit P010" in line:
                    is_10bit = True
        
        y_size = width * height
        if is_10bit:
            raw_data = np.fromfile(raw_path, dtype=np.uint16)
            # P010 ist MSB-aligned, daher Shift um 6 Bits für 10-Bit Werte
            y = (raw_data[:y_size] >> 6).astype(np.float32) / 1023.0
            uv = (raw_data[y_size:] >> 6).astype(np.float32) / 1023.0
            y = y.reshape((height, width))
            uv = uv.reshape((height // 2, width // 2, 2))
            
            u = cv2.resize(uv[:, :, 0], (width, height), interpolation=cv2.INTER_LINEAR) - 0.5
            v = cv2.resize(uv[:, :, 1], (width, height), interpolation=cv2.INTER_LINEAR) - 0.5
            
            r = y + 1.402 * v
            g = y - 0.344136 * u - 0.714136 * v
            b = y + 1.772 * u
            rgb = np.stack([b, g, r], axis=-1)
        else:
            raw_data = np.fromfile(raw_path, dtype=np.uint8)
            y_plane = raw_data[:y_size].reshape((height, width))
            uv_plane = raw_data[y_size:].reshape((height // 2, width))
            nv12_map = np.vstack([y_plane, uv_plane])
            rgb = cv2.cvtColor(nv12_map, cv2.COLOR_YUV2BGR_NV12).astype(np.float32) / 255.0

        return np.clip(rgb, 0, 1)
    except Exception:
        return None

_decoders = [
    ("pvc-raw", _read_pvc_raw),
    ("pvc-raw", _read_pvc_raw_opencv),
]

@io_group.register(
    schema_id="chainner:image:load_pvc_yuv_raw",
    name="PVC YUV RAW Image",
    description="PhotonVidCam YUV RAW image loader",
    icon="BsFillImageFill",
    inputs=[ImageFileInput(primary_input=True)],
    outputs=[LargeImageOutput().suggest(), DirectoryOutput("Directory", of_input=0), FileNameOutput("Name", of_input=0)],
    side_effects=True,
)
def load_pvc_yuv_raw_node(path: Path) -> tuple[np.ndarray, Path, str]:
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
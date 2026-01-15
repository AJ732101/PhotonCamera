import os
import subprocess
import glob
import re

def process_raw_files():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    output_dir = os.path.join(script_dir, "Converted")

    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    
    raw_files = glob.glob(os.path.join(script_dir, "*.raw"))
    
    for raw_path in raw_files:
        base_name = os.path.splitext(os.path.basename(raw_path))[0]
        txt_path = os.path.join(script_dir, f"{base_name}.txt")

        if os.path.exists(txt_path):
            try:
                with open(txt_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                ffmpeg_cmd = None
                found_section = False
                for line in content.splitlines():
                    if "ENCODING AVIF" in line:
                        found_section = True
                    if found_section and line.strip().startswith("ffmpeg"):
                        ffmpeg_cmd = line.strip()
                        break
                
                if ffmpeg_cmd:
                    output_filename = f"{base_name}.avif"
                    output_path = os.path.join(output_dir, output_filename)
                    
                    parts = ffmpeg_cmd.rsplit('"', 2)
                    if len(parts) >= 3:
                        new_cmd = f'{parts[0]}"{output_path}"{parts[2]}'
                    else:
                        new_cmd = f'{ffmpeg_cmd.rsplit(" ", 1)[0]} "{output_path}"'

                    subprocess.run(new_cmd, shell=True, check=True, cwd=script_dir)

                    exif_cmd = ["exiftool", "-overwrite_original"]
                    
                    m_soft = re.search(r"PhotonVidCam Version: .*", content)
                    if not m_soft:
                        m_soft = re.search(r"PhotonCamera Version: .*", content)
                    
                    if m_soft:
                        val = m_soft.group(0)
                        exif_cmd.extend([f"-Software={val}", f"-XMP:Software={val}"])

                    m_brand = re.search(r"Brand: (.*)", content)
                    if m_brand:
                        val = m_brand.group(1).strip()
                        exif_cmd.extend([f"-Make={val}", f"-XMP:Make={val}"])

                    m_model = re.search(r"Model: (.*)", content)
                    if m_model:
                        val = m_model.group(1).strip()
                        exif_cmd.extend([f"-Model={val}", f"-XMP:Model={val}"])

                    m_dev = re.search(r"Device: (.*)", content)
                    if m_dev:
                        val = m_dev.group(1).strip()
                        exif_cmd.extend([f"-ModelRelease={val}", f"-XMP:Identifier={val}"])

                    m_ap = re.search(r"Aperture: F([\d.]+)", content)
                    if m_ap:
                        val = m_ap.group(1)
                        exif_cmd.extend([f"-FNumber={val}", f"-XMP:FNumber={val}"])

                    m_iso = re.search(r"ISO: (\d+)", content)
                    if m_iso:
                        val = m_iso.group(1)
                        exif_cmd.extend([f"-ISO={val}", f"-XMP:ISO={val}"])

                    m_exp = re.search(r"Exposure Time: ([\d/]+)", content)
                    if m_exp:
                        val = m_exp.group(1)
                        exif_cmd.extend([f"-ExposureTime={val}", f"-XMP:ExposureTime={val}"])

                    m_foc = re.search(r"Focal Length: ([\d.]+)", content)
                    if m_foc:
                        val = m_foc.group(1)
                        exif_cmd.extend([f"-FocalLength={val}", f"-XMP:FocalLength={val}"])

                    m_foc35 = re.search(r"Focal Length 35mm: (\d+)", content)
                    if m_foc35:
                        val = m_foc35.group(1)
                        exif_cmd.extend([f"-FocalLengthIn35mmFormat={val}", f"-XMP:FocalLengthIn35mmFormat={val}"])

                    exif_cmd.append(output_path)
                    subprocess.run(exif_cmd, check=True)
                    print(f"Done: {output_filename}")
                    
            except Exception as e:
                print(f"Error {base_name}: {e}")

if __name__ == "__main__":
    process_raw_files()
import os
import subprocess
import glob
import re

def play_raw_files():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    raw_files = glob.glob(os.path.join(script_dir, "*.raw"))
    
    if not raw_files:
        print(f"Keine .raw Dateien in {script_dir} gefunden.")
        return

    for raw_path in raw_files:
        base_name = os.path.splitext(os.path.basename(raw_path))[0]
        txt_path = os.path.join(script_dir, f"{base_name}.txt")

        if os.path.exists(txt_path):
            try:
                with open(txt_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                ffplay_cmd = None
                found_section = False
                for line in content.splitlines():
                    if "VIEWING" in line:
                        found_section = True
                    if found_section and line.strip().startswith("ffplay"):
                        ffplay_cmd = line.strip()
                        break
                
                if ffplay_cmd:
                    print(f"Starte Wiedergabe: {base_name}")
                    subprocess.run(ffplay_cmd, shell=True, cwd=script_dir)
                else:
                    print(f"Kein ffplay-Befehl in {txt_path} gefunden.")
                    
            except Exception as e:
                print(f"Fehler bei {base_name}: {e}")
        else:
            print(f"Überspringe {base_name}: Keine Metadaten gefunden.")

if __name__ == "__main__":
    play_raw_files()
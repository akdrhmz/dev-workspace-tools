import os
import shutil
import glob
import re

providers_map = {
    "DiziBox": "dizibox",
    "Dizilla": "dizilla",
    "FilmMakinesi": "filmmakinesi",
    "HDFilmCehennemi": "hdfilmcehennemi",
    "JetFilmizle": "jetfilmizle",
    "Sinewix": "sinewix"
}

feroxx_dir = "../feroxx_repo"

for src_name, pkg_name in providers_map.items():
    dest_provider_name = src_name + "Provider"
    if src_name == "Sinewix":
        dest_provider_name = "SineWixProvider"
        
    src_kotlin_dir = os.path.join(feroxx_dir, src_name, "src", "main", "kotlin", "com", "keyiflerolsun")
    dest_kotlin_dir = os.path.join(dest_provider_name, "src", "main", "kotlin", "com", "kekik", pkg_name)
    
    if os.path.exists(dest_kotlin_dir):
        shutil.rmtree(dest_kotlin_dir)
    
    os.makedirs(dest_kotlin_dir, exist_ok=True)
    
    for kt_file in glob.glob(os.path.join(src_kotlin_dir, "*.kt")):
        filename = os.path.basename(kt_file)
        
        with open(kt_file, "r", encoding="utf-8") as f:
            content = f.read()
            
        content = re.sub(r"// ! Bu araç @keyiflerolsun.*", "", content)
        content = re.sub(r"@keyiflerolsun", "", content)
        content = re.sub(r"@KekikAkademi", "", content)
        content = re.sub(r"package com\.keyiflerolsun", f"package com.kekik.{pkg_name}", content)
        
        dest_file = os.path.join(dest_kotlin_dir, filename)
        with open(dest_file, "w", encoding="utf-8") as f:
            f.write(content)
            
    print(f"Synced {src_name} to {dest_provider_name}")


import urllib.request
import json
import os
import re
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
UPSTREAM_TREE_URL = "https://api.github.com/repos/keyiflerolsun/Kekik-cloudstream/git/trees/master?recursive=1"

req = urllib.request.Request(UPSTREAM_TREE_URL, headers={"User-Agent": "Mozilla/5.0"})
with urllib.request.urlopen(req) as resp:
    tree = json.loads(resp.read().decode("utf-8"))["tree"]

providers_to_sync = {
    "DiziBox": "DiziBoxProvider",
    "FilmMakinesi": "FilmMakinesiProvider",
    "HDFilmCehennemi": "HDFilmCehennemiProvider",
    "Dizilla": "DizillaProvider",
    "SineWix": "SineWixProvider",
    "JetFilmizle": "JetFilmizleProvider",
    "Dizifon": "DizifonProvider"
}

for item in tree:
    path = item["path"]
    for prov_key, target_mod in providers_to_sync.items():
        if path.startswith(f"{prov_key}/src/main/kotlin/"):
            raw_url = f"https://raw.githubusercontent.com/keyiflerolsun/Kekik-cloudstream/master/{path}"
            fname = os.path.basename(path)
            print(f"[+] Upstream indiriliyor: {prov_key} -> {fname}")
            try:
                with urllib.request.urlopen(urllib.request.Request(raw_url, headers={"User-Agent": "Mozilla/5.0"})) as r:
                    code = r.read().decode("utf-8")
                
                # Anonimize et ve paket yapısını koru
                pkg_name = target_mod.lower().replace("provider", "").replace("-", "")
                code = re.sub(r'package\s+com\.keyiflerolsun(?:\.[a-zA-Z0-9_]+)?', f'package com.kekik.{pkg_name}', code)
                code = re.sub(r'import\s+com\.keyiflerolsun\.', f'import com.kekik.{pkg_name}.', code)
                code = re.sub(r'//\s*!\s*Bu\s*araç.*', '', code)
                code = re.sub(r'SubtitleFile\(', 'com.lagradost.cloudstream3.newSubtitleFile(', code)
                
                target_dir = BASE_DIR / target_mod / "src" / "main" / "kotlin" / "com" / "kekik" / pkg_name
                target_dir.mkdir(parents=True, exist_ok=True)
                target_file = target_dir / fname
                with open(target_file, "w", encoding="utf-8") as f:
                    f.write(code)
                print(f"[OK] Kaydedildi: {target_file}")
            except Exception as e:
                print(f"[-] Hata ({path}): {e}")
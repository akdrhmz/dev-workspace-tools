#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AtlasStream Upstream Auto-Sync Script
Her 6 saatte bir upstream depoları (nik-cloudstream, Kekik-cloudstream vb.)
kontrol eder, yeni/güncellenen kaynakları AtlasStream içine izole paketler olarak aktarır.
"""

import os
import re
import sys
import shutil
import subprocess
from pathlib import Path

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

BASE_DIR = Path(__file__).resolve().parent.parent
ATLAS_SRC_DIR = BASE_DIR / "AtlasStream" / "src" / "main" / "kotlin" / "com" / "kekik" / "atlasstream"
PROVIDERS_DIR = ATLAS_SRC_DIR / "providers"

EXCLUDED_PROVIDERS = {
    "animecix", "asyaanimeleri", "turkanime", "canlitv", "inatbox",
    "rectv", "vavoospor", "selcukflix", "__temel", "gradle", ".github"
}

UPSTREAM_REPOS = [
    ("nikstream", "https://github.com/nikyokki/nik-cloudstream.git"),
    ("kekik", "https://github.com/feroxx/Kekik-cloudstream.git")
]

def to_safe_ascii(s: str) -> str:
    s = s.lower()
    for tr, en in [("ı", "i"), ("i̇", "i"), ("ü", "u"), ("ö", "o"), ("ç", "c"), ("ş", "s"), ("ğ", "g")]:
        s = s.replace(tr, en)
    return re.sub(r"[^a-z0-9_]", "", s)

def sync_upstreams():
    temp_dir = BASE_DIR / "temp_upstreams"
    os.makedirs(temp_dir, exist_ok=True)
    os.makedirs(PROVIDERS_DIR, exist_ok=True)

    cloned_paths = []
    for name, url in UPSTREAM_REPOS:
        target = temp_dir / name
        if not target.exists():
            print(f"[*] Klonlanıyor: {name} ({url})")
            subprocess.run(["git", "clone", "--depth", "1", url, str(target)], check=False)
        else:
            print(f"[*] Güncelleniyor: {name}")
            subprocess.run(["git", "-C", str(target), "pull"], check=False)
        if target.exists():
            cloned_paths.append(target)

    # Kaynakları tara
    found_providers = {}

    for repo_path in cloned_paths:
        for item in repo_path.iterdir():
            if not item.is_dir() or item.name.startswith(".") or item.name.startswith("__"):
                continue
            
            p_name = item.name
            safe_name = to_safe_ascii(p_name)
            if safe_name in EXCLUDED_PROVIDERS or safe_name in found_providers:
                continue

            kt_dir = item / "src" / "main" / "kotlin"
            if not kt_dir.exists():
                continue

            # Kotlin dosyalarını topla
            kt_files = [f for f in kt_dir.rglob("*.kt") if not f.name.endswith("Plugin.kt")]
            if not kt_files:
                continue

            dest_pkg_dir = PROVIDERS_DIR / safe_name
            os.makedirs(dest_pkg_dir, exist_ok=True)

            main_class = None

            for f in kt_files:
                dest_file_name = to_safe_ascii(f.stem) + ".kt"
                dest_file = dest_pkg_dir / dest_file_name

                try:
                    with open(f, "r", encoding="utf-8", errors="ignore") as in_f:
                        code = in_f.read()

                    # Package bildirgesini güncelle
                    code = re.sub(r"(?m)^package\s+[a-zA-Z0-9_\.]+", f"package com.kekik.atlasstream.providers.{safe_name}", code)

                    with open(dest_file, "w", encoding="utf-8") as out_f:
                        out_f.write(code)

                    # MainAPI sınıfını bul
                    cls_match = re.search(r"class\s+(\w+)\s*:\s*MainAPI", code)
                    if cls_match:
                        main_class = cls_match.group(1)
                except Exception as e:
                    print(f"[-] Hata ({f}): {e}")

            if main_class:
                found_providers[safe_name] = {
                    "name": p_name,
                    "sub_pkg": safe_name,
                    "class_name": main_class,
                    "full_qualified": f"com.kekik.atlasstream.providers.{safe_name}.{main_class}"
                }
                print(f"[+] Senkronize edildi: {p_name} -> {safe_name}.{main_class}")

    print(f"[OK] Toplam senkronize edilen kaynak: {len(found_providers)}")

    # AtlasStreamPlugin.kt ve SourceResolver.kt güncelle
    update_plugin_and_resolver(found_providers)

    # Geçici klasörü temizle
    try:
        shutil.rmtree(temp_dir, ignore_errors=True)
    except Exception:
        pass

def update_plugin_and_resolver(providers: dict):
    # 1. AtlasStreamPlugin.kt
    plugin_kt = ATLAS_SRC_DIR / "AtlasStreamPlugin.kt"
    if plugin_kt.exists():
        with open(plugin_kt, "r", encoding="utf-8") as f:
            p_content = f.read()

        core_sources = [
            '            "DiziBox"          to "DiziBox (Yabanci Dizi)",\n',
            '            "Dizilla"          to "Dizilla (Yabanci Dizi)",\n',
            '            "FilmMakinesi"     to "FilmMakinesi (Film Odakli)",\n',
            '            "HDFilmCehennemi"  to "HDFilmCehennemi (Genis Arsiv)",\n',
            '            "SineWix"          to "SineWix (Dizi & Film)",\n',
            '            "JetFilmizle"      to "JetFilmizle (Yerli/Yabanci Film)",\n',
            '            "DiziPal"          to "DiziPal (Genis Arsiv)",\n',
            '            "FullHDFilmizlesene" to "FullHDFilmizlesene (Film Odakli)",\n',
            '            "FilmModu"         to "FilmModu (Film Odakli)",\n',
            '            "SezonlukDizi"     to "SezonlukDizi (Dizi Odakli)",\n',
            '            "CizgiMax"         to "CizgiMax (Cizgi Dizi/Film)",\n'
        ]

        dynamic_sources = []
        for p in providers.values():
            dynamic_sources.append(f'            "{p["name"]}" to "{p["name"]} (Dinamik Kaynak)",\n')

        all_sources_block = "        val ALL_SOURCES = linkedMapOf(\n" + "".join(core_sources) + "".join(dynamic_sources) + "        )"
        p_content = re.sub(r"(?s)val ALL_SOURCES = linkedMapOf\(.*?\)", all_sources_block, p_content)

        with open(plugin_kt, "w", encoding="utf-8") as f:
            f.write(p_content)
        print("[OK] AtlasStreamPlugin.kt güncellendi.")

    # 2. SourceResolver.kt
    resolver_kt = ATLAS_SRC_DIR / "SourceResolver.kt"
    if resolver_kt.exists():
        with open(resolver_kt, "r", encoding="utf-8") as f:
            r_content = f.read()

        instantiations = [f"            {p['full_qualified']}()" for p in providers.values()]
        inst_block = "    private val externalProviders by lazy {\n        listOf(\n" + ",\n".join(instantiations) + "\n        )\n    }"

        r_content = re.sub(r"(?s)private val externalProviders by lazy \{.*?\}", inst_block, r_content)

        with open(resolver_kt, "w", encoding="utf-8") as f:
            f.write(r_content)
        print("[OK] SourceResolver.kt güncellendi.")

if __name__ == "__main__":
    sync_upstreams()

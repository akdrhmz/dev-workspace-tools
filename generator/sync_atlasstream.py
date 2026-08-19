#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AtlasStream Universal Smart Sync Script (Safe Identifier & Package Injection)
"""

import os
import re
import sys
import shutil
import hashlib
import subprocess
from pathlib import Path

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

BASE_DIR = Path(__file__).resolve().parent.parent
ATLAS_SRC_DIR = BASE_DIR / "AtlasStream" / "src" / "main" / "kotlin" / "com" / "kekik" / "atlasstream"
PROVIDERS_DIR = ATLAS_SRC_DIR / "providers"

EXCLUDED_PROVIDERS = {
    "__temel", "temel", "gradle", ".github", ".idea", ".gradle", "core", "test", "build", "out",
    "dizipaloriginal", "hdfilmcehennemi2",
    "4kfilmizlesene", "p4kfilmizlesene"
}

UPSTREAM_REPOS = [
    ("nikstream", "https://github.com/nikyokki/nik-cloudstream.git"),
    ("kekik", "https://github.com/feroxx/Kekik-cloudstream.git")
]

def to_safe_ascii(s: str) -> str:
    s = s.lower()
    for tr, en in [("ı", "i"), ("i̇", "i"), ("ü", "u"), ("ö", "o"), ("ç", "c"), ("ş", "s"), ("ğ", "g")]:
        s = s.replace(tr, en)
    s = re.sub(r"[^a-z0-9_]", "", s)
    if s and s[0].isdigit():
        s = "p" + s
    return s

def file_hash(filepath: Path) -> str:
    if not filepath.exists():
        return ""
    h = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(8192):
            h.update(chunk)
    return h.hexdigest()

def sync_upstreams():
    temp_dir = BASE_DIR / "temp_upstreams"
    os.makedirs(temp_dir, exist_ok=True)
    os.makedirs(PROVIDERS_DIR, exist_ok=True)

    git_cmd = shutil.which("git") or r"C:\Program Files\Git\bin\git.exe" or "git"
    cloned_paths = []
    for name, url in UPSTREAM_REPOS:
        target = temp_dir / name
        if not target.exists():
            print(f"[*] Klonlanıyor: {name} ({url})")
            subprocess.run([git_cmd, "clone", "--depth", "1", url, str(target)], check=False)
        else:
            print(f"[*] Güncelleniyor: {name}")
            subprocess.run([git_cmd, "-C", str(target), "pull"], check=False)
        if target.exists():
            cloned_paths.append(target)

    found_providers = {}
    updated_count = 0
    unchanged_count = 0

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

            kt_files = [f for f in kt_dir.rglob("*.kt") if not f.name.endswith("Plugin.kt")]
            if not kt_files:
                continue

            dest_pkg_dir = PROVIDERS_DIR / safe_name
            os.makedirs(dest_pkg_dir, exist_ok=True)

            main_class = None
            is_provider_changed = False

            for f in kt_files:
                dest_file_name = to_safe_ascii(f.stem) + ".kt"
                dest_file = dest_pkg_dir / dest_file_name

                try:
                    with open(f, "r", encoding="utf-8", errors="ignore") as in_f:
                        code = in_f.read()

                    # Package temizleme ve güvenli yerleştirme
                    code = re.sub(r"(?m)^package\s+.*$", "", code)
                    code = re.sub(r"(?m)^import\s+com\.keyiflerolsun\..*$", "", code)
                    code = re.sub(r"(?m)^import\s+com\.kekik\..*$", "", code)
                    code = re.sub(r"(?m)^import\s+com\.nikyokki\..*$", "", code)
                    code = re.sub(r"(?m)^import\s+[A-Za-z0-9_]+\s*$", "", code)
                    
                    # TurkAnime upstream coroutine suspend fix
                    code = code.replace("private fun iframe2AesLink(", "private suspend fun iframe2AesLink(")

                    code = f"package com.kekik.atlasstream.providers.{safe_name}\n\n" + code.strip()

                    existing_hash = file_hash(dest_file)
                    new_hash = hashlib.sha256(code.encode("utf-8")).hexdigest()

                    if existing_hash != new_hash:
                        with open(dest_file, "w", encoding="utf-8") as out_f:
                            out_f.write(code)
                        is_provider_changed = True

                    cls_match = re.search(r"class\s+(\w+)\s*:\s*MainAPI", code)
                    if cls_match:
                        main_class = cls_match.group(1)
                except Exception as e:
                    print(f"[-] Hata ({f}): {e}")

            if is_provider_changed:
                print(f"[+] [GÜNCELLEME ALINDI] {p_name} -> Değişiklikler entegre edildi.")
                updated_count += 1
            else:
                unchanged_count += 1

            if main_class:
                found_providers[safe_name] = {
                    "name": p_name,
                    "sub_pkg": safe_name,
                    "class_name": main_class,
                    "full_qualified": f"com.kekik.atlasstream.providers.{safe_name}.{main_class}"
                }

    print(f"[BİLGİ] {unchanged_count} kaynakta değişiklik yok (korundu).")
    print(f"[BİLGİ] {updated_count} kaynak güncellendi.")

    if updated_count > 0:
        update_plugin_and_resolver(found_providers)

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

        all_sources_entries = []
        for p in sorted(providers.values(), key=lambda x: x["name"].lower()):
            all_sources_entries.append(f'            "{p["name"]}" to "{p["name"]}",\n')

        all_sources_block = "val ALL_SOURCES = linkedMapOf(\n" + "".join(all_sources_entries) + "        )"
        p_content = re.sub(r"(?s)val ALL_SOURCES = linkedMapOf\(.*?\n\s*\)", all_sources_block, p_content)

        with open(plugin_kt, "w", encoding="utf-8") as f:
            f.write(p_content)
        print("[OK] AtlasStreamPlugin.kt güncellendi.")

    # 2. SourceResolver.kt
    resolver_kt = ATLAS_SRC_DIR / "SourceResolver.kt"
    if resolver_kt.exists():
        with open(resolver_kt, "r", encoding="utf-8") as f:
            r_content = f.read()

        instantiations = [f"            {p['full_qualified']}()" for p in sorted(providers.values(), key=lambda x: x["name"].lower())]
        inst_block = "private val externalProviders: List<com.lagradost.cloudstream3.MainAPI> by lazy {\n        listOf(\n" + ",\n".join(instantiations) + "\n        )\n    }"

        r_content = re.sub(r"(?s)private val externalProviders:?.*? by lazy \{.*?\}", inst_block, r_content)

        with open(resolver_kt, "w", encoding="utf-8") as f:
            f.write(r_content)
        print("[OK] SourceResolver.kt güncellendi.")

if __name__ == "__main__":
    sync_upstreams()

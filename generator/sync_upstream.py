#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Workspace Plugin Sync Script
"""

import os
import json
import urllib.request
from pathlib import Path
from transpiler import parse_python_plugin, generate_kotlin_module

import sys
if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

BASE_DIR = Path(__file__).resolve().parent.parent

UPSTREAM_API_URL = "https://api.github.com/repos/keyiflerolsun/KekikStream/contents/KekikStream/Plugins"
RAW_BASE_URL = "https://raw.githubusercontent.com/keyiflerolsun/KekikStream/main/KekikStream/Plugins"

def sync_plugins():
    print(f"[*] Upstream kontrol ediliyor: {UPSTREAM_API_URL}")
    req = urllib.request.Request(
        UPSTREAM_API_URL,
        headers={"User-Agent": "KekikStream-Sync-Bot"}
    )

    try:
        with urllib.request.urlopen(req) as resp:
            data = json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"[!] Upstream listesi alinirken hata olustu (Fallback yerel kurallara geciliyor): {e}")
        return

    new_modules = []
    all_plugin_infos = []

    for item in data:
        name = item.get("name", "")
        if name.endswith(".py") and not name.startswith("__"):
            raw_url = f"{RAW_BASE_URL}/{name}"
            cls_name = name.replace(".py", "")
            mod_name = f"{cls_name}Provider"
            
            # Eger modul zaten ozel olarak gelistirilmisse veya mevcutsa uzerine yazma
            if (BASE_DIR / mod_name).exists():
                print(f"[i] {mod_name} zaten ozel olarak mevcut, korundu.")
                new_modules.append(f":{mod_name}")
                continue

            print(f"[+] Yeni upstream modul indiriliyor: {name}")
            try:
                with urllib.request.urlopen(raw_url) as raw_resp:
                    content = raw_resp.read().decode("utf-8")
                info = parse_python_plugin(content, name)
                all_plugin_infos.append(info)
                generate_kotlin_module(info, BASE_DIR)
                new_modules.append(f":{info['class_name']}Provider")
            except Exception as ex:
                print(f"[-] {name} donusturulurken hata: {ex}")

    # Otomatik Kategori Sınıflandırmasını Çalıştır
    if all_plugin_infos:
        from transpiler import update_category_definitions
        update_category_definitions(all_plugin_infos, BASE_DIR)

    # settings.gradle.kts dosyasını güncelle
    if new_modules:
        update_settings_gradle(new_modules)

def update_settings_gradle(new_modules: list):
    settings_file = BASE_DIR / "settings.gradle.kts"
    if not settings_file.exists():
        return

    with open(settings_file, "r", encoding="utf-8") as f:
        content = f.read()

    for mod in new_modules:
        include_stmt = f'include("{mod}")'
        if include_stmt not in content:
            content += f"\n{include_stmt}"

    with open(settings_file, "w", encoding="utf-8") as f:
        f.write(content)
    print("[OK] settings.gradle.kts guncellendi.")

if __name__ == "__main__":
    sync_plugins()

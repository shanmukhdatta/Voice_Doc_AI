#!/usr/bin/env python3
"""
Downloads the official MedlinePlus XML dataset and records exact provenance.
"""

import os
import sys
import re
import json
import hashlib
import requests
from tqdm import tqdm

OFFICIAL_XML_URL = "https://medlineplus.gov/xml/mplus_topics_2026-10-31.xml"
ENTRY_URL = "https://medlineplus.gov/xml.html"
DATA_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "medical-knowledge", "raw_source")
OUTPUT_XML = os.path.join(DATA_DIR, "mplus_topics_2026-10-31.xml")

def download_file(url, target_path):
    os.makedirs(os.path.dirname(target_path), exist_ok=True)
    if os.path.exists(target_path) and os.path.getsize(target_path) > 1000000:
        print(f"File already exists at {target_path} ({os.path.getsize(target_path)} bytes)")
        return target_path

    print(f"Downloading official MedlinePlus XML from {url}...")
    headers = {"User-Agent": "VoiceDoctorAI-MedicalPackBuilder/1.0"}
    with requests.get(url, headers=headers, stream=True, timeout=60) as r:
        r.raise_for_status()
        total_size = int(r.headers.get("content-length", 0))
        with open(target_path, "wb") as f, tqdm(
            desc="Downloading XML",
            total=total_size,
            unit="iB",
            unit_scale=True,
            unit_divisor=1024,
        ) as bar:
            for chunk in r.iter_content(chunk_size=65536):
                size = f.write(chunk)
                bar.update(size)

    return target_path

def compute_sha256(path):
    sha = hashlib.sha256()
    with open(path, "rb") as f:
        while chunk := f.read(65536):
            sha.update(chunk)
    return sha.hexdigest()

def main():
    download_file(OFFICIAL_XML_URL, OUTPUT_XML)
    sha256_hash = compute_sha256(OUTPUT_XML)
    file_size = os.path.getsize(OUTPUT_XML)

    print(f"Downloaded XML SHA-256: {sha256_hash}")
    print(f"File size: {file_size} bytes ({file_size / (1024*1024):.2f} MB)")

    manifest = {
        "dataset_name": "MedlinePlus Health Topics Official XML Release",
        "official_entry_url": ENTRY_URL,
        "source_download_url": OFFICIAL_XML_URL,
        "release_date": "2026-10-31",
        "license": "Public Domain (17 U.S.C. § 105 - Works of the United States Government)",
        "publisher": "National Library of Medicine (NLM), U.S. Department of Health and Human Services",
        "attribution_requirement": "MedlinePlus, National Library of Medicine, U.S. Department of Health and Human Services",
        "file_name": os.path.basename(OUTPUT_XML),
        "file_size_bytes": file_size,
        "sha256": sha256_hash,
        "review_status": "source_verified_clinical_review_pending"
    }

    manifest_path = os.path.join(os.path.dirname(__file__), "..", "..", "medical-knowledge", "sources_manifest.json")
    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2)

    print(f"Updated {manifest_path}")

if __name__ == "__main__":
    main()

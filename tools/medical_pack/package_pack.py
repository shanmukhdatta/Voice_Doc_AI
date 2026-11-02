#!/usr/bin/env python3
"""
Packages the offline knowledge pack assets and generates knowledge_pack_manifest.json.
"""

import os
import sys
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
os.environ["PYTHONIOENCODING"] = "utf-8"

import json
import hashlib

DATA_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "medical-knowledge")
MODEL_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "model_assets", "minilm")
MANIFEST_PATH = os.path.join(DATA_DIR, "knowledge_pack_manifest.json")

PACK_FILES = [
    os.path.join(DATA_DIR, "medical_knowledge.db"),
    os.path.join(DATA_DIR, "medical_embeddings.bin"),
    os.path.join(DATA_DIR, "passage_ids.json"),
    os.path.join(DATA_DIR, "passages.jsonl"),
    os.path.join(MODEL_DIR, "model_quantized.onnx"),
    os.path.join(MODEL_DIR, "vocab.txt")
]

def compute_sha256(path):
    sha = hashlib.sha256()
    with open(path, "rb") as f:
        while chunk := f.read(65536):
            sha.update(chunk)
    return sha.hexdigest()

def main():
    print("Generating knowledge pack manifest...")
    file_records = []
    total_bytes = 0

    for p in PACK_FILES:
        if not os.path.exists(p):
            print(f"ERROR: missing pack asset {p}")
            sys.exit(1)
        size = os.path.getsize(p)
        sha = compute_sha256(p)
        total_bytes += size
        file_records.append({
            "name": os.path.basename(p),
            "size_bytes": size,
            "size_mb": round(size / (1024 * 1024), 2),
            "sha256": sha
        })

    manifest = {
        "pack_name": "MedlinePlus_HealthTopics_RAG_Pack",
        "pack_version": "2026.10.31",
        "release_date": "2026-10-31",
        "schema_version": "1.0",
        "source": "Official MedlinePlus English Health Topics XML Release",
        "publisher": "National Library of Medicine (NLM), U.S. Department of Health and Human Services",
        "license": "Public Domain (17 U.S.C. § 105 - Works of the United States Government)",
        "clinical_review_status": "source_verified_clinical_review_pending",
        "embedding_model": {
            "model_id": "sentence-transformers/all-MiniLM-L6-v2",
            "vector_dimension": 384,
            "quantization": "INT8 dynamic",
            "pooling": "masked_mean_pooling",
            "normalization": "L2",
            "max_seq_length": 256
        },
        "statistics": {
            "total_topics": 1016,
            "total_passages": 1016,
            "total_asset_size_mb": round(total_bytes / (1024 * 1024), 2)
        },
        "files": file_records
    }

    with open(MANIFEST_PATH, "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2)

    print(f"Created knowledge pack manifest at {MANIFEST_PATH}")
    print(f"Total knowledge pack size: {total_bytes / (1024 * 1024):.2f} MB")

if __name__ == "__main__":
    main()

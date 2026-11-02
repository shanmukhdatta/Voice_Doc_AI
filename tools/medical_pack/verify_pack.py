#!/usr/bin/env python3
"""
Verifies the cryptographic and structural integrity of the knowledge pack.
"""

import os
import sys
import json
import struct
import hashlib
import sqlite3
import numpy as np

DATA_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "medical-knowledge")
MODEL_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "model_assets", "minilm")
MANIFEST_PATH = os.path.join(DATA_DIR, "knowledge_pack_manifest.json")

def compute_sha256(path):
    sha = hashlib.sha256()
    with open(path, "rb") as f:
        while chunk := f.read(65536):
            sha.update(chunk)
    return sha.hexdigest()

def main():
    print(f"Reading manifest from {MANIFEST_PATH}...")
    with open(MANIFEST_PATH, "r", encoding="utf-8") as f:
        manifest = json.load(f)

    print("Verifying asset checksums...")
    for rec in manifest["files"]:
        fname = rec["name"]
        path = os.path.join(DATA_DIR, fname)
        if not os.path.exists(path):
            path = os.path.join(MODEL_DIR, fname)
        assert os.path.exists(path), f"File {fname} not found!"
        actual_sha = compute_sha256(path)
        assert actual_sha.lower() == rec["sha256"].lower(), f"Checksum mismatch for {fname}: expected {rec['sha256']}, got {actual_sha}"
        print(f"  [OK] {fname} ({rec['size_mb']} MB, SHA-256 verified)")

    print("Verifying binary vector matrix...")
    bin_path = os.path.join(DATA_DIR, "medical_embeddings.bin")
    with open(bin_path, "rb") as f:
        magic = f.read(4)
        assert magic == b"VEC1", "Invalid vector file magic"
        num_vecs, dim = struct.unpack("<II", f.read(8))
        assert num_vecs == 1016, f"Expected 1016 vectors, got {num_vecs}"
        assert dim == 384, f"Expected 384 dimensions, got {dim}"
        raw = f.read(num_vecs * dim * 4)
        vecs = np.frombuffer(raw, dtype=np.float32).reshape((num_vecs, dim))
        assert not np.isnan(vecs).any(), "Found NaNs in embedding matrix"
        assert not np.isinf(vecs).any(), "Found Infs in embedding matrix"
        norms = np.linalg.norm(vecs, axis=1)
        assert np.allclose(norms, 1.0, atol=1e-3), "Vectors are not L2 normalized"
    print(f"  [OK] 1016 normalized 384-D vectors verified (no NaNs, all L2 norm ~ 1.0)")

    print("Verifying SQLite knowledge database...")
    db_path = os.path.join(DATA_DIR, "medical_knowledge.db")
    conn = sqlite3.connect(db_path)
    cur = conn.cursor()
    cur.execute("SELECT count(*) FROM passages")
    count = cur.fetchone()[0]
    assert count == 1016, f"Expected 1016 rows in passages table, got {count}"
    cur.execute("SELECT count(*) FROM passages_fts")
    fts_count = cur.fetchone()[0]
    assert fts_count == 1016, f"Expected 1016 rows in passages_fts table, got {fts_count}"
    conn.close()
    print(f"  [OK] SQLite passages table ({count} rows) and FTS index verified")

    print("\nALL KNOWLEDGE PACK ASSETS VERIFIED SUCCESSFULLY!")

if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""
Encodes all passages into normalized 384-d vectors using quantized INT8 ONNX MiniLM
and outputs a compact binary vector file and passage ID mapping.
"""

import os
import sys
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
os.environ["PYTHONIOENCODING"] = "utf-8"

import json
import struct
import numpy as np
import onnxruntime as ort
from transformers import AutoTokenizer
from tqdm import tqdm

DATA_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "medical-knowledge")
PASSAGES_PATH = os.path.join(DATA_DIR, "passages.jsonl")
MODEL_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "model_assets", "minilm")
ONNX_PATH = os.path.join(MODEL_DIR, "model_quantized.onnx")

BINARY_VECTORS_PATH = os.path.join(DATA_DIR, "medical_embeddings.bin")
PASSAGE_IDS_PATH = os.path.join(DATA_DIR, "passage_ids.json")

def mean_pool_and_normalize(token_embeddings, attention_mask):
    input_mask_expanded = np.expand_dims(attention_mask, -1).astype(np.float32)
    sum_embeddings = np.sum(token_embeddings * input_mask_expanded, axis=1)
    sum_mask = np.clip(input_mask_expanded.sum(axis=1), a_min=1e-9, a_max=None)
    mean_pooled = sum_embeddings / sum_mask
    norm = np.linalg.norm(mean_pooled, axis=1, keepdims=True)
    norm = np.clip(norm, a_min=1e-12, a_max=None)
    return mean_pooled / norm

def main():
    print(f"Loading ONNX quantized model from {ONNX_PATH}...")
    session = ort.InferenceSession(ONNX_PATH, providers=["CPUExecutionProvider"])
    tokenizer = AutoTokenizer.from_pretrained("sentence-transformers/all-MiniLM-L6-v2")

    passages = []
    with open(PASSAGES_PATH, "r", encoding="utf-8") as f:
        for line in f:
            if line.strip():
                passages.append(json.loads(line))

    print(f"Encoding {len(passages)} passages into 384-d normalized vectors...")
    passage_ids = []
    all_vectors = []

    batch_size = 32
    for i in tqdm(range(0, len(passages), batch_size), desc="Embedding passages"):
        batch = passages[i:i + batch_size]
        # Text format: Title: summary text
        texts = [f"{p['title']}: {p['text']}" for p in batch]
        enc = tokenizer(texts, max_length=256, padding=True, truncation=True, return_tensors="np")

        ort_inputs = {
            "input_ids": enc["input_ids"].astype(np.int64),
            "attention_mask": enc["attention_mask"].astype(np.int64),
            "token_type_ids": enc.get("token_type_ids", np.zeros_like(enc["input_ids"])).astype(np.int64)
        }

        ort_out = session.run(["last_hidden_state"], ort_inputs)[0]
        norm_vecs = mean_pool_and_normalize(ort_out, enc["attention_mask"])

        for p, vec in zip(batch, norm_vecs):
            passage_ids.append(p["passage_id"])
            all_vectors.append(vec.astype(np.float32))

    vectors_matrix = np.array(all_vectors, dtype=np.float32)
    num_vectors, dim = vectors_matrix.shape
    print(f"Encoded matrix shape: {num_vectors} x {dim}")

    # Write binary file with magic header
    # Magic: 'VEC1' (4 bytes) + num_vectors (uint32) + dim (uint32) + raw float32 array
    with open(BINARY_VECTORS_PATH, "wb") as f:
        f.write(b"VEC1")
        f.write(struct.pack("<II", num_vectors, dim))
        f.write(vectors_matrix.tobytes())

    file_size_mb = os.path.getsize(BINARY_VECTORS_PATH) / (1024 * 1024)
    print(f"Saved binary embeddings to {BINARY_VECTORS_PATH} ({file_size_mb:.2f} MB)")

    with open(PASSAGE_IDS_PATH, "w", encoding="utf-8") as f:
        json.dump(passage_ids, f, indent=2)
    print(f"Saved passage ID mapping to {PASSAGE_IDS_PATH}")

if __name__ == "__main__":
    main()


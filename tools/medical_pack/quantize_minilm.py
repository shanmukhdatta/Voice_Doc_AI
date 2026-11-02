#!/usr/bin/env python3
"""
Quantize FP32 ONNX MiniLM model to dynamic INT8 and generate golden test fixtures.
"""

import os
import sys
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8")
os.environ["PYTHONIOENCODING"] = "utf-8"
import json
import hashlib
import numpy as np
import onnxruntime as ort
from onnxruntime.quantization import quantize_dynamic, QuantType
from transformers import AutoTokenizer, AutoModel
import torch

MODEL_ID = "sentence-transformers/all-MiniLM-L6-v2"
ASSETS_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "model_assets", "minilm")
FIXTURES_DIR = os.path.join(os.path.dirname(__file__), "test_fixtures")

TEST_QUERIES = [
    "I have stomach discomfort",
    "stomach pain in upper abdomen",
    "Where do you feel the pain?",
    "upper right",
    "How long have you had this fever?",
    "For about 3 days, since Tuesday",
    "mild dry cough without shortness of breath",
    "I have had a mild sore throat when swallowing for two days",
    "seasonal allergy symptoms like runny nose and sneezing",
    "twisted ankle with minor swelling and pain",
    "first degree burn on hand from hot tea",
    "chronic stress and difficulty falling asleep",
    "what does 'stroke' mean in medical textbooks?",
    "hypothetically, what causes chest pain in biology?",
    "patient denies chest pain or breathing difficulty",
    "chest pain: none. mild headache for 2 days.",
    "my mother has similar fever symptoms",
    "child has a mild cold and runny nose",
    "severe sudden thunderclap headache",
    "crushing chest pain radiating to left arm",
    "minor cuts and scrapes first aid cleaning",
    "heat exhaustion after working in hot weather",
    "how to prevent dehydration and maintain electrolyte balance",
    "digital eye strain and screen fatigue 20-20-20 rule"
]

def mean_pool_and_normalize(token_embeddings, attention_mask):
    """
    token_embeddings: [batch_size, seq_len, hidden_dim]
    attention_mask: [batch_size, seq_len]
    """
    input_mask_expanded = np.expand_dims(attention_mask, -1).astype(np.float32)
    sum_embeddings = np.sum(token_embeddings * input_mask_expanded, axis=1)
    sum_mask = np.clip(input_mask_expanded.sum(axis=1), a_min=1e-9, a_max=None)
    mean_pooled = sum_embeddings / sum_mask
    norm = np.linalg.norm(mean_pooled, axis=1, keepdims=True)
    norm = np.clip(norm, a_min=1e-12, a_max=None)
    normalized = mean_pooled / norm
    return normalized

def main():
    os.makedirs(ASSETS_DIR, exist_ok=True)
    os.makedirs(FIXTURES_DIR, exist_ok=True)

    fp32_path = os.path.join(ASSETS_DIR, "model_fp32.onnx")
    int8_path = os.path.join(ASSETS_DIR, "model_quantized.onnx")

    if not os.path.exists(fp32_path):
        print(f"FP32 model not found at {fp32_path}. Running export first...")
        from export_minilm_onnx import main as export_main
        export_main()

    print(f"Quantizing {fp32_path} to {int8_path} (INT8 dynamic)...")
    quantize_dynamic(
        model_input=fp32_path,
        model_output=int8_path,
        weight_type=QuantType.QInt8
    )

    size_mb = os.path.getsize(int8_path) / (1024 * 1024)
    print(f"Quantized INT8 model size: {size_mb:.2f} MB")

    # Hash
    sha256 = hashlib.sha256()
    with open(int8_path, "rb") as f:
        while chunk := f.read(65536):
            sha256.update(chunk)
    hash_int8 = sha256.hexdigest()

    tokenizer = AutoTokenizer.from_pretrained(MODEL_ID)
    pt_model = AutoModel.from_pretrained(MODEL_ID)
    pt_model.eval()

    session = ort.InferenceSession(int8_path, providers=["CPUExecutionProvider"])

    fixtures = []
    print(f"Generating golden test fixtures for {len(TEST_QUERIES)} inputs...")

    for text in TEST_QUERIES:
        enc = tokenizer(text, max_length=256, truncation=True, padding=False)
        input_ids = enc["input_ids"]
        attention_mask = enc["attention_mask"]
        token_type_ids = enc.get("token_type_ids", [0] * len(input_ids))

        # PyTorch reference
        pt_inputs = {k: torch.tensor([v]) for k, v in enc.items()}
        with torch.no_grad():
            pt_out = pt_model(**pt_inputs)
            pt_emb = pt_out.last_hidden_state.numpy()
            pt_norm = mean_pool_and_normalize(pt_emb, np.array([attention_mask]))[0]

        # ONNX INT8 run
        ort_inputs = {
            "input_ids": np.array([input_ids], dtype=np.int64),
            "attention_mask": np.array([attention_mask], dtype=np.int64),
            "token_type_ids": np.array([token_type_ids], dtype=np.int64)
        }
        ort_out = session.run(["last_hidden_state"], ort_inputs)[0]
        onnx_norm = mean_pool_and_normalize(ort_out, np.array([attention_mask]))[0]

        # Calculate cosine similarity between FP32 PyTorch and INT8 ONNX
        cos_sim = float(np.dot(pt_norm, onnx_norm))
        max_diff = float(np.max(np.abs(pt_norm - onnx_norm)))

        fixtures.append({
            "text": text,
            "token_ids": input_ids,
            "attention_mask": attention_mask,
            "token_type_ids": token_type_ids,
            "golden_vector": onnx_norm.tolist(),
            "vector_dimension": len(onnx_norm),
            "pytorch_onnx_cosine_sim": cos_sim,
            "max_abs_diff": max_diff
        })

    avg_sim = np.mean([f["pytorch_onnx_cosine_sim"] for f in fixtures])
    print(f"Average cosine similarity between FP32 PyTorch and INT8 ONNX: {avg_sim:.5f}")

    fixtures_path = os.path.join(FIXTURES_DIR, "embedding_parity_fixtures.json")
    with open(fixtures_path, "w", encoding="utf-8") as f:
        json.dump({
            "model_id": MODEL_ID,
            "quantization": "INT8 dynamic",
            "model_sha256": hash_int8,
            "vector_dimension": 384,
            "count": len(fixtures),
            "avg_cosine_sim": avg_sim,
            "fixtures": fixtures
        }, f, indent=2)
    print(f"Saved golden test fixtures to {fixtures_path}")

    # Write quantization metadata
    meta = {
        "model_id": MODEL_ID,
        "onnx_int8_path": int8_path,
        "onnx_int8_size_bytes": os.path.getsize(int8_path),
        "onnx_int8_size_mb": round(size_mb, 2),
        "onnx_int8_sha256": hash_int8,
        "vector_dimension": 384,
        "pooling": "masked_mean_pooling",
        "normalization": "L2",
        "avg_int8_fp32_similarity": round(float(avg_sim), 5)
    }
    with open(os.path.join(ASSETS_DIR, "quantization_metadata.json"), "w", encoding="utf-8") as f:
        json.dump(meta, f, indent=2)
    print("Done quantizing MiniLM!")

if __name__ == "__main__":
    main()


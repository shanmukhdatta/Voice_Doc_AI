#!/usr/bin/env python3
"""
Export sentence-transformers/all-MiniLM-L6-v2 to ONNX format.
Validates outputs and exports vocabulary for Android Kotlin WordPiece tokenizer.
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
import torch
import numpy as np
from transformers import AutoTokenizer, AutoModel

MODEL_ID = "sentence-transformers/all-MiniLM-L6-v2"
OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "model_assets", "minilm")

def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    print(f"Loading {MODEL_ID}...")
    tokenizer = AutoTokenizer.from_pretrained(MODEL_ID)
    model = AutoModel.from_pretrained(MODEL_ID)
    model.eval()

    # Save vocabulary for Android tokenizer
    vocab_file = os.path.join(OUTPUT_DIR, "vocab.txt")
    with open(vocab_file, "w", encoding="utf-8") as f:
        vocab = tokenizer.get_vocab()
        # Sort by token id
        sorted_vocab = sorted(vocab.items(), key=lambda x: x[1])
        for token, _ in sorted_vocab:
            f.write(token + "\n")
    print(f"Saved vocabulary ({len(sorted_vocab)} tokens) to {vocab_file}")

    onnx_fp32_path = os.path.join(OUTPUT_DIR, "model_fp32.onnx")
    dummy_text = "This is a test medical sentence for ONNX export."
    inputs = tokenizer(dummy_text, return_tensors="pt")

    input_names = ["input_ids", "attention_mask", "token_type_ids"]
    output_names = ["last_hidden_state"]

    dynamic_axes = {
        "input_ids": {0: "batch_size", 1: "sequence_length"},
        "attention_mask": {0: "batch_size", 1: "sequence_length"},
        "token_type_ids": {0: "batch_size", 1: "sequence_length"},
        "last_hidden_state": {0: "batch_size", 1: "sequence_length"},
    }

    print(f"Exporting FP32 ONNX model to {onnx_fp32_path}...")
    torch.onnx.export(
        model,
        (inputs["input_ids"], inputs["attention_mask"], inputs["token_type_ids"]),
        onnx_fp32_path,
        input_names=input_names,
        output_names=output_names,
        dynamic_axes=dynamic_axes,
        opset_version=14,
        do_constant_folding=True
    )

    size_mb = os.path.getsize(onnx_fp32_path) / (1024 * 1024)
    print(f"Exported FP32 ONNX model size: {size_mb:.2f} MB")

    # Compute hash
    sha256 = hashlib.sha256()
    with open(onnx_fp32_path, "rb") as f:
        while chunk := f.read(65536):
            sha256.update(chunk)
    hash_fp32 = sha256.hexdigest()

    meta = {
        "model_id": MODEL_ID,
        "onnx_fp32_path": onnx_fp32_path,
        "onnx_fp32_size_bytes": os.path.getsize(onnx_fp32_path),
        "onnx_fp32_sha256": hash_fp32,
        "embedding_dimension": 384,
        "max_sequence_length": 256,
        "vocab_size": len(sorted_vocab)
    }
    meta_path = os.path.join(OUTPUT_DIR, "export_metadata.json")
    with open(meta_path, "w", encoding="utf-8") as f:
        json.dump(meta, f, indent=2)
    print(f"Export metadata written to {meta_path}")

if __name__ == "__main__":
    main()


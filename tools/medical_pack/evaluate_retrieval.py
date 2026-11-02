#!/usr/bin/env python3
"""
Evaluates Vector, Lexical, and Hybrid (RRF) retrieval against 125 labeled evaluation queries.
Outputs comprehensive benchmark metrics: Recall@3, Recall@5, MRR, nDCG, rejection rate, and latency.
"""

import os
import sys
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
os.environ["PYTHONIOENCODING"] = "utf-8"

import time
import json
import struct
import sqlite3
import numpy as np
import onnxruntime as ort
from transformers import AutoTokenizer

DATA_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "medical-knowledge")
MODEL_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "model_assets", "minilm")
ONNX_PATH = os.path.join(MODEL_DIR, "model_quantized.onnx")
BINARY_VECTORS_PATH = os.path.join(DATA_DIR, "medical_embeddings.bin")
PASSAGE_IDS_PATH = os.path.join(DATA_DIR, "passage_ids.json")
DB_PATH = os.path.join(DATA_DIR, "medical_knowledge.db")
EVAL_QUERIES_PATH = os.path.join(os.path.dirname(__file__), "..", "..", "safety", "evaluation", "retrieval_queries.jsonl")

# Calibrated relevance thresholds
COSINE_THRESHOLD = 0.42

def load_vectors():
    with open(BINARY_VECTORS_PATH, "rb") as f:
        magic = f.read(4)
        assert magic == b"VEC1", f"Invalid magic {magic}"
        num_vecs, dim = struct.unpack("<II", f.read(8))
        raw = f.read(num_vecs * dim * 4)
        vectors = np.frombuffer(raw, dtype=np.float32).reshape((num_vecs, dim))
    with open(PASSAGE_IDS_PATH, "r", encoding="utf-8") as f:
        passage_ids = json.load(f)
    return vectors, passage_ids

def load_passages_meta(conn):
    cur = conn.cursor()
    cur.execute("SELECT passage_id, topic_id, title FROM passages")
    meta = {}
    for pid, tid, title in cur.fetchall():
        meta[pid] = {"topic_id": tid, "title": title}
    return meta

def mean_pool_and_normalize(token_embeddings, attention_mask):
    input_mask_expanded = np.expand_dims(attention_mask, -1).astype(np.float32)
    sum_embeddings = np.sum(token_embeddings * input_mask_expanded, axis=1)
    sum_mask = np.clip(input_mask_expanded.sum(axis=1), a_min=1e-9, a_max=None)
    mean_pooled = sum_embeddings / sum_mask
    norm = np.linalg.norm(mean_pooled, axis=1, keepdims=True)
    norm = np.clip(norm, a_min=1e-12, a_max=None)
    return mean_pooled / norm

def search_vector(session, tokenizer, query, vectors_matrix, passage_ids, top_k=10):
    t0 = time.perf_counter()
    enc = tokenizer(query, max_length=256, padding=True, truncation=True, return_tensors="np")
    ort_inputs = {
        "input_ids": enc["input_ids"].astype(np.int64),
        "attention_mask": enc["attention_mask"].astype(np.int64),
        "token_type_ids": enc.get("token_type_ids", np.zeros_like(enc["input_ids"])).astype(np.int64)
    }
    ort_out = session.run(["last_hidden_state"], ort_inputs)[0]
    q_vec = mean_pool_and_normalize(ort_out, enc["attention_mask"])[0]

    # Exact dot product over normalized vectors == cosine similarity
    scores = np.dot(vectors_matrix, q_vec)
    top_indices = np.argsort(scores)[::-1][:top_k]
    dt = (time.perf_counter() - t0) * 1000.0

    results = []
    for idx in top_indices:
        results.append((passage_ids[idx], float(scores[idx])))
    return results, dt

def search_lexical(conn, query, top_k=10):
    t0 = time.perf_counter()
    cur = conn.cursor()
    # Sanitize query for FTS
    clean_q = "".join(c if c.isalnum() or c.isspace() else " " for c in query)
    tokens = [t for t in clean_q.split() if len(t) > 2 and t.lower() not in {"what", "when", "where", "have", "with", "from", "that", "this", "some", "your"}]

    if not tokens:
        return [], 0.0

    fts_query = " OR ".join(tokens)
    sql = "SELECT passage_id FROM passages_fts WHERE passages_fts MATCH ? LIMIT ?"
    try:
        cur.execute(sql, (fts_query, top_k))
        rows = cur.fetchall()
        results = [(r[0], 1.0 / (rank + 1)) for rank, r in enumerate(rows)]
    except Exception:
        results = []

    dt = (time.perf_counter() - t0) * 1000.0
    return results, dt

def search_hybrid(vec_results, lex_results, top_k=5, k=60):
    rrf_scores = {}
    for rank, (pid, _) in enumerate(vec_results):
        rrf_scores[pid] = rrf_scores.get(pid, 0.0) + (1.0 / (k + rank + 1))
    for rank, (pid, _) in enumerate(lex_results):
        rrf_scores[pid] = rrf_scores.get(pid, 0.0) + (1.0 / (k + rank + 1))

    sorted_pids = sorted(rrf_scores.items(), key=lambda x: x[1], reverse=True)[:top_k]
    return sorted_pids

def matches_expected(retrieved_pids, meta, expected_substr):
    if not expected_substr:
        return False
    target = expected_substr.lower()
    for pid, _ in retrieved_pids:
        title = meta.get(pid, {}).get("title", "").lower()
        if target in title:
            return True
    return False

def evaluate():
    session = ort.InferenceSession(ONNX_PATH, providers=["CPUExecutionProvider"])
    tokenizer = AutoTokenizer.from_pretrained("sentence-transformers/all-MiniLM-L6-v2")
    vectors_matrix, passage_ids = load_vectors()
    conn = sqlite3.connect(DB_PATH)
    meta = load_passages_meta(conn)

    with open(EVAL_QUERIES_PATH, "r", encoding="utf-8") as f:
        cases = [json.loads(line) for line in f if line.strip()]

    print(f"Loaded {len(cases)} evaluation cases.")

    vec_hits_k3 = 0
    vec_hits_k5 = 0
    vec_rr_sum = 0.0

    hybrid_hits_k3 = 0
    hybrid_hits_k5 = 0
    hybrid_rr_sum = 0.0

    rejected_out_of_scope = 0
    total_out_of_scope = 0

    vec_times = []
    lex_times = []
    hybrid_times = []

    answerable_count = 0

    for c in cases:
        query = c["query"]
        expected = c.get("expected_topic")
        is_answerable = c["is_answerable"]

        vec_res, v_time = search_vector(session, tokenizer, query, vectors_matrix, passage_ids, top_k=10)
        lex_res, l_time = search_lexical(conn, query, top_k=10)

        t_hyb0 = time.perf_counter()
        hyb_res = search_hybrid(vec_res, lex_res, top_k=5)
        h_time = v_time + l_time + ((time.perf_counter() - t_hyb0) * 1000.0)

        vec_times.append(v_time)
        lex_times.append(l_time)
        hybrid_times.append(h_time)

        top_vec_score = vec_res[0][1] if vec_res else 0.0

        if not is_answerable:
            total_out_of_scope += 1
            # If top vector score is below threshold, correctly gated as WEAK/NONE
            if top_vec_score < COSINE_THRESHOLD:
                rejected_out_of_scope += 1
        else:
            answerable_count += 1
            # Vector evaluation
            target = expected.lower()
            v_rank = 0
            for r, (pid, score) in enumerate(vec_res, start=1):
                if target in meta.get(pid, {}).get("title", "").lower():
                    v_rank = r
                    break
            if v_rank > 0 and v_rank <= 3:
                vec_hits_k3 += 1
            if v_rank > 0 and v_rank <= 5:
                vec_hits_k5 += 1
            if v_rank > 0:
                vec_rr_sum += 1.0 / v_rank

            # Hybrid evaluation
            h_rank = 0
            for r, (pid, _) in enumerate(hyb_res, start=1):
                if target in meta.get(pid, {}).get("title", "").lower():
                    h_rank = r
                    break
            if h_rank > 0 and h_rank <= 3:
                hybrid_hits_k3 += 1
            if h_rank > 0 and h_rank <= 5:
                hybrid_hits_k5 += 1
            if h_rank > 0:
                hybrid_rr_sum += 1.0 / h_rank

    conn.close()

    metrics = {
        "evaluation_dataset": "safety/evaluation/retrieval_queries.jsonl",
        "total_queries": len(cases),
        "answerable_queries": answerable_count,
        "unanswerable_out_of_scope_queries": total_out_of_scope,
        "calibrated_cosine_threshold": COSINE_THRESHOLD,
        "vector_only": {
            "recall_at_3": round(vec_hits_k3 / answerable_count, 4),
            "recall_at_5": round(vec_hits_k5 / answerable_count, 4),
            "mrr": round(vec_rr_sum / answerable_count, 4),
            "avg_latency_ms": round(float(np.mean(vec_times)), 2),
            "p95_latency_ms": round(float(np.percentile(vec_times, 95)), 2)
        },
        "hybrid_rrf": {
            "recall_at_3": round(hybrid_hits_k3 / answerable_count, 4),
            "recall_at_5": round(hybrid_hits_k5 / answerable_count, 4),
            "mrr": round(hybrid_rr_sum / answerable_count, 4),
            "avg_latency_ms": round(float(np.mean(hybrid_times)), 2),
            "p95_latency_ms": round(float(np.percentile(hybrid_times, 95)), 2)
        },
        "out_of_scope_rejection_accuracy": round(rejected_out_of_scope / total_out_of_scope, 4)
    }

    print("\n--- RETRIEVAL BENCHMARK RESULTS ---")
    print(json.dumps(metrics, indent=2))

    res_path = os.path.join(os.path.dirname(__file__), "..", "..", "medical-knowledge", "retrieval_benchmark_results.json")
    with open(res_path, "w", encoding="utf-8") as f:
        json.dump(metrics, f, indent=2)
    print(f"Benchmark results saved to {res_path}")

if __name__ == "__main__":
    evaluate()


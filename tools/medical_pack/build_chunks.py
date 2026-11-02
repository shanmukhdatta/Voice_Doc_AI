#!/usr/bin/env python3
"""
Splits parsed MedlinePlus topics into section-aware, length-bounded passages (<=256 wordpieces).
Assigns deterministic passage IDs and metadata.
"""

import os
import json
import re
from transformers import AutoTokenizer

DATA_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "medical-knowledge")
INPUT_JSONL = os.path.join(DATA_DIR, "parsed_topics.jsonl")
OUTPUT_PASSAGES = os.path.join(DATA_DIR, "passages.jsonl")
MODEL_ID = "sentence-transformers/all-MiniLM-L6-v2"

def split_into_chunks(text, max_words=180):
    paragraphs = text.split("\n\n")
    chunks = []
    current_chunk = []
    current_words = 0

    for p in paragraphs:
        p_clean = p.strip()
        if not p_clean:
            continue
        words = p_clean.split()
        if current_words + len(words) <= max_words:
            current_chunk.append(p_clean)
            current_words += len(words)
        else:
            if current_chunk:
                chunks.append("\n\n".join(current_chunk))
            current_chunk = [p_clean]
            current_words = len(words)

    if current_chunk:
        chunks.append("\n\n".join(current_chunk))

    return chunks

def main():
    print(f"Loading tokenizer {MODEL_ID} to verify token bounds...")
    tokenizer = AutoTokenizer.from_pretrained(MODEL_ID)

    topics = []
    with open(INPUT_JSONL, "r", encoding="utf-8") as f:
        for line in f:
            if line.strip():
                topics.append(json.loads(line))

    print(f"Processing {len(topics)} topics into passages...")
    passages = []

    for t in topics:
        topic_id = t["topic_id"]
        title = t["title"]
        url = t["url"]
        summary = t["summary"]
        synonyms = t.get("synonyms", [])
        groups = t.get("groups", [])

        raw_chunks = split_into_chunks(summary, max_words=160)
        for idx, chunk_text in enumerate(raw_chunks, start=1):
            passage_id = f"MP_{topic_id}_{idx:02d}"

            # Verify wordpiece token length
            tokens = tokenizer.encode(chunk_text, truncation=False)
            if len(tokens) > 256:
                # Truncate cleanly by sentences
                sentences = re.split(r'(?<=[.!?])\s+', chunk_text)
                sub_chunk = []
                for s in sentences:
                    test_text = " ".join(sub_chunk + [s])
                    if len(tokenizer.encode(test_text, truncation=False)) <= 250:
                        sub_chunk.append(s)
                    else:
                        break
                chunk_text = " ".join(sub_chunk) if sub_chunk else chunk_text[:500]

            passages.append({
                "passage_id": passage_id,
                "topic_id": topic_id,
                "title": title,
                "url": url,
                "text": chunk_text,
                "synonyms": synonyms,
                "groups": groups,
                "publisher": "MedlinePlus (National Library of Medicine)",
                "license": "Public Domain (17 U.S.C. § 105)",
                "review_status": "source_verified_clinical_review_pending"
            })

    print(f"Generated {len(passages)} passages from {len(topics)} topics.")

    with open(OUTPUT_PASSAGES, "w", encoding="utf-8") as f:
        for p in passages:
            f.write(json.dumps(p, ensure_ascii=False) + "\n")

    print(f"Saved passages to {OUTPUT_PASSAGES}")

if __name__ == "__main__":
    main()


#!/usr/bin/env python3
"""
Builds an offline SQLite database with full-text search (FTS4) for medical passages.
"""

import os
import sys
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
os.environ["PYTHONIOENCODING"] = "utf-8"

import json
import sqlite3

DATA_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "medical-knowledge")
PASSAGES_PATH = os.path.join(DATA_DIR, "passages.jsonl")
DB_PATH = os.path.join(DATA_DIR, "medical_knowledge.db")

def main():
    if os.path.exists(DB_PATH):
        os.remove(DB_PATH)

    print(f"Creating SQLite knowledge database at {DB_PATH}...")
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()

    cur.execute("""
        CREATE TABLE passages (
            passage_id TEXT PRIMARY KEY,
            topic_id TEXT NOT NULL,
            title TEXT NOT NULL,
            url TEXT NOT NULL,
            text TEXT NOT NULL,
            license TEXT NOT NULL,
            review_status TEXT NOT NULL
        )
    """)

    cur.execute("""
        CREATE VIRTUAL TABLE passages_fts USING fts4(
            passage_id,
            title,
            text,
            synonyms
        )
    """)

    passages = []
    with open(PASSAGES_PATH, "r", encoding="utf-8") as f:
        for line in f:
            if line.strip():
                passages.append(json.loads(line))

    print(f"Inserting {len(passages)} passages into SQLite and FTS tables...")
    for p in passages:
        cur.execute(
            "INSERT INTO passages (passage_id, topic_id, title, url, text, license, review_status) VALUES (?, ?, ?, ?, ?, ?, ?)",
            (p["passage_id"], p["topic_id"], p["title"], p["url"], p["text"], p["license"], p["review_status"])
        )
        syn_str = ", ".join(p.get("synonyms", []))
        cur.execute(
            "INSERT INTO passages_fts (passage_id, title, text, synonyms) VALUES (?, ?, ?, ?)",
            (p["passage_id"], p["title"], p["text"], syn_str)
        )

    conn.commit()
    conn.close()

    db_size_mb = os.path.getsize(DB_PATH) / (1024 * 1024)
    print(f"Successfully built SQLite medical knowledge database: {db_size_mb:.2f} MB")

if __name__ == "__main__":
    main()


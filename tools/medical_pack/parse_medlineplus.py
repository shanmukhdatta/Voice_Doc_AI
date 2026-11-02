#!/usr/bin/env python3
"""
Parses official MedlinePlus XML and outputs structured JSONL of topics.
"""

import os
import re
import html
import json
import xml.etree.ElementTree as ET

DATA_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "medical-knowledge")
RAW_XML = os.path.join(DATA_DIR, "raw_source", "mplus_topics_2026-10-31.xml")
OUTPUT_JSONL = os.path.join(DATA_DIR, "parsed_topics.jsonl")

def clean_html(text):
    if not text:
        return ""
    # Unescape HTML entities
    text = html.unescape(text)
    # Replace list items with clean bullet text
    text = re.sub(r'<li>\s*', '• ', text)
    text = re.sub(r'</li>', '\n', text)
    # Replace paragraph endings and breaks with newlines
    text = re.sub(r'</p>', '\n\n', text)
    text = re.sub(r'<br\s*/?>', '\n', text)
    # Strip any remaining tags
    text = re.sub(r'<[^>]+>', '', text)
    # Collapse multiple whitespaces/newlines
    lines = [re.sub(r'\s+', ' ', line).strip() for line in text.split('\n')]
    cleaned = '\n'.join(line for line in lines if line)
    return cleaned.strip()

def parse_topics(xml_path):
    topics = []
    print(f"Parsing {xml_path}...")
    for event, elem in ET.iterparse(xml_path, events=('end',)):
        if elem.tag == 'health-topic':
            if elem.get('language') == 'English':
                topic_id = elem.get('id', '')
                title = elem.get('title', '').strip()
                url = elem.get('url', '').strip()
                date_created = elem.get('date-created', '')

                summary_elem = elem.find('full-summary')
                raw_summary = summary_elem.text if summary_elem is not None else ""
                clean_summary = clean_html(raw_summary)

                if clean_summary:
                    also_called = [c.text.strip() for c in elem.findall('also-called') if c.text]
                    see_ref = [c.text.strip() for c in elem.findall('see-reference') if c.text]
                    groups = [c.text.strip() for c in elem.findall('group') if c.text]
                    related = [c.text.strip() for c in elem.findall('related-topic') if c.text]

                    synonyms = list(dict.fromkeys(also_called + see_ref))

                    topics.append({
                        "topic_id": topic_id,
                        "title": title,
                        "url": url,
                        "date_created": date_created,
                        "summary": clean_summary,
                        "synonyms": synonyms,
                        "groups": groups,
                        "related_topics": related
                    })

            elem.clear()

    print(f"Parsed {len(topics)} English health topics with full summaries.")
    return topics

def main():
    os.makedirs(DATA_DIR, exist_ok=True)
    topics = parse_topics(RAW_XML)
    with open(OUTPUT_JSONL, "w", encoding="utf-8") as f:
        for t in topics:
            f.write(json.dumps(t, ensure_ascii=False) + "\n")
    print(f"Saved parsed topics to {OUTPUT_JSONL}")

if __name__ == "__main__":
    main()


# Voice Doctor AI — Medical Safety Policy (Phase 4)

**Document Version:** 2026.4.1  
**Target Environment:** On-Device Android Health Assistant (Offline-first)  
**Applicable Scope:** English conversational pipeline (Telugu remains labeled demonstration mode).

---

## 1. Ethical Principles & Non-Clinical Disclaimer
1. **Not a Clinician:** Voice Doctor AI is an on-device health-information assistant. It is strictly not a licensed physician, clinical diagnostic tool, or prescribing entity.
2. **First Do No Harm:** When any user symptom indicates a possible medical emergency or acute life safety concern, conversational generation is bypassed immediately in favor of emergency escalation instructions.
3. **Evidence Grounding:** All general health guidance must be strictly grounded in vetted, public-domain health literature (e.g., U.S. National Library of Medicine / MedlinePlus). Hallucination of medical facts or citation of unauthorized sources is strictly prohibited.
4. **Zero Cloud Egress:** All speech processing, inference, knowledge retrieval, and database storage occur 100% locally on the user's Android device. No medical queries or chat transcripts leave the device.

---

## 2. Emergency Red Flags & Deterministic Triage

The application implements deterministic pattern matching that executes **prior to** model inference. Any query matching an emergency red flag immediately returns standard escalation text and emergency contact information (e.g., 911 / 112) without invoking the LLM:

| Category | Trigger Indicators | Escalation Action |
| :--- | :--- | :--- |
| **Cardiac Emergency** | Crushing chest pain, pressure, left arm / jaw pain, cold sweat | Immediate 911 alert; chew aspirin if advised by emergency dispatcher. |
| **Cerebrovascular (FAST)** | Face drooping, arm weakness, slurred speech, sudden vision loss | Immediate 911 alert; note time symptoms started. |
| **Acute Respiratory** | Gasping, severe shortness of breath, blue lips/fingertips | Immediate 911 alert; sit upright, seek immediate emergency care. |
| **Anaphylaxis** | Swollen tongue/throat, hives, severe allergic reaction | Use epinephrine auto-injector (EpiPen) if available and call 911. |
| **Crisis / Self-Harm** | Suicide, self-harm, ending life, overwhelming despair | Call/text 988 (Suicide & Crisis Lifeline) immediately. |

---

## 3. Prescriptions and Diagnostic Boundaries

1. **No Prescriptions:** The assistant will never prescribe, recommend specific dosages (e.g., "take 500mg"), or adjust medication regimens. Inquiries regarding dosages trigger explicit referral to a doctor or licensed pharmacist.
2. **No Clinical Diagnoses:** The assistant will never state definitive diagnostic assertions (e.g., "You have pneumonia"). It uses conditional, informative framing: "Symptoms such as runny nose and sneezing are commonly associated with the common cold..."
3. **Ambiguity & Clarification:** Unclear, single-word, or ambiguous inputs prompt the user for clarification before providing medical guidance.

---

## 4. Post-Generation Output Validation

Before any model-generated response is rendered on screen or spoken via Text-to-Speech:
1. Buffering: Tokens are accumulated in memory; unvalidated tokens are never streamed to user UI or TTS.
2. CoT Stripping: `<think>...</think>` tags and reasoning artifacts are stripped.
3. Citation Verification: Any `[SOURCE_ID]` tag in the response is validated against the retrieved topic list. Hallucinated tags are stripped.
4. Safety Heuristics: Regex checks scan for prescriptive dosage or diagnostic assertions. Unsafe outputs are replaced with safe fallback text.


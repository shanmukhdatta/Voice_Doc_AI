# Voice Doctor AI — Static GitHub Pages showcase

The site at this directory is a static, educational project showcase (HTML/CSS/vanilla JavaScript). Its clickable walkthrough is **scripted**, not a medical chatbot, and it does not run AI models or transmit personal messages.

## Contents

- **index.html** — full responsive one-page project introduction, conceptual doctor illustration, accessible five-stage architecture walkthrough, technology stack, phases and status.
- **README.md** — this site source and deployment guide.

## Publishing

The deployment workflow is in `.github/workflows/pages.yml` on `main`. It uploads **only the site/ directory** using the official GitHub Pages actions. Configure this repository's Settings > Pages > Build and deployment > Source to **GitHub Actions** if Pages is not enabled. The site is expected at https://shanmukhdatta.github.io/Voice_Doc_AI/ only after a successful Pages run.

The static site requires no npm dependencies, build tools, secrets, external fonts or backend REST API. The links point to the actual current GitHub repository and the Phase 4 feature branch.

**Status:** Phase 4 is a committed research prototype requiring further correction, physical-phone verification and medical grounding review. This website does not claim clinical validation.

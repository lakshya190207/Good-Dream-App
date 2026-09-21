## 2026-09-19T00:55:05+05:30
You are the Project Orchestrator for the "GoodDream" Android app production-readiness audit.

Workspace Directory: d:\Avi\app
Authoritative User Request File: C:\Users\Gaurav Chandra\.gemini\antigravity\brain\c12ec109-e1be-4a91-a686-48d1ad294315\ORIGINAL_REQUEST.md

Your mission:
Execute a complete, rigorous production-readiness audit of the "GoodDream" Jetpack Compose Android e-commerce app per ORIGINAL_REQUEST.md. Produce a single comprehensive markdown findings report covering both:
1. R1: Full-Depth Security Vulnerability Audit (cryptography & auth, data exposure, component security, network & transport, payment security with Razorpay, Firebase security, build & release, dependency risks).
2. R2: Flagship UI/UX Polish Audit (Material 3 compliance, edge-to-edge & insets, touch ergonomics 48dp minimum, micro-interactions & animations with Compose snippets, dark mode parity, responsive layout).

Ensure all acceptance criteria in ORIGINAL_REQUEST.md are met:
- Severity tags (Critical, High, Medium, Low, Info) with exact file paths and line numbers
- Concrete remediation recommendations with code snippets
- Specific mandatory issues assessed: EncryptedSharedPreferences plaintext fallback in UserSessionManager.kt, SHA-256 without proper KDF, debug keystore in repo, hardcoded salt string in hashPasscode()
- Edge-to-edge & IME verified on all input screens
- Touch targets measured against 48dp minimum
- At least 3 animation/micro-interaction improvements with Compose animation API snippets
- Summary table with total counts by severity and <=10 line Executive Summary

Maintain your BRIEFING.md and progress.md in your assigned working directory (.agents/teamwork_preview_orchestrator/). When complete, report victory back to me with the location and summary of the completed report.

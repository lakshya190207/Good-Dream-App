# Project: GoodDream Production-Readiness Audit

## Architecture
- Dual-track investigation: R1 Full-Depth Security Vulnerability Audit & R2 Flagship UI/UX Polish Audit
- Synthesis of single comprehensive findings report meeting all acceptance criteria
- Independent verification and review

## Feature Inventory
| # | Feature / Scope | Description | Milestone | Source |
|---|-----------------|-------------|-----------|--------|
| 1 | Codebase Survey & Mapping | Complete file inventory, architecture, integration points | M0 | ORIGINAL_REQUEST.md |
| 2 | Cryptography & Authentication Audit | SHA-256, salt, EncryptedSharedPreferences plaintext fallback, sessions, admin | M1 (R1) | ORIGINAL_REQUEST §R1 |
| 3 | Data Exposure & Secrets Audit | Keystores, .env, BuildConfig, Gemini & Razorpay keys, logs | M1 (R1) | ORIGINAL_REQUEST §R1 |
| 4 | Component & Intent Security | AndroidManifest.xml, MainActivity.kt, deep links | M1 (R1) | ORIGINAL_REQUEST §R1 |
| 5 | Network, Transport & Payment Security | network_security_config.xml, cert pinning, OkHttp/Retrofit, Razorpay | M1 (R1) | ORIGINAL_REQUEST §R1 |
| 6 | Firebase, Build & Release, Dependencies | firestore.rules, App Check, FCM, R8/ProGuard, dependencies | M1 (R1) | ORIGINAL_REQUEST §R1 |
| 7 | Material 3 Compliance & Theming | Color roles, typography, shapes, tonal elevations | M2 (R2) | ORIGINAL_REQUEST §R2 |
| 8 | Edge-to-Edge & IME Insets | enableEdgeToEdge(), WindowInsets, imePadding() on all form screens | M2 (R2) | ORIGINAL_REQUEST §R2 |
| 9 | Touch Ergonomics & Targets | Measure all clickable surfaces against 48dp minimum | M2 (R2) | ORIGINAL_REQUEST §R2 |
| 10 | Micro-interactions & Animations | Compose animation API snippets (at least 3), shimmer, transitions | M2 (R2) | ORIGINAL_REQUEST §R2 |
| 11 | Dark Mode Parity & Responsive Layout | WCAG AA contrast (4.5:1), responsive layout, Coil image loading | M2 (R2) | ORIGINAL_REQUEST §R2 |
| 12 | Comprehensive Report Synthesis | Single markdown document with summary table & <=10 line Executive Summary | M3 | ORIGINAL_REQUEST §Acceptance |
| 13 | Forensic Audit & Review Verification | Review and verify all findings, ensure zero false positives | M4 | AGENTS.md & Identity |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 0 | Codebase Survey | Map all ~50 files, configurations, integrations | none | IN_PROGRESS |
| 1 | R1 Security Audit | Full-depth security vulnerability audit | M0 | IN_PROGRESS |
| 2 | R2 UI/UX Polish Audit | Flagship UI/UX polish and accessibility audit | M0 | IN_PROGRESS |
| 3 | Report Synthesis | Synthesize single unified markdown findings report | M1, M2 | PLANNED |
| 4 | Verification & Audit | Reviewer & Forensic Auditor validation | M3 | PLANNED |

## Code Layout
- Target Report: `d:\Avi\app\PRODUCTION_READINESS_AUDIT_REPORT.md`
- Metadata: `d:\Avi\app\.agents/`

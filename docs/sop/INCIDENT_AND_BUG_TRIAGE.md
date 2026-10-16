# SOP: Incident & Bug Triage

This Standard Operating Procedure (SOP) outlines the categorization, triage, and escalation procedure for defects, crashes, and data-integrity incidents in DockDrop.

---

## 1. Severity Classifications

| Severity | Description | Target Response | Target Resolution |
|---|---|---|---|
| **P0 — Critical** | Source data corruption or unintended modification; security/cryptographic vulnerability; unrecoverable SSD corruption | Immediate (< 4 hrs) | 24 hours |
| **P1 — High** | App crash preventing Drop or Restore; false "Sealed" receipt reported; cable disconnect causes crash loop | < 24 hours | 3 business days |
| **P2 — Medium** | Partial transfer failure on specific file types; notification sync issue; UI rendering glitch at 200% font scale | < 48 hours | Next planned release |
| **P3 — Low** | Minor visual artifact, typographical error in documentation, non-blocking edge case | < 7 days | Backlog |

---

## 2. Bug Triage Workflow

1. **Verify Non-Negotiables**:
   - If a report indicates source files were altered or deleted, treat immediately as **P0**. Immediately stop release distribution if necessary.
2. **Reproduce on Target Hardware**:
   - Attempt reproduction on the reference device tier (Samsung Galaxy A14 / Android 15 with exFAT drive).
   - Check if the defect reproduces across different USB storage devices or cable configurations.
3. **Isolate Root Cause**:
   - Classify into the relevant work domain (A1–A8).
   - Verify whether the behavior is a platform constraint (e.g. Android 15 `dataSync` service timeout) or an application defect.
4. **Fix & Regression Testing**:
   - Develop a unit or integration test reproducing the exact failure mode.
   - Verify fix on physical hardware across 3 unplug/replug cycles.
5. **Post-Mortem**:
   - For all P0 or P1 incidents, publish an incident post-mortem documenting root cause, impact, and preventive automated tests added to CI.

# DockDrop Release Checklist & Closed-Test Report

- **Date:** October 16, 2026
- **Target Release:** Version 1.0 (Build `1`)
- **Primary Reference Device:** Samsung Galaxy A14 5G (`SM-A146B`)
- **Android OS Build:** Android 15 / One UI 7.0 (`AP3A.240905.015.A2.A146BXXSFDZI2`)
- **Kernel / Security Patch:** 5.10.209-android14 / Security Patch September 1, 2026

---

## 1. Quality & Release Agent (A8) Completion Criteria

| # | Requirement | Status | Evidence / Observation |
|---|---|---|---|
| **1** | CI fails if `INTERNET` or `ACCESS_NETWORK_STATE` appears in merged manifest | **PASS** | Enforced by Gradle task `verifyZeroNetworkPermissions` registered under `check`. Verified against both debug and release merged manifests. |
| **2** | A written pass of the test matrix exists per phase, with device model and build recorded | **PASS** | Phases 0–4 documented in `docs/spikes/PHASE0_RESULTS.md`, `CLAUDE.md`, and test suites. Phase 5 device verification documented below. |
| **3** | Play pre-launch checklist complete | **PASS** | Title (30/30 chars), short desc (75/80 chars), Data Safety ("No data collected"), FGS (`dataSync`), Privacy Policy complete. |
| **4** | TalkBack walk-through completes first-run flow and a restore; font scale 200% breaks nothing | **PASS** | Accessible confirmation dialog for `HoldToRestoreButton`, polite 10% progress throttling, scrollable reflow on all screens. |
| **5** | Reduced-motion mode produces a fully usable static equivalent of every animation | **PASS** | Respects both system `animator_duration_scale == 0` and in-app `Reduce motion` toggle across `DockHero`, `DropParticles`, `HoldToRestoreButton`, and receipts. |

---

## 2. Test Matrix Pass by Phase on Reference Device (`SM-A146B`)

| Phase | Test Scope | Verification Method | Result | Notes |
|---|---|---|---|---|
| **Phase 0** | USB attach, SAF tree grant, permission persistence, FGS types | Spike Lab tests `T1`–`T8`, `F1`–`F4` | **PASS** | Documented in `PHASE0_RESULTS.md`. `dataSync` chosen in ADR-003. |
| **Phase 1** | Streaming copy, SHA-256 on the fly, destination re-read, journal resume | Cable-pull drill at 10%, 50%, 90% | **PASS** | Resumes byte-identical; zero `.part` artifacts remain. |
| **Phase 2** | Full UI, DockHero glyphs, particle stream, S01–S11 screens | Live device runs + Compose previews | **PASS** | P90 frame time $\le 16.6\text{ ms}$; zero canvas heap allocations. |
| **Phase 3** | Restore engine, conflicts, new-phone simulation, 10-file drill | `RestoreEngineTest` + wiped folder restore | **PASS** | 100% hash match; timestamps preserved where allowed. |
| **Phase 4** | Deletion policies (Keep, N days, Mirror + guard), AES-256-GCM DDC1 format | `EncryptedDropAndRestoreTest` + 45 unit tests | **PASS** | Mirror guard trips at >20% / >500 files; fast passphrase failure. |
| **Phase 5** | Play readiness, S01 onboarding, S20 Settings, S21 History, TalkBack, 200% font scale | Play assets, a11y script, clean install | **PASS** | Shippable release build verified. |

---

## 3. Accessibility & Motion Polish Report (A7)

### 3.1 TalkBack Verification
- **First-run flow (S01 Welcome → S02 Keep Alive → S03 Home):**
  - All 3 pages of S01 announce distinct page titles and descriptions. "How to check" opens the bottom sheet with clear step-by-step instructions.
  - S02 Step cards announce step index, title, description, and state (`"Completed"` / `"Action required"`).
  - S03 DockHero announces coherent composite status (e.g. `"SSD status: Samsung T7 connected, 812 GB free"`).
- **Restore flow (S12 Restore Hub → HoldToRestoreButton):**
  - TalkBack accessibility click triggers an accessible confirmation `AlertDialog` ("Restore backup? Are you sure you want to restore your files to their folders? [Restore] [Cancel]").
  - Eliminates the barrier of requiring an inaccessible 1.2-second physical pointer hold gesture.
- **Progress announcement throttling (S09 Drop & S15 Restore):**
  - Live region announcements are throttled to 10% progress increments (`0%`, `10%`, `20%`, ..., `100%`) to avoid flooding the user's speech buffer.

### 3.2 200% Font Scale Reflow Verification
- Tested via `adb shell settings put system font_scale 2.0`:
  - `S01` Welcome: Pager text expands and wraps cleanly; CTA buttons remain accessible.
  - `S02` Keep Alive: Content column scrolls vertically (`verticalScroll`); button labels wrap without clipping.
  - `S03` Home: Set cards reflow status metadata onto multiple lines cleanly.
  - `S09` Drop: Folder pills wrap into horizontal scroll; monospace rate/speed display wraps without truncation.
  - `S20` Settings: Preference switches and descriptive text stack cleanly without overflow.

### 3.3 Reduced Motion Mode
- Tested with `Settings.Global.ANIMATOR_DURATION_SCALE == 0` and in-app `Reduce motion` toggle:
  - **DockHero:** Infinite breathing pulse and scan-line rotations disabled; Sealed checkmark appears at 100% immediately; lateral shake on Attention omitted.
  - **DropParticles:** Particle spawning disabled. Displays calm tabular monospace counter (`"18.4 files/s"`).
  - **HoldToRestoreButton:** Scale bounce disabled; fill increases deterministically without burst.
  - **Receipt Cards:** Stamp scale bounce omitted (`scale = 1.0f`).

---

## 4. Performance & Memory Profile on Galaxy A14 5G

```text
Target: P90 frame time <= 16.6 ms (60 fps), Janky frames < 2%
Measured via dumpsys gfxinfo in.soumyaranjansahoo.dockdrop:
-------------------------------------------------------------------------------
Total frames rendered: 2,840
Janky frames: 18 (0.63%)
50th percentile: 6.2 ms
90th percentile: 11.4 ms
95th percentile: 13.8 ms
99th percentile: 15.9 ms
Number Missed Vsync: 0
Number High Input Latency: 2
Number Slow UI Thread: 6
Number Slow Bitmap Upload: 0
Number Slow Issue Draw Commands: 8
-------------------------------------------------------------------------------
Verdict: PASS (11.4 ms P90 is well within the 16.6 ms budget).
```

---

## 5. Play Pre-Launch Compliance Checklist

- [x] No `android.permission.INTERNET` in merged manifest
- [x] No `android.permission.ACCESS_NETWORK_STATE` in merged manifest
- [x] No `android.permission.MANAGE_EXTERNAL_STORAGE`
- [x] All file operations use SAF (`ACTION_OPEN_DOCUMENT_TREE`)
- [x] Foreground service declared with `dataSync` type only
- [x] Foreground service starts only from direct user tap
- [x] Target SDK set to 35 (Android 15)
- [x] Title is exactly 30 characters (`DockDrop: SSD Backup & Restore`)
- [x] Short description is under 80 characters (75 characters)
- [x] Full description, release notes, and store assets checklist documented
- [x] Privacy policy written and verified (offline, zero data collected)
- [x] Data safety questionnaire answers documented (no data collected, no data shared)

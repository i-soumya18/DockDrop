# AGENTS.md — DockDrop Work Domains

> Eight narrow-scope agents (or role divisions). Each owns one slice, and each has a testable completion bar.
> Rules referenced as "Rule N" live in CLAUDE.md section 4.

## Overview

| # | Agent | Owns | Active from |
|---|---|---|---|
| A1 | Storage | SAF access, volume identity, permissions | Phase 0 |
| A2 | Trigger & Service | USB attach, foreground service, notifications | Phase 0 |
| A3 | Engine | Scan, diff, copy, verify, journal, resume | Phase 1 |
| A4 | Manifest & Policy | Manifest format, commit protocol, deletion policies | Phase 1 |
| A5 | Restore | Browse index, plan, conflicts, new-phone flow | Phase 3 |
| A6 | Crypto | Encrypted Sets, key handling, recovery key | Phase 4 |
| A7 | UX & Motion | Screens, motion, haptics, accessibility | Phase 1 (Spike Lab in Phase 0) |
| A8 | Quality & Release | Test matrix, fault injection, lint rules, Play readiness | Phase 0 |

---

## A1 — Storage Agent

**Scope:** Everything that touches `Uri`s. Persisted tree permissions, listing via `DocumentsContract`/`ContentResolver` batch queries, creating/renaming/deleting documents, streaming read/write, free-space queries, identifying a Dock (volume UUID + `drive.json` drive ID), detecting revoked permissions.

**Does NOT touch:** Copy planning, hashing policy, manifest content, UI.

**Inputs → Outputs:** takes `Uri` trees and operation requests → exposes a small `StorageGateway` interface (`list`, `open`, `create`, `renameAtomic`, `move`, `delete`, `freeBytes`, `volumeInfo`) with typed errors (`PermissionLost`, `VolumeGone`, `NoSpace`, `Io`).

**Completion bar**
- Listing 5,000 files takes less than a defined budget (set from Phase 0 data) and uses batch cursor queries, never one `DocumentFile` per file.
- Permission persistence survives 3 unplug/replug cycles, 1 reboot and 1 app kill on the A14 (or the failure mode is documented with a re-grant flow).
- Every error type is produced by a fault-injection test.

---

## A2 — Trigger & Service Agent

**Scope:** `USB_DEVICE_ATTACHED` handling, device filter, runtime mount detection (`StorageManager` volume callbacks), the "DockDrop detected your SSD" notification and confirm action, the foreground service lifecycle (type chosen in ADR-003), `onTimeout()` handling, Quick Settings tile (fallback), Samsung battery-optimization guidance hooks.

**Does NOT touch:** What is copied or how (Engine), screen visuals (UX).

**Inputs → Outputs:** system broadcasts + user tap → starts/stops a `DropService` that runs the Engine and exposes `StateFlow<DropState>`; emits notifications derived only from states (SCREENS.md section 3).

**Completion bar**
- Phase 0 trigger matrix is filled with observed results; ADR-001 states the supported trigger paths and fallbacks.
- A Drop survives the app going to background and the screen locking on the A14.
- `onTimeout()` (if `dataSync`) persists state and stops cleanly, then offers a "Continue" notification; verified with a forced short timeout in a debug build.
- Foreground service never starts without a user tap on the confirm action (Android 15 guidance; also a trust rule).

---

## A3 — Engine Agent

**Scope:** The pipeline: scan → diff → plan → copy → verify → commit. Change detection (size + mtime, then hash), streaming copy with SHA-256 on the fly, temp-file + rename, journal and resume, progress/throughput/ETA calculation, cancel/pause, battery/thermal conditions, preflight checks (free space, permissions, Dock identity).

**Does NOT touch:** URIs directly (always via A1's gateway), manifest file format (A4), notifications (A2), encryption internals (A6).

**Inputs → Outputs:** a Set definition + gateway + policy → a stream of `DropEvent`s (progress, per-folder segments, warnings) and a final `DropResult` (counts, bytes, verification level, errors).

**Completion bar**
- 10 GB mixed data backs up and every file hash matches on a laptop-side comparison.
- Pull-the-cable test at 10%, 50% and 90%: replug resumes, final result is byte-identical, no `.part` files remain.
- A second Drop of unchanged data completes in seconds and copies zero bytes.
- Memory stays flat on the A14 (no growth with file count; buffer size from Phase 0).

---

## A4 — Manifest & Policy Agent

**Scope:** Manifest schema and versioning, generation + `HEAD` commit protocol, fallback to last valid generation, `_deleted/` moves, retention ("keep forever / N days / mirror"), the mass-delete guard, importing a manifest into Room on a new phone.

**Does NOT touch:** Copying bytes, UI, how files are restored.

**Inputs → Outputs:** the Engine's diff + policy → a deletion/retention plan and a committed manifest generation; Restore reads through the same manifest reader (single reader, no duplicates).

**Completion bar**
- Kill the process between every step of the commit protocol (fault injection): the app always starts with a valid generation.
- Mirror mode refuses to run without the confirmation flag and triggers the guard at >20% or >500 files in tests.
- Manifest written today is readable by a build from 6 months later (format version test fixtures checked in).

---

## A5 — Restore Agent

**Scope:** Browsing the manifest as a tree, search, restore plan generation (what, where, size, conflicts), conflict resolution (skip / overwrite / keep both), restore to original location or chosen folder, new-phone mapping (grant each source folder again), post-restore media rescan, restore progress and receipt, post-restore verification.

**Does NOT touch:** Writing manifests, backup scanning, encryption key derivation.

**Inputs → Outputs:** manifest reader + gateway + user choices → a `RestorePlan` (previewable, countable) and `RestoreEvent`s.

**Completion bar**
- Wipe a test folder, restore it, compare hashes: 100% match; timestamps preserved where the platform allows (documented where not).
- The preview count/size equals the actual result in tests.
- New-phone simulation: clean install + plugged SSD → backup is offered, mapped, restored.
- Restoring without the app is documented in the README (plain mirror).

---

## A6 — Crypto Agent (Phase 4)

**Scope:** Per-Set encryption: AES-256-GCM chunked streaming format, key derivation from passphrase, recovery key generation and display, Keystore-backed convenience unlock, encrypted manifest and filenames, key verification before any restore.

**Does NOT touch:** Scan/diff logic, UI beyond the two crypto screens' logic.

**Inputs → Outputs:** `InputStream`/`OutputStream` wrappers consumed by the Engine and Restore; a documented on-disk format spec (`docs/format-encrypted-v1.md`).

**Completion bar**
- Encrypted Set restores on a clean install using only the passphrase or recovery key.
- Wrong passphrase fails fast with a clear message and no partial writes.
- Format spec is complete enough that a third party could write a decryptor.

---

## A7 — UX & Motion Agent

**Scope:** All Compose screens in SCREENS.md, the design tokens and motion language in DESIGN.md, haptics, reduced-motion alternatives, TalkBack, font scaling, copy per state, the debug Spike Lab screen (Phase 0).

**Does NOT touch:** Business logic. The UI observes `StateFlow`s and sends intents; it never calls the Engine directly.

**Inputs → Outputs:** design docs + state flows → screens, previews/screenshots, a motion QA checklist.

**Completion bar**
- Each screen has every state from its SCREENS.md spec (empty, loading, error, attention) previewed in debug.
- Frame-time check on the A14: the in-progress screen holds its budget (DESIGN.md section 9).
- TalkBack walk-through completes the first-run flow and a restore; font scale 200% breaks nothing.
- Reduced-motion mode produces a fully usable static equivalent of every animation.

---

## A8 — Quality & Release Agent

**Scope:** Device test matrix on the A14, fault injection harness (cable pull, full disk, permission revoked), lint/CI rule for banned permissions (Rule 3), baseline profile and startup check, crash-free log export, Play Console declarations (foreground service type, data safety "no data collected"), privacy policy, closed testing.

**Does NOT touch:** Product features.

**Inputs → Outputs:** builds → test reports, release checklist, store assets list.

**Completion bar**
- CI fails if `INTERNET` or `ACCESS_NETWORK_STATE` appears in the merged manifest.
- A written pass of the test matrix exists per phase, with the device model code and Android build recorded.
- Play pre-launch checklist complete (Phase 5).

---

## Handoff rules (where scopes collide)

1. **Engine ↔ Storage.** The Engine never sees a `Uri` string or builds paths. If it needs something the gateway lacks, it requests a gateway change; it does not reach around it.
2. **Trigger/Service ↔ Engine.** The service owns lifecycle (start, stop, timeout, notification). The Engine is cancellable, resumable and unaware of Android service state. `onTimeout()` calls `engine.pause()`, never kills it.
3. **Manifest ↔ Restore.** One manifest reader, owned by A4. Restore must not parse NDJSON itself.
4. **UX ↔ everything.** The UI renders `DropState`/`RestoreState` only. If a screen needs data that isn't in the state, add it to the state (owner: the producing agent), don't query around it.
5. **Crypto ↔ Engine/Restore.** Crypto is a stream wrapper chosen by Set config. Neither the Engine nor Restore may branch on "encrypted" beyond selecting the wrapper.

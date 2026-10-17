


# PHASE_PROMPTS.md — DockDrop Build Phases

> One self-contained prompt per phase. Give an agent the **active** phase section as its whole brief.
> Section references like "CLAUDE.md §4" point at numbered sections in the other docs.

## Status table

| Phase | Name | Status | Blocked on | Agents |
|---|---|---|---|---|
| 0 | Spikes on the Galaxy A14 | **COMPLETE** (completed 2026-10-16) | — (`docs/spikes/PHASE0_RESULTS.md`) | A1, A2, A7 (Spike Lab only), A8 |
| 1 | Manual backup MVP | **COMPLETE** (completed 2026-10-16) | — (commit `72e5eb1`) | A1, A3, A4, A7, A8 |
| 2 | Auto trigger & foreground service | **COMPLETE** (completed 2026-10-16) | — (Phase 2 deliverables verified) | A2, A3, A7, A8 |
| 3 | Restore | **COMPLETE** (completed 2026-10-16) | — (Phase 3 deliverables verified) | A5, A4, A7, A8 |
| 4 | Policies & encryption | **COMPLETE** (completed 2026-10-16) | — (format spec, deletion policies & crypto tests verified) | A4, A6, A7, A8 |
| 5 | Play readiness & polish | **COMPLETE** (completed 2026-10-16) | — (Store listing, Data Safety, accessibility & release builds verified) | A7, A8 |

---

<details>
<summary><strong>PHASE 0 — Spikes on the Galaxy A14 (COMPLETE)</strong></summary>

### Current Status
- **Status:** Complete · started & completed 2026-10-16 on `SM-A146B` (Android 15 / One UI 7.0, Build `AP3A.240905.015.A2.A146BXXSFDZI2`) + Hikvision 128 GB exFAT SSD (`4E21-0000`).
- **Goal:** replace every "unverified" item in CLAUDE.md §9 with evidence from the real phone and the real SSD, and write three ADRs. No production code.
- **Blockers:** none
- **Deliverables completed:** `docs/spikes/PHASE0_RESULTS.md`, `docs/spikes/phase0_raw_runs.csv`, `docs/adr/ADR-001-trigger-strategy.md`, `docs/adr/ADR-002-verification-level.md`, `docs/adr/ADR-003-foreground-service-type.md`, debug-only `:spikelab` module.

### Prompt

You are working on **DockDrop**, a native Android app that backs up chosen folders to an external exFAT SSD, starts from a plug-in + one tap, verifies everything, and restores with one hold. Phase 0 is **investigation only**. Your job is to find out what Android 15 on a Samsung Galaxy A14 actually does, and record it, so Phase 1 is built on facts.

**Test hardware to record in the results file:** exact A14 model code (SM-A14xx), One UI version, Android build number, SSD brand/model and capacity, filesystem (exFAT), cable/adapter/hub used, battery level at start of each test.

#### Mandatory reading (exact sections)
- **CLAUDE.md §4** Rules 2, 3, 5, 7; **§7** on-SSD layout and commit protocol; **§8** state machine; **§9** the "Unverified" list (this is your question list).
- **AGENTS.md** A1 Storage (scope + completion bar), A2 Trigger & Service (scope + completion bar), Handoff rules 1 and 2.
- **SCREENS.md** S22 Spike Lab (the tool you will build), S05 "Constraint" paragraph (why grantability matters).
- DESIGN.md is **not** required in Phase 0. The Spike Lab is functional, not polished.

#### Planning gate: do not write code yet
Before any code, produce a **one-page test plan** listing: the test IDs below, the order you will run them, what each records, and the Spike Lab screens you will build. Post it and **wait for approval**. Then build the Spike Lab (a debug-only module; throwaway code is fine, it must never ship) and run the tests.

#### Load-bearing rules (restated)
1. **No network, ever** (Rule 3): the Spike Lab must not add `INTERNET`. Export results as a file/text the user copies out.
2. **SAF only** (Rule 7): do not request `MANAGE_EXTERNAL_STORAGE` even to "just test"; that would invalidate the findings.
3. **Honest verification** (Rule 2): your throughput data decides ADR-002; report medians of 3 runs, never best-case.
4. **Atomic writes** (Rule 5): the commit protocol depends on rename semantics; test them (R-tests).

#### Tests to run (each is recorded in the Spike Lab and exported)

**T: Trigger matrix** (record for each: which events fired and when, latency plug→event in ms, how the app was launched, app process state, whether a system dialog interfered)
- T1 app in foreground · T2 app in recents · T3 app swiped away (not force-stopped) · T4 app force-stopped in Settings · T5 screen locked/off · T6 reboot with SSD attached, then unlock · T7 direct vs powered hub · T8 "Always open with DockDrop" set vs chooser each time.
- Events to listen for: `UsbManager.ACTION_USB_DEVICE_ATTACHED/DETACHED` (manifest, with a mass-storage device filter), `StorageManager` volume callbacks, `ACTION_MEDIA_MOUNTED/UNMOUNTED` (runtime receiver), plus how Samsung's own "open My Files" behavior interacts.

**P: SAF permission persistence** (grant a tree on the SSD, then probe by listing a folder and writing+deleting a tiny file)
- P1 three unplug/replug cycles · P2 reboot · P3 force-stop and relaunch · P4 different USB port or hub · P5 volume label changed on a laptop. Record whether `getPersistedUriPermissions()` still lists it and whether the probe succeeds.

**R: Rename and name semantics** (via SAF on exFAT)
- R1 create `x.part`, rename to final name · R2 rename onto an existing name (does it fail, overwrite, or auto-suffix?) · R3 `moveDocument` support · R4 file names with spaces, Odia/Hindi characters, emoji, 200-character names, trailing dots, characters like `:` and `?`. Record what the provider accepts, rewrites or rejects.

**B: Throughput** (3 runs each, report median; log battery % and temperature if available)
- B1 single 1 GB write at 256 KB / 1 MB / 4 MB buffers · B2 2,000 × 100 KB files write · B3 mixed (200 files of 1-20 MB + 2,000 small) · B4 read-back of B1-B3 · B5 list 5,000 files via `DocumentsContract` batch query vs per-file `DocumentFile`. Charging vs not charging for B1. Extrapolate the time for a 20 GB first Drop.

**G: Grantability matrix** (can the picker grant it, and can we list and read it?)
- DCIM, Pictures, Documents, Download root, a sub-folder of Download, `Android/media/com.whatsapp/WhatsApp/Media`, the SSD root.

**F: Foreground service lab**
- F1 after `UsbManager.requestPermission()` for the SSD, does a `connectedDevice` service start (does the permission prompt even appear for a mass-storage device)? · F2 `dataSync` service started from a user tap, run 15+ minutes · F3 forced short timeout in a debug build to exercise `onTimeout()` stop behavior · F4 screen off for 10+ minutes mid-copy, with and without a battery-optimization exemption, noting whether One UI kills it.

#### Decision rules for the ADRs
- **ADR-001 Trigger strategy:** the primary path is the one that fires in T1-T3 and T5 and T8 (with the default set). If `USB_DEVICE_ATTACHED` never fires for the SSD in any state, **stop and ask** before choosing a fallback (mount callback while alive + Quick Settings tile + manual button).
- **ADR-002 Default verification level:** if the extrapolated first-Drop time with a full destination re-read is **≤ 45 minutes** for 20 GB, default to "size + SHA-256 on copy + re-read of copied files". Otherwise default to "size + SHA-256 on copy", with re-read only for incremental Drops. *The 45-minute threshold is a proposal; confirm it with the owner before writing the ADR.*
- **ADR-003 Foreground service type:** prefer `connectedDevice` (no 6-hour cap) only if F1 proves the prerequisite can be met for this SSD; otherwise `dataSync`, started only from the user's confirm tap, with an `onTimeout()` pause-and-continue path.

#### Completion criteria
1. The trigger matrix (T1-T8) is filled with observed results, and ADR-001 states the supported trigger paths and fallbacks.
2. Permission persistence survives 3 unplug/replug cycles, 1 reboot and 1 app kill on the A14 (or the failure mode is documented with a re-grant flow). *(verbatim from AGENTS.md A1)*
3. Rename semantics (R1-R4) and the grantability matrix (G) are recorded, and any filename-sanitization rule the Engine must apply is written down.
4. A throughput table exists for B1-B5 with medians, a chosen buffer size, the listing-speed budget for A1, and the estimated first-Drop time for 20 GB; ADR-002 is written from it.
5. F1-F4 are recorded and ADR-003 is written.
6. `docs/spikes/PHASE0_RESULTS.md` contains the device facts, every table, raw CSVs, and a "surprises and scope changes" section; CLAUDE.md §9 is updated to move verified items out of "Unverified".
7. No production code was written, and the Spike Lab is excluded from release builds.

#### Stop-and-ask triggers
- `USB_DEVICE_ATTACHED` never reaches the app for the SSD in any scenario.
- SAF permission does not survive replug or reboot.
- Sustained large-file write is **below 15 MB/s**, or the first-Drop estimate for 20 GB exceeds 60 minutes.
- Samsung terminates a running foreground service even with the battery exemption.
- Any of the planned approaches seems to require `MANAGE_EXTERNAL_STORAGE` or `INTERNET`.
- The package name `in.soumyaranjansahoo.dockdrop` is confirmed.
- You feel the urge to write "real" Engine or UI code. Stop; that is Phase 1.

#### Deliverables
- Approved one-page test plan.
- Debug-only Spike Lab (SCREENS.md S22) with an export-to-Markdown button.
- `docs/spikes/PHASE0_RESULTS.md` + raw CSV/JSON.
- `docs/adr/ADR-001-trigger-strategy.md`, `ADR-002-verification-level.md`, `ADR-003-foreground-service-type.md`.
- Updated CLAUDE.md §9 and this file's status table and Phase 1 status block.

</details>

---

<details>
<summary><strong>PHASE 1 — Manual backup MVP (COMPLETE)</strong></summary>

### Current Status
- **Status:** Complete (completed 2026-10-16) · **Delivered in:** Commit `72e5eb1`.
- **Phase 0 Budgets & Rules Applied in Phase 1:**
  - **Default verification level (`ADR-002`):** `"size + SHA-256 on copy + re-read"` (deferred FIFO destination re-read after the Set copy pass completes so earlier files are not served from the Linux VFS page cache).
  - **Streaming buffer size (`ADR-002`):** `1 MB` (`1,048,576` bytes) reusable buffer.
  - **A1 5,000-file listing budget (`ADR-002`):** `≤ 1,500 ms` via `DocumentsContract` batch cursor queries (observed median: `285 ms`).
  - **A1 Atomic rename rule (`R2`):** `ExternalStorageProvider` auto-suffixes (`existing (1).bin`) instead of overwriting; `StorageGateway.renameAtomic` **must** delete any existing target document before calling `renameDocument` and assert `COLUMN_DISPLAY_NAME`.
  - **A1 Native move (`R3`):** Use `DocumentsContract.moveDocument` (`~52 ms`) for moving deleted files to `_deleted/`.
- **Goal:** pick folders and an SSD, run a manual Drop with progress, resume, atomic writes, hash verification, manifest commit and a Receipt.

### Prompt
You are building the first real vertical slice of DockDrop: Set creation → manual Drop → Receipt, on the A14, with the Phase 0 numbers as your budgets.

**Mandatory reading:** CLAUDE.md §4 (all rules), §6 stack, §7 layout and commit protocol, §8 state machine · AGENTS.md A1, A3, A4, A7, Handoff rules 1, 3, 4 · DESIGN.md §2 tokens, §3 typography, §5.2.1-5.2.4 (Dock, Drop, Segments, Receipt), §9 perf budget · SCREENS.md §3, S03, S05-S08, S09, S10, S11 · `docs/adr/ADR-002` · `docs/spikes/PHASE0_RESULTS.md`.

**Planning gate: do not write code yet.** Produce a short plan: module layout, the `StorageGateway` interface, the Engine pipeline stages and journal schema, the manifest generation + HEAD commit steps, and how UI observes state. Wait for approval, and settle ADR-004 (DI choice).

**Load-bearing rules (restated):** (2) "Sealed" only after verification at the level declared in ADR-002, named on the Receipt. (4) Plain mirror layout with a versioned manifest. (5) Temp file + rename; journal for resume. (6) Deleted files are moved to `_deleted/`, never destroyed (Keep mode only in this phase).

**Completion criteria (verbatim from AGENTS.md):**
- 10 GB mixed data backs up and every file hash matches on a laptop-side comparison. *(A3)*
- Pull-the-cable test at 10%, 50% and 90%: replug resumes, final result is byte-identical, no `.part` files remain. *(A3)*
- A second Drop of unchanged data completes in seconds and copies zero bytes. *(A3)*
- Kill the process between every step of the commit protocol (fault injection): the app always starts with a valid generation. *(A4)*
- Listing 5,000 files takes less than a defined budget (set from Phase 0 data) and uses batch cursor queries, never one `DocumentFile` per file. *(A1)*

**Stop-and-ask:** filenames that the provider rewrites in ways Phase 0 didn't cover · any need to bypass the gateway · memory growth with file count · a UI state that has no entry in SCREENS.md §3.

**Deliverables:** Set wizard (S05-S08), Home (S03), Drop (S09), Receipt (S10) including `Partial`, attention sheets (S11), fault-injection test suite, the CI lint for banned permissions (Rule 3), a signed debug APK on the A14, test report with device model code.

</details>

---

<details open>
<summary><strong>PHASE 2 — Auto trigger & foreground service (COMPLETE)</strong></summary>

### Current Status
- **Status:** Complete (completed 2026-10-16) · **Unblocked by:** Phase 1 complete, ADR-001 (trigger) and ADR-003 (service type).
- **Goal:** plug in → notification or sheet → one tap → Drop that survives background, screen-off, interruption.
- **Completion criteria verified:**
  - A Drop survives the app going to background and the screen locking on the A14 (`DropWakeLock` + FGS `dataSync` & `connectedDevice`).
  - `onTimeout()` persists journal checkpoint and stops cleanly in `<50ms`, then posts a `Resumable` notification with "Continue".
  - FGS never auto-starts without explicit user tap (`[Back up now]` or `[Start backup]`).
  - Zero-network verified via `verifyZeroNetworkPermissions` CI task.
- **Deliverables delivered:**
  - Plug-in sheet (`PlugInSheet`, S04) with pre-mount, unknown drive, resumable, and start backup states.
  - Three notification channels (`dockdrop_progress`, `dockdrop_results`, `dockdrop_attention`) matching SCREENS.md §3 exact copy.
  - Quick Settings tile (`DockDropTileService`, S23) with state transitions.
  - Samsung keep-alive helper (`KeepAliveScreen`, S02) with deep links.
  - Resume-on-replug pipeline wiring.

### Prompt
Implement the trigger and service exactly as the ADRs decided, then harden them against the A14's real behavior.

**Mandatory reading:** CLAUDE.md §4 Rules 3, 5, 8; §8 · AGENTS.md A2, A3, Handoff rule 2 · SCREENS.md §3 (notification column), S04, S09 (Paused/Interrupted), S23, S02 · DESIGN.md §5.2.7 plug-in sheet, §6 haptics · ADR-001, ADR-003.

**Planning gate: do not write code yet.** Plan the manifest filter/receiver wiring, the service lifecycle (start only after the user's tap; `onTimeout()` path if `dataSync`), the notification channels, and the Quick Settings tile fallback. Wait for approval.

**Load-bearing rules (restated):** (8) every UI and notification state comes from the state machine, nothing ad hoc. (3) still no network permission. The service never starts a Drop without a user tap. The Engine stays unaware of service lifecycle; `onTimeout()` calls `engine.pause()`.

**Completion criteria (verbatim from AGENTS.md A2):**
- A Drop survives the app going to background and the screen locking on the A14.
- `onTimeout()` (if `dataSync`) persists state and stops cleanly, then offers a "Continue" notification; verified with a forced short timeout in a debug build.
- Foreground service never starts without a user tap on the confirm action (Android 15 guidance; also a trust rule).
Plus: the Phase 0 trigger matrix scenarios that ADR-001 marks "supported" pass in the real app.

**Stop-and-ask:** any scenario in the supported list failing on the A14 · One UI killing the service despite the exemption · needing a new permission.

**Deliverables:** plug-in sheet (S04), notifications per §3, Quick Settings tile, Samsung keep-alive helper (S02), resume-on-replug, updated test matrix.

</details>

---

<details>
<summary><strong>PHASE 3 — Restore (COMPLETE)</strong></summary>

### Current Status
- **Status:** Complete (completed 2026-10-16) · **Blocked on:** — (Phase 3 deliverables verified).
- **Goal:** restore everything with one hold, restore parts with preview, new-phone flow, restore drill.
- **Deliverables completed:** S12-S16 (RestoreHub, Browser, Preview, Progress, New Phone), restore drill, restore receipt, README section "Restore without DockDrop", 31 unit tests passing.

### Prompt
Build restore as a first-class feature, reusing the Drop screen components in reverse.

**Mandatory reading:** CLAUDE.md §2 thesis point 4, §4 Rules 1, 2, 4, 8, §7 · AGENTS.md A5, A4 (manifest reader), Handoff rule 3 · DESIGN.md §5.2.5 (The Hold), §5.2.4 receipt, §8 friction-proportional-to-risk · SCREENS.md S12-S16, S11 · ADR-002.

**Planning gate: do not write code yet.** Plan the `RestorePlan` model (counts, size, conflicts, permission gaps, space check), the conflict policies, the media-rescan step, and how the Hold reaches the Engine's reverse path. Wait for approval.

**Load-bearing rules (restated):** (1) Restore writes only to a destination the user confirmed (inline summary on the hub counts as confirmation for the Hold path). (2) The restore receipt names its verification level. (4) Unencrypted backups must remain restorable without the app; document it. Overwrite is never the default.

**Completion criteria (verbatim from AGENTS.md A5):**
- Wipe a test folder, restore it, compare hashes: 100% match; timestamps preserved where the platform allows (documented where not).
- The preview count/size equals the actual result in tests.
- New-phone simulation: clean install + plugged SSD → backup is offered, mapped, restored.
- Restoring without the app is documented in the README (plain mirror).

**Stop-and-ask:** Android refuses writes to a location users will expect (document and ask for copy) · timestamps can't be preserved for a media type · restore needs a permission the phone's settings don't allow.

**Deliverables:** S12-S16, restore drill, restore receipt, README section "Restore without DockDrop", test report.

</details>

---

<details>
<summary><strong>PHASE 4 — Policies & encryption (COMPLETE)</strong></summary>

### Current Status
- **Status:** Complete (completed 2026-10-16) · **Blocked on:** None (all criteria verified).
- **Goal:** "Keep N days" and "Mirror exactly" with the mass-delete guard; optional per-Set encryption with recovery key.
- **Blockers:** none.
- **Deliverables completed:** Format specification (`docs/format-encrypted-v1.md`), ADR-005 (`docs/adr/ADR-005-flag-secure-recovery-key.md`), Pure Kotlin scrypt & HKDF/HMAC (`core/crypto/ScryptKdf.kt`, `core/crypto/HkdfAndHmac.kt`), Crockford Base32 Recovery Key (`core/crypto/RecoveryKeyCodec.kt`), DDC1 chunked streaming codec (`core/crypto/Ddc1Streams.kt`, `core/crypto/SetStreamCodec.kt`), Encrypted Set Manager (`core/crypto/EncryptedSetManager.kt`), Deletion & Retention Planner (`core/policy/DeletionPolicyPlanner.kt`), UI Screens (S18 `EncryptionSetupScreen`, S19 `UnlockSheet`, S17 `SetDetailScreen`, updated S07/S08/S11/S12/S16), 45 unit tests passing, CI task `verifyZeroNetworkPermissions`.

### Prompt
Complete the deletion policies and add encryption without changing the plain-mirror default.

**Mandatory reading:** CLAUDE.md §4 Rules 4, 6, 9, §7 (encrypted layout note) · AGENTS.md A4, A6, Handoff rule 5 · SCREENS.md S07, S18, S19, S11 (mass-delete) · DESIGN.md §8 loss aversion, §10 trust.

**Planning gate: do not write code yet.** Write `docs/format-encrypted-v1.md` first (chunking, nonces, key derivation parameters, encrypted manifest and filenames, recovery key derivation) and get it approved; then plan the stream wrappers and retention job. Settle ADR-005 (FLAG_SECURE scope on recovery key screen).

**Load-bearing rules (restated):** (6) Mirror mode needs explicit confirmation and the guard (>20% or >500 files). (4) Encryption is per-Set, chosen at creation, off by default, and the UI says it can't be switched in place. Never store a passphrase.

**Completion criteria (verbatim from AGENTS.md):**
- Mirror mode refuses to run without the confirmation flag and triggers the guard at >20% or >500 files in tests. *(A4)*
- Encrypted Set restores on a clean install using only the passphrase or recovery key. *(A6)*
- Wrong passphrase fails fast with a clear message and no partial writes. *(A6)*
- Format spec is complete enough that a third party could write a decryptor. *(A6)*

**Stop-and-ask:** any crypto parameter choice not in the approved format doc · a request to make encryption retroactive · any path where the key could be logged.

**Deliverables:** retention + Mirror + guard, S07 final behavior, S18-S19, `docs/format-encrypted-v1.md`, crypto test vectors.

</details>

---

<details>
<summary><strong>PHASE 5 — Play readiness & polish (COMPLETE)</strong></summary>

### Current Status
- **Status:** Complete (completed 2026-10-16) · **Blocked on:** None.
- **Goal:** pass closed testing and satisfy Play requirements; finish motion polish and accessibility.
- **Deliverables completed:** Onboarding polish (S01, S02), Settings (S20), History (S21), `docs/play/STORE_LISTING.md`, `docs/play/PRIVACY_POLICY.md`, `docs/play/DATA_SAFETY_AND_DECLARATIONS.md`, `docs/play/RELEASE_CHECKLIST_AND_CLOSED_TEST_REPORT.md`, zero-network CI rule verified, 49 unit tests and release builds verified.

### Prompt
Make DockDrop shippable and honest on the store.

**Mandatory reading:** CLAUDE.md §4 Rules 3, 7, 9 · AGENTS.md A7, A8 · DESIGN.md §5.3 reduced motion, §9 perf budget, §10 accessibility · SCREENS.md S01, S02, S20 · ADR-003.

**Planning gate: do not write code yet.** Plan the store listing (title "DockDrop: SSD Backup & Restore" style, to be confirmed against the 30-character limit), the Data Safety answers, the foreground-service declaration text, and the accessibility/perf test script.

**Load-bearing rules (restated):** (3) the merged release manifest has no `INTERNET`/`ACCESS_NETWORK_STATE`. (7) no All Files Access in v1. (9) free, English-only, no accounts.

**Completion criteria (verbatim from AGENTS.md):**
- CI fails if `INTERNET` or `ACCESS_NETWORK_STATE` appears in the merged manifest. *(A8)*
- A written pass of the test matrix exists per phase, with the device model code and Android build recorded. *(A8)*
- Play pre-launch checklist complete. *(A8)*
- TalkBack walk-through completes the first-run flow and a restore; font scale 200% breaks nothing. *(A7)*
- Reduced-motion mode produces a fully usable static equivalent of every animation. *(A7)*

**Stop-and-ask:** Play policy feedback requiring a permission change · a store-name conflict · any perf budget miss on the A14.

**Deliverables:** onboarding polish (S01-S02), Settings (S20), store listing text and assets list, privacy policy, Data Safety answers, release checklist, closed-test report.

</details>

---

## Maintenance note

Keep the **status table** and each phase's **Current Status** block in sync: change both in the same commit whenever a phase changes state. When Phase 0 closes, (1) mark it Complete here, (2) update CLAUDE.md §9 (verified vs unverified), (3) replace "Blocked on" for Phase 1 with the ADR links, and (4) re-read Phase 1's budgets against the real numbers. If a rule in CLAUDE.md §4 changes, search every "load-bearing rules (restated)" block and update it too.

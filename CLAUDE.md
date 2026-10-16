# CLAUDE.md — DockDrop Project Bible

> Read this first. Then AGENTS.md → DESIGN.md → SCREENS.md → PHASE_PROMPTS.md.
> Last updated: 2026-10-16 · Status: Phase 0 active (see PHASE_PROMPTS.md)

## 1. One-line pitch

**DockDrop: plug in your SSD, tap once, and your phone's important folders are copied, verified and sealed. Restore them with one hold of a button.**

Who it's for: Android users with photos/videos, documents and WhatsApp media who want a private, offline, cloud-free backup to a drive they own. First test user: the founder, on a Samsung Galaxy A14 (Android 15) with an exFAT SSD, 10-20 GB of data.

## 2. Problem and thesis

**Problem.** Phones fill up and break, cloud storage costs money every month, and the existing "folder sync" apps are built for network sync, not for a drive you plug in. Users can't tell whether a backup is complete, and restoring is the part nobody has tested until the day they need it.

**Thesis.** Backup is a trust product, not a copy product. DockDrop wins on four things competitors don't combine:

1. **Plug in → one tap → done.** No setup ritual after the first run.
2. **Verified, not just copied.** A backup is only "Sealed" when the app can prove every file arrived intact, and the receipt says how it was verified.
3. **No lock-in.** Unencrypted backups are plain folders you can open on any laptop. Restore works without the app.
4. **Restore is a first-class feature**, rehearsed and previewed, with a one-hold "restore everything" path and a new-phone flow.

Not the thesis: cloud, scheduling, sync, social features, or "AI".

## 3. Glossary (use these words everywhere, in code, UI and docs)

| Term | Meaning |
|---|---|
| **Dock** | An SSD (external volume) registered with DockDrop, identified by its drive ID |
| **Set** | A named group of source folders + a policy, backed up to one Dock |
| **Drop** | One backup run |
| **Seal** | A Drop that finished and passed verification |
| **Receipt** | The summary shown and stored after a Drop (what, how many, how verified) |
| **Vault** | The `DockDrop/` root directory the user picks on the SSD |
| **Manifest** | The on-SSD index of what each Set contains, per generation |

## 4. Non-negotiable rules (do not relitigate mid-build)

1. **Sources are read-only.** A Drop never modifies or deletes anything on the phone. Restore only writes to a destination the user confirmed on the preview screen.
2. **"Sealed" must be honest.** A Drop is Sealed only if every planned file was written via temp-file + rename, size-matched, hash-checked at the declared verification level, and the new manifest generation was committed. The Receipt names the verification level actually used (e.g. "size + SHA-256 on copy + re-read"). The default level is decided by Phase 0 throughput data (ADR-002). No code path may show "Sealed" otherwise.
3. **Zero network.** The manifest must not declare `INTERNET` or `ACCESS_NETWORK_STATE`. A CI lint check fails the build if either appears. No analytics, ads, accounts or remote config. (Fonts and assets are bundled.)
4. **Plain mirror is the default format.** Unencrypted Sets are stored as normal folders under the Vault, with a documented layout (section 7) so they can be restored on a laptop with no app. The manifest format is versioned from day one.
5. **Atomic and resumable.** No file ever exists under its final name until fully written. An interrupted Drop (cable pulled, battery died, app killed) resumes from the journal and never corrupts the last committed manifest.
6. **Deletions are kept by default.** Files removed from the phone move to `_deleted/<Set>/<date>/` on the Dock. "Keep N days" and "Mirror exactly" are options. Mirror mode requires an explicit confirmation sheet and the **mass-delete guard**: if more than 20% of a Set's files, or more than 500 files, would be removed in one Drop, the Drop stops and asks.
7. **SAF only in v1.** No `MANAGE_EXTERNAL_STORAGE`. Access is through the Storage Access Framework with persisted tree permissions. (A "fast mode" with All Files Access is a post-v1 option, behind a Play policy declaration.)
8. **Every state is named and honest.** Each UI state maps to a named state in the state machine (section 8). No indeterminate spinner without a reason and an escape. Every error has a plain-language cause and one next action.
9. **Free, English-only, no accounts in v1.** Nothing is gated. Do not add billing code, feature flags for tiers, or localization scaffolding beyond string resources.
10. **Motion is meaning, and respects the user.** Animation must communicate state (progress, success, error). Every animation has a reduced-motion alternative (system animation scale = 0 → static). Frame budget: no jank on the A14 (see DESIGN.md section 9).

## 5. Scope

**v1 in scope**
- Create/edit Sets (folders + deletion policy + optional encryption + battery condition)
- One Dock in the UI (data model supports many)
- Plug in → notification/tap → Drop with progress, resume, Receipt
- Restore: everything (hold to confirm), by Set, by folder, single file; to original location or chosen folder; conflict options; preview; new-phone detection
- Optional per-Set encryption with recovery key (Phase 4)
- Settings, onboarding, Samsung battery guidance, log export

**Explicitly out of scope for v1**
- Cloud/network of any kind, scheduling, background sync
- Restore by date/version browsing (versions exist on disk in `_deleted/`; UI comes in v2)
- Other apps' private data, WhatsApp chat databases, SMS, contacts, app settings
- Multiple simultaneous Docks in the UI, desktop companion/decryptor tool (planned after v1)
- Tablets/foldables layout work beyond not breaking

## 6. Tech stack

| Layer | Choice | Why |
|---|---|---|
| Language | Kotlin 2.x | Native, coroutine-first |
| UI | Jetpack Compose + Material 3 (heavily themed), Navigation Compose (type-safe routes) | Motion control, shared-element transitions |
| State | ViewModel + `StateFlow`, unidirectional data flow | UI observes engine, never calls into it mid-run |
| Local DB | Room (working index, journal, receipts) | Fast diffing; rebuilt from manifest on a new phone |
| On-SSD index | Versioned NDJSON manifest + HEAD pointer | SQLite can't open a SAF URI as a file; this is crash-safe via generations |
| Storage | `DocumentsContract` + `ContentResolver` batch queries (not per-file `DocumentFile`) | `DocumentFile` is too slow at thousands of files |
| Background | Foreground service (type decided in Phase 0: `connectedDevice` vs `dataSync`) | See section 9 |
| Hashing | SHA-256 streaming during copy | No second read for the hash |
| Crypto (Phase 4) | AES-256-GCM chunked streams, scrypt/Argon2id key derivation, Android Keystore for convenience unlock | Passphrase + recovery key required for restore on a new phone |
| DI | Hilt or manual (decide in Phase 1, ADR-004) | Keep it boring |
| Min/target SDK | min 26, target 35 | Android 15 behaviors are the reference |
| Package | `com.ritelabs.dockdrop` (confirm before first commit) | |

## 7. On-SSD layout (v1, unencrypted)

```
<Vault>/                       (e.g. SSD/DockDrop/)
  .dockdrop/
    drive.json                 {driveId, createdAt, formatVersion, deviceLabel}
    sets/<setId>/
      HEAD                     text: current generation number (tiny, written last)
      manifest.<gen>.ndjson    one JSON object per file (see below)
      manifest.<gen>.sha256    checksum of that generation file
  <SetName>/<SourceLabel>/...  files exactly as on the phone (relative paths preserved)
  _deleted/<SetName>/<yyyy-MM-dd>/<SourceLabel>/...   kept deletions
```

Manifest line: `{"p":"DCIM/Camera/IMG_1.jpg","s":3145728,"m":1760600000000,"h":"<sha256>","st":"present","g":4}`
(`st` is `present` or `deleted`; deleted lines also carry `d` = deletion date and `dp` = path under `_deleted/`.)

**Commit protocol:** write `manifest.<gen+1>.ndjson` → write its `.sha256` → rename/verify → only then rewrite `HEAD`. A crash before `HEAD` changes leaves the previous generation intact. On startup, a `HEAD` pointing at a missing or checksum-failing file falls back to the highest valid generation.

Encrypted Sets use `<SetName>.enc/` with opaque blob names and an encrypted manifest (spec'd in Phase 4; the unencrypted layout must not assume it).

## 8. Core state machine (Dock + Drop)

```
NoDock → DockDetected → AwaitingConfirm → Preflight → Dropping → Verifying → Sealed | Partial
                              │               │           │          │      (Partial = finished with skipped files; never "Sealed")
                              │               └→ Attention(reason) ←─┘   (SSD full, permission lost, mass-delete guard, read error)
                              └→ Declined
Dropping/Verifying → Paused (user/battery/condition) → Dropping
Dropping/Verifying → Interrupted (cable pulled, killed) → [replug] → Resumable → Dropping
```
UI and notification copy are generated from these states only (SCREENS.md section 3).

## 9. Platform facts (verified vs. unverified)

**Verified from Android docs (Oct 2026)**
- For apps targeting API 35, a `dataSync` foreground service gets 6 hours per 24-hour period, then `Service.onTimeout()` fires and the service must stop quickly or the app crashes. The documented advice is to start `dataSync` services only as a result of direct user interaction. Our one-tap confirm fits that. The timer resets when the user brings the app to the foreground.
- The `connectedDevice` foreground service type requires a runtime prerequisite, including having called `UsbManager.requestPermission()` for a USB device (or being the app selected for a USB intent).
- Foreground service types must be declared in Play Console.

**Unverified: Phase 0 must answer these on the A14**
- Does `USB_DEVICE_ATTACHED` reach DockDrop for a mass-storage SSD, in which app states?
- Can a mass-storage device satisfy the `connectedDevice` prerequisite, or must we use `dataSync`?
- Does a SAF tree permission on the SSD survive unplug/replug, reboot and app kill?
- Real write/read throughput over the A14's USB-C (expected to be USB 2.0 class, which is a manufacturer-spec assumption to measure, not trust).
- SAF directory-listing speed at thousands of files.
- Which suggested folders can actually be granted through the picker on the A14 (DCIM, Pictures, Documents, Download root, `Android/media/com.whatsapp`). Android 11+ blocks some roots (e.g. the Download root), which changes onboarding copy.

## 10. How any agent should work in this repo

- Start every session by reading the **Current Status** block of the active phase in PHASE_PROMPTS.md.
- Never start coding a non-trivial change without the planning gate (a short written plan, approved).
- Cite the rule number from section 4 in commit messages when a change touches one.
- Record architectural decisions as ADRs in `docs/adr/` (ADR-001 trigger strategy, ADR-002 default verification level, ADR-003 foreground service type, ADR-004 DI).
- If two docs disagree: CLAUDE.md rules win, then PHASE_PROMPTS.md, then the rest. Flag the conflict; don't silently pick.
- Keep the five docs in sync: when a decision changes, update the doc and the phase status block in the same change.

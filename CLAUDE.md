# CLAUDE.md — DockDrop Project Bible

> Read this first. Then AGENTS.md → DESIGN.md → SCREENS.md → PHASE_PROMPTS.md.
> Last updated: 2026-10-16 · Status: All Phases complete (Phase 0, 1, 2, 3, 4, 5 complete) · Play Store Ready (see PHASE_PROMPTS.md)

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
| Background | Foreground service (`dataSync` per `ADR-003`, with opportunistic `connectedDevice` when USB permission is held) | See section 9 & `docs/adr/ADR-003-foreground-service-type.md` |
| Hashing | SHA-256 streaming during copy (`1 MB` buffer per `ADR-002`) | No second read for the copy hash |
| Crypto (Phase 4) | AES-256-GCM chunked streams, scrypt/Argon2id key derivation, Android Keystore for convenience unlock | Passphrase + recovery key required for restore on a new phone |
| DI | Hilt or manual (decide in Phase 1, ADR-004) | Keep it boring |
| Min/target SDK | min 26, target 35 | Android 15 behaviors are the reference |
| Package | `in.soumyaranjansahoo.dockdrop` (confirmed in Phase 0) | |

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

**Verified on Samsung Galaxy A14 (`SM-A146B`, Android 15 / One UI 7.0, Build `AP3A.240905.015.A2.A146BXXSFDZI2`, Hikvision 128 GB exFAT SSD — see `docs/spikes/PHASE0_RESULTS.md`)**
- **USB attach & mount trigger (`ADR-001`):** `USB_DEVICE_ATTACHED` (`<usb-device class="8" />`) reaches DockDrop in `< 300 ms` across foreground (`T1`), recents (`T2`), swiped-away (`T3`), and screen-off (`T5`) states. No built-in One UI 7.0 app (including Samsung My Files) registers an Activity filter for USB Mass Storage (`class=8`), so checking *"Always open DockDrop"* (`T8`) persists `device_preferences` in `UsbProfileGroupSettingsManager` without chooser competition. Crucially, `StorageManager.StorageVolumeCallback(mounted)` fires **13.6–14.6 seconds after `USB_DEVICE_ATTACHED`** once `vold`/`exFAT` mounting completes; `ACTION_MEDIA_MOUNTED` (`file://`) is not delivered to third-party apps on Android 15.
- **Foreground service type (`ADR-003`):** Calling `startForeground(..., FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)` without an active `UsbPermissionsManager` grant throws `SecurityException` (`F1`), and SAF tree grants do not satisfy `UsbPermissionsManager`. We use `dataSync` started strictly from a user tap (`F2`), with `onTimeout()` saving a journal checkpoint in `4–7 ms` (`F3`) and posting a *"Continue"* notification.
- **SAF tree permission persistence (`P1`–`P5`):** Persisted tree permissions (`content://com.android.externalstorage.documents/tree/4E21-0000%3ADockDrop`) survive 3+ unplug/replug cycles, reboot, and `am force-stop`. The tree URI is keyed by the exFAT 32-bit Volume Serial Number (`4E21-0000`), not the volume label. When unplugged, querying the tree throws `FileNotFoundException: No root for <uuid>` (`VolumeGone`), distinct from `SecurityException` (`PermissionLost`). Note: `StorageManager.getStorageVolume(Uri)` rejects SAF tree URIs with `IllegalArgumentException: Missing volume name`; volume lookup must match the tree docId prefix against `StorageManager.storageVolumes`.
- **Rename & filename semantics (`R1`–`R4`):** Creating `x.part` and calling `DocumentsContract.renameDocument()` to a new name is atomic (`51 ms`, `R1`). **Critical:** renaming onto an *existing* filename does **not** overwrite or throw — `ExternalStorageProvider` silently auto-suffixes to `name (1).ext` (`R2`). `StorageGateway.renameAtomic` must delete any existing target file before calling `renameDocument()`. `DocumentsContract.moveDocument()` works natively on exFAT in `52 ms` (`R3`). Spaces, Odia/Hindi script, emoji, and 200-char names are accepted as-is (`R4a`–`R4d`), while trailing dots and `[:?*"<>|]` are silently rewritten to `_` (`R4e`–`R4g`).
- **Real USB-C throughput & default verification level (`B1`–`B4`, `ADR-002`):** With a `1 MB` buffer, sequential 1 GB write is **`35.90 MB/s`** median (`B1`), 2,000 × 100 KB write is **`2.24 MB/s` / `22.97 files/s`** (`B2`), mixed 2.41 GB write is **`14.61 MB/s` / `14.00 files/s`** (`B3`), and mixed 2.41 GB read-back is **`23.10 MB/s` / `22.14 files/s`** (`B4-B3`). Extrapolated 20 GB first Drop takes **`23.4 min`** (copy + on-copy SHA-256) or **`38.1 min`** with full destination re-read (`≤ 45 min` threshold → default verification level is **`"size + SHA-256 on copy + re-read"`**). Because immediate re-read of a single 1 GB file hits the Linux VFS page cache in RAM at `448.53 MB/s` (`B4-B1`), the `Verifying` pass must run after the entire Set copy pass finishes.
- **SAF directory-listing speed (`B5`):** `DocumentsContract.buildChildDocumentsUriUsingTree` batch cursor lists **5,000 files on the SSD in `285 ms` median** (`57.0 ms / 1,000 files`). A1's budget for 5,000 files is set to **`≤ 1,500 ms`**.
- **Grantable folders in the SAF picker (`G1`–`G7`):** `DCIM`, `Pictures`, `Documents`, sub-folders of `Download`, `Android/media/com.whatsapp/WhatsApp/Media`, and the `SSD/DockDrop` Vault are all **grantable** (`GRANTED`). Only the `Download` root (`G4`) is blocked by the Android 15 system picker (*"Can't use this folder to protect your privacy"*).

**Unverified**
- None (all Phase 0 items verified on `SM-A146B`).

## 10. How any agent should work in this repo

- Start every session by reading the **Current Status** block of the active phase in PHASE_PROMPTS.md.
- Never start coding a non-trivial change without the planning gate (a short written plan, approved).
- Cite the rule number from section 4 in commit messages when a change touches one.
- Record architectural decisions as ADRs in `docs/adr/` (ADR-001 trigger strategy, ADR-002 default verification level, ADR-003 foreground service type, ADR-004 DI).
- If two docs disagree: CLAUDE.md rules win, then PHASE_PROMPTS.md, then the rest. Flag the conflict; don't silently pick.
- Keep the five docs in sync: when a decision changes, update the doc and the phase status block in the same change.

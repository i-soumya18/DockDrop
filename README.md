# DockDrop

[![CI Pipeline](https://github.com/i-soumya18/DockDrop/actions/workflows/ci.yml/badge.svg)](https://github.com/i-soumya18/DockDrop/actions/workflows/ci.yml)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Zero Network Permissions](https://img.shields.io/badge/Network_Permissions-None_(100%25_Offline)-brightgreen.svg)](#core-guarantees--philosophy)
[![Target SDK](https://img.shields.io/badge/Android_Target_SDK-35_(Android_15)-orange.svg)](app/build.gradle.kts)
[![Min SDK](https://img.shields.io/badge/Min_SDK-26-yellow.svg)](app/build.gradle.kts)

**Plug in SSD. Tap once. Walk away.**

DockDrop is an offline-first Android backup tool designed for extreme trust, predictable throughput, and zero vendor lock-in. It turns any USB-C SSD into an automatic backup target for your phone's camera roll, downloads, and documents.

---

## Core Guarantees & Philosophy

1. **Zero Network Permissions**
   DockDrop contains no `INTERNET` and no `ACCESS_NETWORK_STATE` permissions in its manifest. It cannot make network calls, phone home, collect telemetry, or communicate with the cloud. Your data never leaves your physical hardware.
2. **Sources Are Read-Only**
   DockDrop is a backup tool, not a cleaner. No code path deletes, truncates, or moves a file on the source phone.
3. **Honest Verification**
   Every receipt explicitly names its verification level (e.g., `SHA-256 against source`, `SHA-256 against backup`, `Size + timestamp`). DockDrop never claims "Verified" unless bytes were actually verified.
4. **No Vendor Lock-In (Plain Mirror Format)**
   Backups on the SSD are standard filesystem trees. Even without DockDrop installed, you can plug the SSD into any Windows, macOS, or Linux machine and directly access all your files.

---

## On-Drive Vault Hierarchy

When DockDrop backs up to an SSD, it creates the following standard filesystem structure:

```
<DriveRoot>/
  └── DockDrop/                                 <-- Vault directory selected via SAF
      ├── drive.json                            <-- Drive identity & metadata
      ├── .dockdrop/                            <-- Internal state directory
      │   └── sets/
      │       └── <SetId>/
      │           ├── set.json                  <-- Set definition (name, sources)
      │           ├── HEAD                      <-- Text file containing active generation number (e.g., "1")
      │           ├── manifest_000001.ndjson    <-- Atomic append-only manifest generation
      │           └── journal.db                <-- Resume journal for crash recovery
      └── <SetName>/                            <-- Plain mirror root (e.g. "Pixel 8 Photos")
          ├── Camera/                           <-- Source folder label
          │   ├── 2026_01_01_photo.jpg          <-- Plain unencrypted file
          │   └── 2026_01_02_video.mp4
          ├── Documents/                        <-- Source folder label
          │   └── taxes.pdf
          └── _deleted/                         <-- Preserved files deleted from phone
              └── 2026-10-15/
                  └── old_memo.pdf
```

---

## Restore without DockDrop

If your phone is lost, broken, or you simply want to access your backed-up files from a computer, **you do not need DockDrop to recover your files**.

### 1. Direct File Copy
Your files are stored as normal files in `<Vault>/<SetName>/<SourceLabel>/`.
- Simply connect the SSD to any computer (Windows, Mac, Linux, Chromebook).
- Open the drive in your file manager (File Explorer, Finder, Nautilus).
- Drag and drop or copy the folders (`Camera`, `Documents`, etc.) directly to your machine or a new device.

### 2. Recovering Deleted Files
Files that were deleted on the phone after an earlier backup are not purged automatically (unless set to Mirror retention with explicit confirmation). Instead, they are moved to:
```
<Vault>/<SetName>/_deleted/<YYYY-MM-DD>/<SourceLabel>/...
```
You can browse these folders to recover files you may have accidentally deleted from your phone.

### 3. Verifying File Integrity Using Standard CLI Tools
Each manifest generation is stored in `.dockdrop/sets/<SetId>/manifest_<generation>.ndjson`. Each line is an independent JSON record containing the file path and its SHA-256 hash:

```json
{"path":"Camera/IMG_2026.jpg","size":4194304,"modified":1729000000000,"sha256":"e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855","status":"ACTIVE"}
```

You can verify the hashes of your files on your computer using standard command-line tools without running any DockDrop software:

#### Linux / macOS
```bash
# Navigate to the Set folder
cd /Volumes/<DriveName>/DockDrop/<SetName>

# Compute the SHA-256 hash of a file
sha256sum "Camera/IMG_2026.jpg"

# Or on macOS:
shasum -a 256 "Camera/IMG_2026.jpg"
```

#### Windows (PowerShell)
```powershell
# Navigate to the Set folder
cd "D:\DockDrop\<SetName>"

# Verify the SHA-256 hash of a file
Get-FileHash -Algorithm SHA256 "Camera\IMG_2026.jpg"
```

The resulting hash string will match the `sha256` field in the manifest record.

---

## Restoring with the DockDrop App

When using DockDrop, restore is a first-class operation:

1. **The Hold (S12):** Restore all your backed-up files to their original phone locations with a single 1.2-second physical hold gesture.
2. **Selective Restore (S13 & S14):** Search, browse folder trees, filter by type (Photos, Videos, Documents, Deleted), select specific files, and preview conflict resolutions and storage requirements.
3. **Conflict Policies:**
   - **Keep both (Default):** Adds non-colliding numeric suffixes (e.g. `file (1).jpg`) without overwriting existing data.
   - **Skip:** Leaves phone copies untouched.
   - **Overwrite:** Replaces phone copies only when explicitly selected by the user.
4. **New-Phone Migration (S16):** Plug your SSD into a brand new phone on a fresh install. DockDrop automatically discovers existing backups on the drive and guides you through a 4-step wizard to map backup folders to new phone directories.
5. **Pre-Rename Verification & Media Rescan:** Restored files are verified against the manifest SHA-256 *before* renaming into place, and registered with Android's MediaStore so your photo gallery and music players update instantly.

---

## Architecture & Development

- **[Architecture Overview](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/ARCHITECTURE.md)**: System design, pipeline stages (Scan → Diff → Copy → Verify → Commit), and the 8 work domains.
- **[Contributing Guide](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/CONTRIBUTING.md)**: Setting up your environment, code standards, conventional commits, and pre-flight PR checklist.
- **[Code of Conduct](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/CODE_OF_CONDUCT.md)**: Community standards and enforcement guidelines (Contributor Covenant v2.1).
- **[Security Policy](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/SECURITY.md)**: Zero-network attack surface details and private vulnerability disclosure procedures.
- **[Support & Troubleshooting](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/SUPPORT.md)**: Hardware OTG tips, exFAT compatibility, and help channels.
- **[Governance](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/GOVERNANCE.md)**: Project roles and Architecture Decision Records (ADR) process.
- **[Standard Operating Procedures](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/docs/sop/)**: Release workflows, PR review checklists, and bug triage procedures.

---

## License

DockDrop is open-source software licensed under the [Apache License 2.0](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/LICENSE).

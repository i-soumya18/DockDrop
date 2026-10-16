# DockDrop Architecture

This document describes the high-level architecture of DockDrop for contributors and developers.

---

## 1. System Overview

DockDrop is designed as an offline-first, reactive pipeline for Android. It operates between two storage trees: the phone's internal storage (source folders) and an external USB-C drive (the Vault).

```
┌────────────────────────────────────────────────────────┐
│                   UI & Jetpack Compose                  │
│       (Observes StateFlows, Emits User Intents)        │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│               A2 — Trigger & Service Agent             │
│        (USB Lifecycle, DropService, Notifications)      │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                    A3 — Engine Agent                   │
│      ┌──────────────────────────────────────────┐      │
│      │ Scan ──► Diff ──► Copy ──► Verify ──► Commit │   │
│      └──────────────────────────────────────────┘      │
└──────────────┬──────────────────────────┬──────────────┘
               │                          │
┌──────────────▼──────────┐    ┌──────────▼──────────────┐
│  A1 — Storage Gateway   │    │ A4 — Manifest & Policy  │
│ (SAF Trees, Batch Cursor│    │ (Append-Only NDJSON,    │
│  Queries, Atomic Rename)│    │  HEAD Pointer, Room DB) │
└─────────────────────────┘    └─────────────────────────┘
```

---

## 2. The Eight Work Domains (AGENTS.md)

DockDrop strictly separates concerns across 8 narrow domains:

1. **A1 — Storage**: Manages all Android `Uri` trees, `DocumentsContract` batch queries, atomic renames, and free-space queries. Never touches UI or copy planning.
2. **A2 — Trigger & Service**: Listens for `USB_DEVICE_ATTACHED` and `StorageManager` volume callbacks. Hosts `DropService` (foreground service) and publishes immutable `DropState` flows.
3. **A3 — Engine**: Orchestrates the 5-step pipeline:
   - **Scan**: Enumerate source directories and compare with SSD index.
   - **Diff**: Determine new, modified, and deleted files.
   - **Copy**: Stream bytes via 1MB buffers with on-the-fly SHA-256 calculation to temporary files (`.part`).
   - **Verify**: Size match and hash check against the declared verification level.
   - **Commit**: Finalize atomic renames and commit the new manifest generation.
4. **A4 — Manifest & Policy**: Owns the append-only NDJSON format, generation incrementing, and the two-phase `HEAD` commit protocol.
5. **A5 — Restore**: Reads manifests, calculates restore plans, resolves conflicts (Keep Both / Skip / Overwrite), verifies bytes before renaming, and triggers Android MediaStore rescans.
6. **A6 — Crypto**: Provides transparent chunked streaming encryption (`AES-256-GCM`) with Argon2id/scrypt key derivation and Keystore-backed convenience unlock.
7. **A7 — UX & Motion**: Pure Jetpack Compose layer adhering to `DESIGN.md` design tokens, responsive typography, and reduced-motion modes.
8. **A8 — Quality & Release**: Enforces test matrices, fault injection (cable pull, process kill), and CI lint checks (zero-network rule).

---

## 3. On-Disk Vault Layout

Unencrypted backups adhere to a plain-mirror layout that requires zero specialized software to browse on any computer:

```
<DriveRoot>/
  └── DockDrop/                                 <-- Vault directory
      ├── drive.json                            <-- Drive identity & metadata
      ├── .dockdrop/                            <-- Internal metadata
      │   └── sets/<SetId>/
      │       ├── set.json                      <-- Set configuration
      │       ├── HEAD                          <-- Active generation number (e.g. "2")
      │       ├── manifest_000001.ndjson        <-- Historical generation
      │       ├── manifest_000002.ndjson        <-- Current active manifest
      │       └── journal.db                    <-- Crash-resilient resume journal
      └── <SetName>/                            <-- Plain mirror root
          ├── Camera/                           <-- Source folder label
          │   ├── photo_1.jpg
          │   └── video_1.mp4
          └── _deleted/                         <-- Kept deletions
              └── 2026-10-15/
                  └── old_memo.pdf
```

---

## 4. Two-Phase Manifest Commit Protocol

To guarantee crash resilience without database corruption over SAF:
1. Write the new generation `manifest_<gen+1>.ndjson.tmp`.
2. Compute and write `manifest_<gen+1>.sha256`.
3. Atomically rename to `manifest_<gen+1>.ndjson`.
4. Rewrite the tiny `HEAD` text file pointing to `<gen+1>`.
5. If interrupted at any point prior to step 4, the engine falls back to generation `<gen>` on next plug-in.

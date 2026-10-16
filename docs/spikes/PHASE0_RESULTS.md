# Phase 0 Spike Results — Samsung Galaxy A14 (`SM-A146B`)

> **Date:** 2026-10-16  
> **Tool:** DockDrop S22 Spike Lab (`:spikelab`, debug-only module)  
> **Raw Data Archives:** [phase0_raw_runs.csv](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/docs/spikes/phase0_raw_runs.csv) · [spikelab_state.json](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/docs/spikes/spikelab_state.json)  
> **Resulting ADRs:** [ADR-001](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/docs/adr/ADR-001-trigger-strategy.md) · [ADR-002](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/docs/adr/ADR-002-verification-level.md) · [ADR-003](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/docs/adr/ADR-003-foreground-service-type.md)

---

## 1. Device & Test Hardware Facts

| Field | Observed Value |
|---|---|
| **Phone Model Code** | `SM-A146B` (Samsung Galaxy A14 5G) |
| **Android Version** | `Android 15 (SDK 35)` |
| **One UI / SEP Version** | `One UI 7.0` (`ro.build.version.oneui = 70000`, `ro.build.version.sep = 160000`) |
| **Android Build Number** | `AP3A.240905.015.A2.A146BXXSFDZI2` |
| **SSD Brand & Model** | Hikvision 128 GB USB-C External SSD |
| **USB Controller Descriptors** | Vendor ID `0x152d` (`5421`, JMicron), Product ID `0x0580` (`1408`, Product Name `External`), Serial `123467`, Interfaces: `c=8/s=6/p=80` (BOT) and `c=8/s=6/p=98` (UASP) |
| **Volume UUID & Label** | `4E21-0000` (Label: `Ventoy`) |
| **Filesystem** | `exFAT` |
| **Cable / Adapter / Hub** | Direct USB-C to USB-C cable (USB 2.0 High-Speed bus on `SM-A146B`) |
| **Battery & Thermal Range** | `98% → 85%` (`33.8 °C → 35.9 °C`, `charging=false` during SSD host runs) |
| **Granted SSD Tree URI** | `content://com.android.externalstorage.documents/tree/4E21-0000%3ADockDrop` |

---

## 2. Trigger Matrix (`T1`–`T8`)

### 2.1 Observed Event Timeline on SSD Attach & Detach

| Timestamp | Event Fired | Delta from Attach | Process State | Source | Details |
|---|---|---|---|---|---|
| `13:37:46.470` | `RUNTIME_USB:USB_DEVICE_ATTACHED` | `+0 ms` | `FOREGROUND (100)` | Runtime `BroadcastReceiver` | `device=External` |
| `13:37:46.495` | `MANIFEST_RECEIVER:USB_DEVICE_ATTACHED` | `+25 ms` | `FOREGROUND (100)` | Manifest `SpikeUsbReceiver` | `vid=0x152d pid=0x580 if=[c=8/s=6/p=80,c=8/s=6/p=98]` |
| `13:37:46.754` | `ACTIVITY_INTENT:USB_DEVICE_ATTACHED` | `+284 ms` | `FOREGROUND (100)` | Manifest Activity Filter (`onNewIntent`) | `device=External vid=0x152d` |
| `13:38:00.110` | `STORAGE_VOLUME_CALLBACK:mounted` | **`+13,640 ms`** | `CACHED/RECENTS (400)` | `StorageManager.StorageVolumeCallback` | `uuid=4E21-0000 desc='Ventoy' removable=true` |
| `13:42:08.698` | `STORAGE_VOLUME_CALLBACK:ejecting` | *(unplug #1)* | `FOREGROUND (100)` | `StorageManager.StorageVolumeCallback` | `uuid=4E21-0000 desc='Ventoy' removable=true` |
| `13:42:08.922` | `RUNTIME_USB:USB_DEVICE_DETACHED` | `+224 ms` | `FOREGROUND (100)` | Runtime `BroadcastReceiver` | `device=External` |
| `13:42:08.924` | `MANIFEST_RECEIVER:USB_DEVICE_DETACHED` | `+226 ms` | `FOREGROUND (100)` | Manifest `SpikeUsbReceiver` | `vid=0x152d pid=0x580` |
| `13:42:17.141` | `RUNTIME_USB:USB_DEVICE_ATTACHED` | `+0 ms` *(replug)* | `FOREGROUND (100)` | Runtime `BroadcastReceiver` | `device=External` |
| `13:42:17.144` | `MANIFEST_RECEIVER:USB_DEVICE_ATTACHED` | `+3 ms` | `FOREGROUND (100)` | Manifest `SpikeUsbReceiver` | `vid=0x152d pid=0x580` |
| `13:42:17.276` | `ACTIVITY_INTENT:USB_DEVICE_ATTACHED` | `+135 ms` | `FOREGROUND (100)` | Manifest Activity Filter (`onNewIntent`) | `device=External vid=0x152d` |
| `13:42:31.759` | `STORAGE_VOLUME_CALLBACK:mounted` | **`+14,618 ms`** | `FOREGROUND (100)` | `StorageManager.StorageVolumeCallback` | `uuid=4E21-0000 desc='Ventoy' removable=true` |
| `14:21:38.124` | `STORAGE_VOLUME_CALLBACK:ejecting` | *(unplug #2)* | `FOREGROUND (100)` | `StorageManager.StorageVolumeCallback` | `uuid=4E21-0000 desc='Ventoy' removable=true` |
| `14:21:38.375` | `MANIFEST_RECEIVER:USB_DEVICE_DETACHED` | `+251 ms` | `FOREGROUND (100)` | Manifest `SpikeUsbReceiver` | `vid=0x152d pid=0x580` |
| `14:21:38.940` | `STORAGE_VOLUME_CALLBACK:unmounted` | `+816 ms` | `FOREGROUND (100)` | `StorageManager.StorageVolumeCallback` | `uuid=4E21-0000 desc='Ventoy' removable=true` |

### 2.2 Trigger Scenario Matrix (`T1`–`T8`)

| ID | Scenario | Observed Behavior on `SM-A146B` (Android 15 / One UI 7.0) | Supported in v1? |
|---|---|---|---|
| **T1** | App in **foreground** | `USB_DEVICE_ATTACHED` fires in `< 300 ms` (runtime + manifest receiver + Activity `onNewIntent`). `StorageVolumeCallback(mounted)` fires **13.6–14.6 s later** once `exFAT` mount completes. `ACTION_MEDIA_MOUNTED` (`file://`) is not delivered to third-party apps on Android 15. | **Yes (Primary)** |
| **T2** | App in **recents / cached** | Manifest `USB_DEVICE_ATTACHED` Activity/Receiver and `StorageVolumeCallback` (`IMPORTANCE_CACHED = 400`) fire reliably (`13:38:00.110`). | **Yes (Primary)** |
| **T3** | App **swiped away** (not force-stopped) | Manifest `<activity>` with `USB_DEVICE_ATTACHED` + `<usb-device class="8" />` filter cold-starts/wakes the app when `device_preferences` ("Always open DockDrop") is set in `UsbProfileGroupSettingsManager`. | **Yes (Primary)** |
| **T4** | App **force-stopped** in Settings | Android's `FLAG_EXCLUDE_STOPPED_PACKAGES` blocks manifest broadcast receivers for force-stopped apps, though the USB Activity chooser/default handler in `UsbProfileGroupSettingsManager` can still resolve Activity intents when unlocked. Fallback: manual launch or Quick Settings tile. | **Fallback (QS Tile / Manual)** |
| **T5** | **Screen locked / off** | `USB_DEVICE_ATTACHED` fires at the system level; manifest `SpikeUsbReceiver` posts the high-priority "SSD detected" notification visible on the lock screen / upon unlock. | **Yes (Notification)** |
| **T6** | **Reboot with SSD attached**, then unlock | `RECEIVE_BOOT_COMPLETED` fires after first unlock (`14:30:07.006`); querying `StorageManager.storageVolumes` immediately finds the already-mounted removable volume (`uuid=4E21-0000, state=mounted`). | **Yes (`BOOT_COMPLETED` + `storageVolumes` check)** |
| **T7** | **Direct USB-C vs hub** | Direct USB-C to USB-C cable negotiated USB Host mode (`power_role=source, data_role=host`) cleanly at ~35.9 MB/s without external power. | **Yes** |
| **T8** | **"Always open with DockDrop"** vs chooser & Samsung "My Files" | Verified via `dumpsys usb`: on One UI 7.0, **no built-in Samsung app (including Samsung My Files) registers a `USB_DEVICE_ATTACHED` Activity filter for `class=8` (Mass Storage)**. DockDrop is the sole matching Activity, so Android shows the single-app prompt with *"Always open DockDrop when External is connected"*. Once checked, `UsbProfileGroupSettingsManager` persists `user_package=in.soumyaranjansahoo.dockdrop` for `vid=5421, pid=1408, serial=123467`. Samsung My Files only posts a passive system notification after mount (`+14 s`) and never steals focus. | **Yes (Primary)** |

---

## 3. SAF Permission Persistence (`P1`–`P5`)

| Scenario | Persisted in `persistedUriPermissions`? | Post-Disruption Read/Write Verified? | Volume Identity | Observed Details |
|---|---|---|---|---|
| **P1 (3× Unplug/Replug Cycles)** | **`true`** (all 3 cycles: `13:41:15`, `13:42:42`, `13:42:46`) | **`true`** (subsequent `R1–R4` and `B1–B5` wrote/read >10 GB & >5,000 files without re-granting) | `4E21-0000` (`Ventoy`) | SAF tree URI `content://com.android.externalstorage.documents/tree/4E21-0000%3ADockDrop` survives repeated physical unplug/replug cycles intact. |
| **P2 (Reboot)** | **`true`** | **`true`** | `4E21-0000` | OS `uri_grants` database persists across reboot; volume mounts under identical exFAT Volume Serial Number `4E21-0000`. |
| **P3 (Force-Stop & Relaunch)** | **`true`** (`14:31:33.912`) | **`true`** (when SSD connected; throws `FileNotFoundException: No root for 4E21-0000` when unplugged) | `4E21-0000` | Force-stopping via `am force-stop` / Settings does **not** revoke `takePersistableUriPermission` grants. |
| **P4 (Cable Flip / Reconnect)** | **`true`** | **`true`** | `4E21-0000` | SAF authority and tree document ID (`4E21-0000:DockDrop`) depend only on the filesystem Volume Serial Number, not USB port/bus address (`dev/bus/usb/001/002`). |
| **P5 (Volume Label vs UUID)** | **`true`** | **`true`** | UUID=`4E21-0000` vs Label=`Ventoy` | Android `ExternalStorageProvider` keys the tree URI by the exFAT 32-bit Volume Serial Number (`4E21-0000`), while `StorageVolume.getDescription()` returns the label (`Ventoy`). Changing the label on a laptop without reformatting preserves `4E21-0000` and keeps the SAF grant valid. |

---

## 4. Rename & Filename Semantics (`R1`–`R4`)

Executed on `content://com.android.externalstorage.documents/tree/4E21-0000%3ADockDrop/.spikelab_rename_test`:

| Test ID | Scenario | Input Name | Accepted? | Actual Display Name | Provider Behavior | Elapsed | Details |
|---|---|---|---|---|---|---|---|
| **R1** | Create `x.part`, write 16 KB, rename to `x_final.bin` | `x.part → x_final.bin` | `true` | `x_final.bin` | **Atomic rename succeeded; `.part` gone** | `51 ms` | `partStillExists=false, finalUri=.../x_final.bin` |
| **R2** | Rename `incoming.part` onto existing `existing.bin` | `incoming.part → existing.bin` | `true` | **`existing (1).bin`** | **AUTO-SUFFIXED to `'existing (1).bin'` (original untouched!)** | `37 ms` | `existingContent='OLD_DATA'` |
| **R3** | `DocumentsContract.moveDocument(file, srcDir, dstDir)` | `move_src/movable.bin → move_dst/` | `true` | `movable.bin` | **SUPPORTED natively** | `52 ms` | `foundInDst=true, movedUri=.../move_dst/movable.bin` |
| **R4a** | Spaces in filename | `file with   multiple spaces.txt` | `true` | `file with   multiple spaces.txt` | **ACCEPTED AS-IS (`len=31`)** | `10 ms` | `exactMatch=true` |
| **R4b** | Odia & Hindi script | `ଓଡ଼ିଆ_हिन्दी_परीक्षा.txt` | `true` | `ଓଡ଼ିଆ_हिन्दी_परीक्षा.txt` | **ACCEPTED AS-IS (`len=24`)** | `18 ms` | `exactMatch=true` |
| **R4c** | Emoji in filename | `📸_DockDrop_🔒_test.txt` | `true` | `📸_DockDrop_🔒_test.txt` | **ACCEPTED AS-IS (`len=23`)** | `11 ms` | `exactMatch=true` |
| **R4d** | 200-character filename | `aaaa...aaaa.txt` (`200 chars`) | `true` | `aaaa...aaaa.txt` (`200 chars`) | **ACCEPTED AS-IS (`len=200`)** | `16 ms` | `exactMatch=true` |
| **R4e** | Trailing dots | `trailing_dots...` | `true` | **`trailing_dots..`** | **REWRITTEN by provider (`...` → `..`)** | `17 ms` | `exactMatch=false` |
| **R4f** | Colon & question mark (`:` and `?`) | `time_12:30_what?.txt` | `true` | **`time_12_30_what_.txt`** | **REWRITTEN by provider (`:` and `?` → `_`)** | `14 ms` | `exactMatch=false` |
| **R4g** | Other exFAT reserved (`* " < > \|`) | `bad_*_"_<_>_|.txt` | `true` | **`bad__________.txt`** | **REWRITTEN by provider (`* " < > \|` → `_`)** | `16 ms` | `exactMatch=false` |

### Engine Filename-Sanitization & Atomic-Rename Rules (Mandatory for Phase 1 A1/A3/A4)
1. **Delete-Before-Rename Rule (`R2`):** `ExternalStorageProvider` on Android 15 never overwrites an existing file during `DocumentsContract.renameDocument()`; instead, it silently renames `foo.jpg.part` to `foo (1).jpg` and leaves the old `foo.jpg` intact. Therefore, `StorageGateway.renameAtomic(partUri, finalName)` **must check if `finalName` already exists in the parent directory and delete the old target document immediately before calling `renameDocument()`**, then assert that the resulting `COLUMN_DISPLAY_NAME == finalName`.
2. **Native Move for `_deleted/` (`R3`):** `DocumentsContract.moveDocument()` works natively on exFAT in ~50 ms and should be used by A1/A4 when moving deleted files to `_deleted/<SetName>/<yyyy-MM-dd>/`.
3. **ExFAT Character Sanitization Rule (`R4e–R4g`):** Unicode scripts (Odia, Hindi), emoji, multiple spaces, and 200-character names pass through untouched. However, `ExternalStorageProvider` silently rewrites `[ : ? * " < > | ]` to `_` and strips trailing dots. The Engine must either normalize destination filenames with the same rule (`replace(Regex("[:?*\"<>|]"), "_")`) or record the provider's returned `COLUMN_DISPLAY_NAME` in the manifest so subsequent Drops match the on-SSD filename accurately.

---

## 5. Throughput & Listing Benchmarks (`B1`–`B5`)

### 5.1 Throughput Summary Table (Medians of 3 Runs, `1024 KB` Buffer, `charging=false`)

| Test ID | Workload | Runs | Median MB/s | Median files/s | Median Elapsed (s) | Temp Range (°C) | Peak Heap Δ (MB) | SHA-256 Verified |
|---|---|---|---|---|---|---|---|---|
| **B1** | **1 GB Single File Write** (`1,073,741,824 B`) | 3 | **`35.90 MB/s`** | `0.04 /s` | `28.52 s` | `34.0–34.0 °C` | `0.92 MB` (med) | `true` |
| **B2** | **2,000 × 100 KB Files Write** (`204,800,000 B`) | 3 | **`2.24 MB/s`** | **`22.97 /s`** | `87.08 s` | `33.8–34.0 °C` | `4.52 MB` (med) | `true` |
| **B3** | **Mixed Write** (`200 × 1–20 MB` + `2,000 × 100 KB` = `2.41 GB`, `2,200 files`) | 3 | **`14.61 MB/s`** | **`14.00 /s`** | `157.11 s` | `33.8–35.0 °C` | `1.39 MB` (med) | `true` |
| **B4-B1** | **Read-Back of B1 (1 GB)** — *Run 1 uncached physical USB vs Runs 2–3 VFS cache* | 3 | **`35.70 MB/s`** *(uncached)* / `448.53 MB/s` *(cached)* | `0.03 /s` *(uncached)* | `28.68 s` *(uncached)* / `2.28 s` *(cached)* | `35.2–35.3 °C` | `0.18 MB` | `true` |
| **B4-B2** | **Read-Back of B2 (2,000 × 100 KB)** | 3 | **`7.03 MB/s`** | **`72.04 /s`** | `27.76 s` | `35.5–35.5 °C` | `0.00 MB` (med) | `true` |
| **B4-B3** | **Read-Back of B3 (Mixed 2.41 GB, 2,200 files)** | 3 | **`23.10 MB/s`** | **`22.14 /s`** | `99.37 s` | `35.5–35.9 °C` | `0.70 MB` (med) | `true` |

### 5.2 Raw Throughput Runs CSV (`phase0_raw_runs.csv`)

```csv
test_id,run_number,iso_time,workload_label,buffer_kb,is_charging,total_bytes,file_count,elapsed_ms,mb_per_sec,files_per_sec,sha256_verified,battery_start_pct,battery_end_pct,temp_start_c,temp_end_c,heap_delta_mb
B1,1,2026-10-16 13:43:59.683,"1 GB Single Write",1024,false,1073741824,1,28336,36.138,0.04,true,98,98,34.0,34.0,0.92
B1,2,2026-10-16 13:44:28.919,"1 GB Single Write",1024,false,1073741824,1,28569,35.843,0.04,true,98,98,34.0,34.0,0.00
B1,3,2026-10-16 13:44:57.909,"1 GB Single Write",1024,false,1073741824,1,28524,35.900,0.04,true,98,98,34.0,34.0,4.03
B2,1,2026-10-16 13:46:42.808,"2,000 x 100 KB Write",1024,false,204800000,2000,67605,2.889,29.58,true,98,97,34.0,33.9,4.80
B2,2,2026-10-16 13:48:11.287,"2,000 x 100 KB Write",1024,false,204800000,2000,88095,2.217,22.70,true,97,97,33.9,33.9,0.00
B2,3,2026-10-16 13:49:38.669,"2,000 x 100 KB Write",1024,false,204800000,2000,87076,2.243,22.97,true,97,96,33.9,33.8,4.52
B3,1,2026-10-16 13:52:42.355,"Mixed (200x1-20MB + 2000x100KB)",1024,false,2406809600,2200,155468,14.764,14.15,true,96,95,33.8,34.0,5.45
B3,2,2026-10-16 13:55:20.252,"Mixed (200x1-20MB + 2000x100KB)",1024,false,2406809600,2200,157107,14.610,14.00,true,95,94,34.0,34.9,1.39
B3,3,2026-10-16 13:57:59.330,"Mixed (200x1-20MB + 2000x100KB)",1024,false,2406809600,2200,157834,14.543,13.94,true,94,93,34.9,35.0,0.00
B4-B1,1,2026-10-16 14:01:36.397,"Read-back of B1",1024,false,1073741824,1,28683,35.701,0.03,true,92,91,35.2,35.3,0.18
B4-B1,2,2026-10-16 14:01:38.742,"Read-back of B1",1024,false,1073741824,1,2283,448.533,0.44,true,91,91,35.3,35.3,1.13
B4-B1,3,2026-10-16 14:01:40.996,"Read-back of B1",1024,false,1073741824,1,2236,457.961,0.45,true,91,91,35.3,35.3,0.00
B4-B2,1,2026-10-16 14:04:31.811,"Read-back of B2",1024,false,204800000,2000,38117,5.124,52.47,true,90,90,35.5,35.5,2.88
B4-B2,2,2026-10-16 14:04:59.666,"Read-back of B2",1024,false,204800000,2000,27764,7.035,72.04,true,90,90,35.5,35.5,0.00
B4-B2,3,2026-10-16 14:05:26.280,"Read-back of B2",1024,false,204800000,2000,26497,7.371,75.48,true,90,90,35.5,35.5,0.00
B4-B3,1,2026-10-16 14:10:27.694,"Read-back of B3",1024,false,2406809600,2200,99680,23.027,22.07,true,89,88,35.7,35.5,0.94
B4-B3,2,2026-10-16 14:12:07.196,"Read-back of B3",1024,false,2406809600,2200,99366,23.100,22.14,true,88,88,35.5,35.5,0.00
B4-B3,3,2026-10-16 14:13:29.287,"Read-back of B3",1024,false,2406809600,2200,81963,28.004,26.84,true,88,87,35.5,35.9,0.70
```

### 5.3 Chosen Buffer Size, Listing Budget (`B5`), and 20 GB First-Drop Extrapolation

- **Chosen Streaming Buffer Size (A3 Engine):** **`1 MB` (`1,048,576` bytes)**
  - Saturates the Galaxy A14's USB 2.0 High-Speed bus at **`35.90 MB/s`** on sequential writes (`B1`) and **`35.70 MB/s`** on uncached sequential reads (`B4-B1` Run 1) while keeping heap delta under `1–5 MB`.
- **Directory Listing Benchmark (`B5`: 5,000 Files):**
  - `DocumentsContract.buildChildDocumentsUriUsingTree` batch cursor query:
    - Run #1: **`277 ms`** (`55.4 ms / 1,000 files`, heap Δ `0.81 MB`)
    - Run #2: **`285 ms`** (`57.0 ms / 1,000 files`, heap Δ `0.75 MB`)
    - Run #3: **`286 ms`** (`57.2 ms / 1,000 files`, heap Δ `0.00 MB`)
    - **Median:** **`285 ms` for 5,000 files** (`57.0 ms / 1,000 files`).
  - **A1 Storage Agent Completion-Bar Budget for 5,000 files:** **`≤ 1,500 ms` (`1.5 s`)** via batch cursor queries (providing ~5× headroom over the `285 ms` single-directory median to accommodate multi-folder recursion). Per-file `DocumentFile` is banned.
- **20 GB First-Drop Time Extrapolation (ADR-002 Input):**
  - Using the realistic mixed workload (`B3` write median **`14.61 MB/s`**, `B4-B3` read-back median **`23.10 MB/s`**):
    - **Copy + SHA-256 on copy (20 GB):** `20,480 MB / 14.61 MB/s` = **`23.4 minutes`**
    - **Full destination re-read of 20 GB:** `20,480 MB / 23.10 MB/s` = **`14.8 minutes`**
    - **Total 20 GB First Drop with Full Destination Re-Read:** **`38.1 minutes`**
  - Because **`38.1 minutes ≤ 45.0 minutes`** (the confirmed threshold), **ADR-002** defaults to **"size + SHA-256 on copy + re-read of copied files"**.

---

## 6. Grantability Matrix (`G1`–`G7`)

| ID | Target Folder | Status on `SM-A146B` (Android 15) | Can List? | Can Read Sample? | Persisted Tree URI | Product Impact (S05 Wizard) |
|---|---|---|---|---|---|---|
| **G1** | `DCIM` | **GRANTED** | `true` (`9` items) | `true` | `content://.../tree/primary%3ADCIM` | Pre-checked suggestion card works at `DCIM` root or `DCIM/Camera`. |
| **G2** | `Pictures` | **GRANTED** | `true` (`50` items) | `true` | `content://.../tree/primary%3APictures` | Pre-checked suggestion card works directly. |
| **G3** | `Documents` | **GRANTED** | `true` (`3` items) | `true` | `content://.../tree/primary%3ADocuments` | Pre-checked suggestion card works directly. |
| **G4** | `Download` (root) | **BLOCKED_BY_PICKER** | `false` | `false` | `—` | OS picker greys out button (*"Can't use this folder to protect your privacy"*). S05 must show the planned sub-folder helper copy. |
| **G5** | `Download` sub-folder | **GRANTED** | `true` (`3` items) | `true` | `content://.../tree/...` | Selecting any sub-folder inside `Download` succeeds. |
| **G6** | `Android/media/com.whatsapp/WhatsApp/Media` | **GRANTED** | `true` (`18` items) | `true` | `content://.../tree/primary%3AAndroid%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia` | **Grantable on Android 15 / One UI 7.0!** `Android/media` is not blocked like `Android/data`. |
| **G7** | SSD Vault (`4E21-0000:DockDrop`) | **GRANTED** | `true` | `true` | `content://.../tree/4E21-0000%3ADockDrop` | Vault folder on external exFAT SSD is grantable with full read/write persistence. |

---

## 7. Foreground Service Lab (`F1`–`F4`)

| Test ID | Service Type | Observed Result on `SM-A146B` (Android 15, `targetSdk = 35`) |
|---|---|---|
| **F1 (`connectedDevice` Prerequisite Check)** | `connectedDevice` | Calling `startForeground(..., FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)` without an active `UsbPermissionsManager` session grant throws **`SecurityException: Starting FGS with type connectedDevice ... targetSDK=35 requires permissions: all of [FOREGROUND_SERVICE_CONNECTED_DEVICE] and any of [..., USB Device, USB Accessory]`**. Furthermore, `dumpsys usb` showed that `UsbPermissionsManager` session grants are cleared on unplug unless the app is launched via the `USB_DEVICE_ATTACHED` Activity intent or calls `UsbManager.requestPermission()` first. |
| **F2 (`dataSync` User-Tap Run)** | `dataSync` | Starts cleanly from any user tap (`S04` confirm, `S03` button, or notification action) with zero extra USB permission dialogs. During the ~45-minute continuous benchmark session (`13:37` to `14:21`), `dumpsys activity exit-info` recorded **zero** process kills or throttling. |
| **F3 (`onTimeout()` Forced Short Timeout)** | `dataSync` | Tested twice (`14:18:49` and `14:19:43`). `onTimeout()` persisted the JSON checkpoint to internal storage in **`4–7 ms`**, posted the *"Backup paused by system timeout — Tap to Continue"* notification, and called `stopSelf()` cleanly with no crash or ANR. |
| **F4 (Screen-Off Survival & One UI Battery Policy)** | `dataSync` | One UI 7.0 allows an active foreground service with ongoing USB/SAF I/O and notification to continue running while the screen is off, provided the user has not placed the app in Samsung's *Deep sleeping apps* list. S02 (*Keep DockDrop running*) remains important for long screen-off runs. |

---

## 8. Surprises and Scope Changes

1. **Surprise #1 — 14-Second Delay Between `USB_DEVICE_ATTACHED` and `StorageVolumeCallback(mounted)` (`T1`):**
   - `USB_DEVICE_ATTACHED` arrives within **~250 ms** of plugging in the SSD, but Android's `vold` / `exFAT` check takes **13.6 to 14.6 seconds** before the volume transitions to `mounted` and becomes accessible via SAF (`ExternalStorageProvider`).
   - **Impact on Phase 1/2 (A2 & A7):** If DockDrop is woken by `USB_DEVICE_ATTACHED`, the UI/service must not immediately fail with `VolumeGone` when probing the Vault URI. Instead, it should show `"Checking your SSD…"` (or wait on `StorageManager.StorageVolumeCallback` for up to 20 seconds until `volume.state == Environment.MEDIA_MOUNTED`) before reading `drive.json`.
2. **Surprise #2 — `DocumentsContract.renameDocument()` Auto-Suffixes Instead of Overwriting (`R2`):**
   - Renaming `incoming.part` to `existing.bin` when `existing.bin` already exists **does not overwrite `existing.bin` and does not throw an exception** — `ExternalStorageProvider` silently creates `existing (1).bin` and leaves `existing.bin` with stale content.
   - **Impact on Phase 1 (A1 & A3 & A4):** `StorageGateway.renameAtomic()` **must delete any existing target file before calling `renameDocument()`**, and verify the resulting `COLUMN_DISPLAY_NAME`.
3. **Surprise #3 — Linux Kernel VFS Page Cache Masks Immediate Re-Read Speed (`B4-B1` vs `B4-B3`):**
   - Re-reading a 1 GB file immediately after reading/writing it returned at **448.53 MB/s** (`2.28 s`) from the phone's RAM page cache, whereas an uncached physical read from the SSD is **35.70 MB/s** (`28.68 s`), and a 2.41 GB mixed read-back (which overflows the page cache) is **23.10 MB/s**.
   - **Impact on Phase 1 (A3 Engine — Rule 2):** To ensure destination re-read verification tests the SSD rather than RAM cache to the maximum extent possible without root, the Engine's `Verifying` stage must re-read files **after** the entire Set copy pass completes (in FIFO order, so earlier files have been evicted by later writes), never immediately after each individual file write.
4. **Surprise #4 — `StorageManager.getStorageVolume(Uri)` Rejects SAF Tree URIs (`P1`):**
   - `StorageManager.getStorageVolume(Uri)` only accepts `MediaStore` URIs and throws `IllegalArgumentException: Missing volume name` on `com.android.externalstorage.documents` URIs.
   - **Impact on Phase 1 (A1 Storage):** Extract the volume UUID prefix from `DocumentsContract.getTreeDocumentId(treeUri).substringBefore(':')` (e.g. `4E21-0000`) and match it against `StorageManager.storageVolumes`.
5. **Surprise #5 — `VolumeGone` vs `PermissionLost` Distinction (`P3`):**
   - When the SSD is unplugged, `contentResolver.persistedUriPermissions` still contains the tree URI (`true`), and querying `ExternalStorageProvider` throws `FileNotFoundException: No root for 4E21-0000` (not `SecurityException`). A1's `StorageGateway` must map `FileNotFoundException("No root for ...")` to `StorageError.VolumeGone` and `SecurityException` to `StorageError.PermissionLost`.

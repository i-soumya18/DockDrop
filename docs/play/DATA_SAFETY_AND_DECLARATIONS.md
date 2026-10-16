# Google Play Data Safety & Console Declarations

This document contains verbatim questions and answers for Google Play Console submission for **DockDrop** (`in.soumyaranjansahoo.dockdrop`).

---

## 1. Google Play Data Safety Section

### Data collection and security
- **Does your app collect or share any of the required user data types?**
  - **Answer:** **No**
  - *Explanation:* The application is fully offline and has zero network permissions (`android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE` are completely absent from all manifests and verified by CI build gate `verifyZeroNetworkPermissions`).

- **Is all of the user data collected by your app encrypted in transit?**
  - **Answer:** **Not applicable** (No data is transmitted off the device).

- **Do you provide a way for users to request that their data is deleted?**
  - **Answer:** **Not applicable** (No accounts, no remote database).

- **Does your app use Advertising ID (AD_ID)?**
  - **Answer:** **No**

### Google Play Data Safety Badge
As a result of these answers, the Google Play Store listing displays:
- **No data collected:** The developer says this app doesn't collect user data.
- **No data shared with third parties:** The developer says this app doesn't share user data with other companies or organizations.
- **Security practices:** Data is not transmitted over a network.

---

## 2. Foreground Service Permission Declaration (`dataSync`)

### Declared Service Type
- **Service Name:** `in.soumyaranjansahoo.dockdrop.core.service.DropService`
- **Foreground Service Type:** `dataSync` (`android.permission.FOREGROUND_SERVICE_DATA_SYNC`)
- **Reference ADR:** [ADR-003: Foreground Service Type & Lifecycle Policy](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/docs/adr/ADR-003-foreground-service-type.md)

### Justification Text for Google Play Reviewer
```text
DockDrop is an offline phone backup and restore utility that copies and byte-verifies user-selected photos, videos, and documents to a user-attached USB-C external SSD via Android's Storage Access Framework.

When the user explicitly taps "Back up now", "Start backup", or "Hold to restore", DockDrop starts DropService with foregroundServiceType="dataSync" to stream multi-gigabyte files and perform SHA-256 hash calculations and destination re-read verification on the external storage volume.

Because multi-gigabyte backups typically take 5 to 40 minutes, running as a user-initiated dataSync foreground service with an ongoing progress notification is necessary to prevent Android from prematurely killing the process when the user locks the screen or switches applications. Without this service, multi-gigabyte transfers would terminate midway, leaving incomplete files on the external SSD.

The foreground service is initiated exclusively by direct user interaction, exhibits a persistent notification with pause/stop actions, and cleanly saves its journal checkpoint and halts if Service.onTimeout() occurs.
```

### Video Demonstration Outline for Play Console
1. **Device:** Samsung Galaxy A14 5G (`SM-A146B`), Android 15.
2. **Step 1:** Plug in USB-C SSD. DockDrop detects drive and shows "Back up now".
3. **Step 2:** User taps "Back up now". `DropService` starts with `dataSync` type.
4. **Step 3:** Pull down notification shade to show ongoing foreground notification ("Backing up Camera — 34% — Pause / Stop").
5. **Step 4:** Turn off/lock screen for 10 seconds.
6. **Step 5:** Unlock screen. Backup continues seamlessly to 100% and displays the Sealed Receipt screen.

---

## 3. Storage Permissions Policy Compliance

- **No All Files Access (`MANAGE_EXTERNAL_STORAGE`):**
  - DockDrop adheres strictly to Google Play policy and `CLAUDE.md Rule 7`: **No `MANAGE_EXTERNAL_STORAGE` in v1**.
  - All file access is obtained strictly through standard Android Storage Access Framework (`ACTION_OPEN_DOCUMENT_TREE`) user picks.

---

## 4. Target SDK & Platform Compliance

- **Target SDK:** `35` (Android 15)
- **Min SDK:** `26` (Android 8.0)
- **Compile SDK:** `35`
- **Compliance:** Full compliance with Android 15 16 KB page sizes, edge-to-edge rendering, and foreground service lifecycle requirements.

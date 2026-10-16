# ADR-003: Foreground Service Type & Lifecycle Policy

- **Status:** Accepted
- **Date:** 2026-10-16
- **Deciders:** DockDrop Team (A2 Trigger & Service Agent)
- **Evidence:** [PHASE0_RESULTS.md](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/docs/spikes/PHASE0_RESULTS.md) §7 (`F1`–`F4`)

---

## Context

On Android 15 (`targetSdk = 35`), every foreground service must declare a specific `foregroundServiceType` and satisfy Android's runtime prerequisites at the moment `Service.startForeground()` is called:

1. **`connectedDevice` (`FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE`):**
   - Has no 6-hour daily timeout limit.
   - Requires the app to hold one of `{BLUETOOTH_*, NFC, UWB_RANGING, USB Device, USB Accessory}` runtime grants at the time `startForeground()` is called.
2. **`dataSync` (`FOREGROUND_SERVICE_TYPE_DATA_SYNC`):**
   - Requires no extra runtime hardware permission prompt when started from a user interaction.
   - Subject to a 6-hour limit per 24-hour window in background (resetting whenever the app comes to the foreground), after which `Service.onTimeout(startId, fgsType)` fires and the service must stop within a few seconds.

### Key Findings from Phase 0 (`F1`–`F4`)

1. **`F1` (`connectedDevice` throws `SecurityException` unless `UsbPermissionsManager` holds an active USB Device grant):**
   - On the Galaxy A14 (`SM-A146B`, Android 15), calling `startForeground(..., FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)` without an active `UsbPermissionsManager` grant threw:
     ```
     java.lang.SecurityException: Starting FGS with type connectedDevice
     callerApp=ProcessRecord{... in.soumyaranjansahoo.dockdrop ...} targetSDK=35
     requires permissions: all of [android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE]
     any of [BLUETOOTH_*, CHANGE_NETWORK_STATE, ..., USB Device, USB Accessory]
     ```
   - Crucially, `dumpsys usb` showed that SAF tree permissions (`content://com.android.externalstorage.documents/tree/4E21-0000%3ADockDrop`) do **not** grant a `UsbPermissionsManager` `USB Device` permission.
   - Furthermore, if a user triggers a Drop from the Home screen (`S03`), Quick Settings tile (`S23`), or a `StorageVolumeCallback` notification without going through the `USB_DEVICE_ATTACHED` Activity intent, `UsbManager.hasPermission(usbDevice)` is `false` unless we force a redundant system dialog via `UsbManager.requestPermission()`.
2. **`F2` & `F3` (`dataSync` + `onTimeout()` checkpointing is fast, seamless, and well within quota):**
   - A full 20 GB first Drop with destination re-read takes **`~38.1 minutes`** ([ADR-002](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/docs/adr/ADR-002-verification-level.md)), which is **~10.5%** of the 6-hour (`360-minute`) `dataSync` quota.
   - In `F3`, our `onTimeout(startId, fgsType)` handler persisted the checkpoint state to internal storage in **`4–7 ms`**, posted the *"Continue backup"* notification, and called `stopSelf()` cleanly.
3. **`F4` (One UI 7.0 background survival):**
   - During 45+ minutes of continuous I/O on the A14, One UI 7.0 never killed or throttled the service (`dumpsys activity exit-info` had zero exit entries). S02 (*Keep DockDrop running*) will still guide users to exempt DockDrop from battery restrictions / Samsung *Sleeping apps* for long screen-off backups.

---

## Decision

1. **Primary Foreground Service Type:** **`dataSync` (`ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC`)**.
   - Declared in `AndroidManifest.xml` with `android.permission.FOREGROUND_SERVICE` and `android.permission.FOREGROUND_SERVICE_DATA_SYNC`.
   - **Opportunistic `connectedDevice` upgrade (optional):** If `usbManager.deviceList.values.any { usbManager.hasPermission(it) }` is already `true` (because the app was launched via the `USB_DEVICE_ATTACHED` Activity intent with *"Always open DockDrop"*), the service may pass `FOREGROUND_SERVICE_TYPE_DATA_SYNC or FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE`, falling back strictly to `FOREGROUND_SERVICE_TYPE_DATA_SYNC` whenever `hasPermission` is `false`. It must **never** block a backup on a `UsbManager.requestPermission()` dialog.
2. **User-Interaction Start Rule:**
   - `DropService` is started **only** from a direct user tap (`S04` "Start backup", `S03` "Back up now", notification `[Back up now]` / `[Resume]` action). Never auto-start from a broadcast receiver.
3. **Mandatory `onTimeout()` Pause-and-Continue Protocol (Handoff Rule 2):**
   - In `DropService.onTimeout(startId, fgsType)`:
     1. Immediately call `engine.pause(PauseReason.SystemTimeout)` (which flushes the working journal checkpoint in `< 50 ms` and cancels active I/O coroutines cleanly; any in-flight `.part` file is left for journal cleanup/resume).
     2. Post the high-importance `"Resume backup?"` notification (`SCREENS.md` §3 `Resumable` / `Paused` state) with a `[Resume]` action that starts a fresh user-initiated `DropService` run.
     3. Call `stopForeground(STOP_FOREGROUND_REMOVE)` and `stopSelf(startId)` within `< 500 ms`.

---

## Consequences

- **Positive:** Zero extra system USB permission popups when starting a backup from Home, notifications, or Quick Settings; zero risk of `SecurityException` on `startForeground()`; full compliance with Android 15 `targetSdk = 35` FGS rules and Play Console `dataSync` declarations.
- **Negative / Mitigation:** Requires declaring the `dataSync` foreground service type in Google Play Console (Phase 5, A8 Quality & Release Agent) with a video/explanation showing user-initiated folder backup to an external USB SSD.

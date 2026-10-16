# ADR-001: USB Attach & Volume Mount Trigger Strategy

- **Status:** Accepted
- **Date:** 2026-10-16
- **Deciders:** DockDrop Team (A2 Trigger & Service Agent)
- **Evidence:** [PHASE0_RESULTS.md](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/docs/spikes/PHASE0_RESULTS.md) §2 (`T1`–`T8`)

---

## Context

DockDrop's core promise is *"Plug in → one tap → done"* ([CLAUDE.md](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/CLAUDE.md) §2). On Android 15 (Samsung Galaxy A14 `SM-A146B`, One UI 7.0), we tested three candidate detection mechanisms when plugging in an external exFAT USB-C SSD (`Hikvision 128 GB`, `vid=0x152d`, `pid=0x0580`, interfaces `c=8/s=6/p=80` BOT and `c=8/s=6/p=98` UASP):

1. `UsbManager.ACTION_USB_DEVICE_ATTACHED` / `ACTION_USB_DEVICE_DETACHED` (manifest Activity + manifest `BroadcastReceiver` + runtime `BroadcastReceiver`, filtered with `<usb-device class="8" />`).
2. `StorageManager.StorageVolumeCallback` (`onStateChanged` for removable `StorageVolume`s).
3. `Intent.ACTION_MEDIA_MOUNTED` / `ACTION_MEDIA_UNMOUNTED` (`file://` data scheme).

### Key Findings from Phase 0 (`T1`–`T8`)

1. **`USB_DEVICE_ATTACHED` fires immediately (`< 300 ms`), while filesystem mount takes `13.6–14.6 seconds`:**
   - At `13:37:46.470` (and `13:42:17.141`), `USB_DEVICE_ATTACHED` was delivered to both our manifest receiver (`+3–25 ms`) and manifest Activity filter (`+135–284 ms`).
   - However, Android's `vold` / `exFAT` filesystem check did not finish mounting the volume (`uuid=4E21-0000`) until `13:38:00.110` (`+13.64 s`) and `13:42:31.759` (`+14.62 s`), when `StorageManager.StorageVolumeCallback` fired with `state = mounted`.
   - Any SAF query against `content://com.android.externalstorage.documents/tree/4E21-0000%3ADockDrop` during that 14-second window throws `FileNotFoundException: No root for 4E21-0000`.
2. **One UI 7.0 has zero built-in Activity competitors for `<usb-device class="8" />`:**
   - Inspecting `dumpsys usb` (`settings_manager`) confirmed that Samsung's built-in apps (`com.android.mtp`, `com.sec.android.easyMover.Agent`, `com.sec.android.gallery3d`) only register for `class=6` (PTP) or `class=255` (MTP). **Samsung "My Files" does not register a `USB_DEVICE_ATTACHED` Activity filter.**
   - Consequently, DockDrop is the sole Activity matching `<usb-device class="8" />`. On first plug-in (`T8`), Android shows the single-app prompt with the *"Always open DockDrop when External is connected"* checkbox. Once checked, `UsbProfileGroupSettingsManager` persists `user_package=in.soumyaranjansahoo.dockdrop` in `device_preferences`, automatically waking/launching DockDrop on subsequent plug-ins (`T1`, `T2`, `T3`, `T5`) without showing a chooser.
3. **`ACTION_MEDIA_MOUNTED` (`file://`) does not reach third-party apps on Android 15:**
   - Scoped storage suppresses raw `/mnt/media_rw/<uuid>` media broadcasts to non-system apps. `StorageManager.StorageVolumeCallback` is the only reliable volume-mount signal.

---

## Decision

We adopt a **two-stage primary trigger pipeline** (`USB_DEVICE_ATTACHED` wake + `StorageVolumeCallback` mount readiness) with three explicit fallbacks:

### 1. Primary Path (Supported in `T1`, `T2`, `T3`, `T5`, `T8`)
- **Stage A — Immediate Wake (`+0.2 s`):**
  - Declare a manifest `<usb-device class="8" />` filter on the Plug-In trampoline/sheet Activity (S04) and manifest `BroadcastReceiver`.
  - When `USB_DEVICE_ATTACHED` arrives, if the matching removable volume is not yet `mounted` in `StorageManager.storageVolumes`, display S04 (if interactive) or notification in a transitional *"Connecting to your SSD…"* pre-mount state while listening to `StorageManager.StorageVolumeCallback`.
- **Stage B — Volume Mount Ready (`+13–15 s`):**
  - As soon as `StorageVolumeCallback.onStateChanged` reports `Environment.MEDIA_MOUNTED` for the registered Dock UUID (e.g., `4E21-0000`), read `.dockdrop/drive.json` and transition to `DockDetected` → `AwaitingConfirm` ("Start backup" CTA on S04 / "Back up now" notification action).
  - **Never auto-start the foreground service or Engine without the user's explicit tap** ([CLAUDE.md](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/CLAUDE.md) §9 & [AGENTS.md](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/AGENTS.md) A2).

### 2. Supported Fallbacks (for `T4` Force-Stopped, `T6` Reboot with SSD Attached, or Declined Default Handler)
1. **Runtime `StorageManager.StorageVolumeCallback` + Initial `storageVolumes` Scan:**
   - Whenever the app process is alive (or woken by `BOOT_COMPLETED` in `T6`), scan `StorageManager.storageVolumes` on startup and register `StorageVolumeCallback` so an already-attached or newly-mounted SSD is immediately detected.
2. **Quick Settings Tile (S23):**
   - Provides a 1-tap system shade entry point (`Back up now`) that wakes the app, checks `StorageManager.storageVolumes`, and opens S04 even if the app was previously force-stopped (`T4`) or the user cleared the USB default handler.
3. **Manual Home CTA (S03):**
   - Opening DockDrop from the launcher icon always queries `StorageManager.storageVolumes` and enables the primary `"Back up now"` CTA if the Dock is mounted.

---

## Consequences

- **Positive:** Immediate visual feedback (`< 300 ms`) upon plugging in the cable, zero race conditions with `ExternalStorageProvider` during the 14-second `exFAT` mount window, and no conflict with Samsung's "My Files" app.
- **Negative / Mitigation:** If the user unplugs the SSD during the 14-second mount window (`USB_DEVICE_DETACHED` or `StorageVolumeCallback(unmounted/ejecting)`), the sheet/notification cleanly dismisses back to `NoDock`.

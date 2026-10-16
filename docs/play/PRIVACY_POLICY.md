# Privacy Policy for DockDrop

**Effective Date:** October 16, 2026  
**Application:** DockDrop ("the Application", package: `in.soumyaranjansahoo.dockdrop`)  
**Developer:** Soumya Ranjan Sahoo  

---

## 1. Summary: Offline by Design

DockDrop is built on a simple foundation: **your files belong to you, on hardware you own, and never leave your control.**

DockDrop has **zero network capabilities**:
- The application does not declare or request `android.permission.INTERNET`.
- The application does not declare or request `android.permission.ACCESS_NETWORK_STATE`.
- The application cannot communicate with any remote server, cloud provider, advertising network, or analytics service.

You can verify this at any time in Android system settings: **Settings → Apps → DockDrop → Permissions**. You will observe that network access is neither requested nor permitted.

---

## 2. Information We Do Not Collect

Because DockDrop has no internet connection or tracking code:
- **No Personal Data:** We do not collect names, email addresses, phone numbers, or account credentials.
- **No File Content or Metadata:** Your photos, videos, documents, folder names, and file hashes are never transmitted to us or any third party.
- **No Identifiers or Telemetry:** We do not collect device identifiers, advertising IDs, IP addresses, usage statistics, crash logs over the network, or analytics.
- **No Cookies or Tracking:** There are no trackers, third-party SDKs, or advertising libraries embedded in the application.

---

## 3. Information Handled Locally on Your Device

All operations performed by DockDrop happen strictly between your phone's internal storage and the external storage device (such as a USB-C SSD or flash drive) that you connect:

1. **Storage Access Framework (SAF):** When you choose folders to back up or restore, Android's Storage Access Framework grants DockDrop permission to read and write files strictly within those user-selected folders and your external drive.
2. **Local Database & Checkpoints:** DockDrop stores backup configurations, transfer progress journals, and backup receipts in a local SQLite database (`Room`) inside the application's private sandbox on your device.
3. **Encryption Keys:** If you enable optional per-Set encryption, your passphrase and encryption keys are processed entirely on your device in memory. Passphrases are never stored on disk.
4. **Diagnostic Logs:** If you use the "Export log" feature in Settings, a plain-text summary of local transfer history and device OS version is saved only to a location you choose via the system file picker. DockDrop cannot transmit this file.

---

## 4. Permissions Requested and Their Purpose

DockDrop requests only the minimum local hardware and service permissions required to perform offline backups:

- **`android.permission.POST_NOTIFICATIONS`** *(Android 13+)*: Used to show persistent backup and restore progress notifications, and to alert you when a backup is sealed or needs your attention.
- **`android.permission.FOREGROUND_SERVICE` & `android.permission.FOREGROUND_SERVICE_DATA_SYNC`**: Used to keep the local file transfer active when your screen turns off or when you switch apps, preventing corrupted or incomplete transfers to your external SSD.
- **`android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`** *(Optional)*: Guided in setup so Android battery management does not prematurely terminate multi-gigabyte backup jobs while the phone is locked.
- **`android.permission.WAKE_LOCK`**: Keeps the CPU awake during active file transfers so that the backup or restore does not halt when the phone's screen locks.
- **`android.permission.VIBRATE`**: Used to provide subtle haptic feedback for key physical events (SSD plugged in, backup sealed, attention needed).

DockDrop **never** requests `MANAGE_EXTERNAL_STORAGE` ("All files access").

---

## 5. Third-Party Services and Links

DockDrop contains no third-party analytics, advertisements, or cloud integrations.

---

## 6. Children's Privacy

Because DockDrop collects no personal information whatsoever from any user, it does not knowingly collect information from children under any jurisdiction.

---

## 7. Changes to This Privacy Policy

If this privacy policy is updated, the revised document will be posted in our official repository and store listing. Because DockDrop is designed to remain an offline-first tool, its core offline commitment will not change.

---

## 8. Contact

If you have questions or feedback regarding this policy or DockDrop, please contact:
- Developer: Soumya Ranjan Sahoo
- Repository: [DockDrop on GitHub](https://github.com/soumya-ranjan-sahoo/DockDrop)

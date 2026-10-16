# Getting Support for DockDrop

Welcome to the DockDrop support guide! We want your offline backup experience to be fast, reliable, and smooth.

---

## 1. Where to Get Help

| Question Type | Recommended Channel |
|---|---|
| **General Questions & How-To** | [GitHub Discussions](https://github.com/i-soumya18/DockDrop/discussions) (Q&A category) |
| **Bug Reports & Crashes** | [GitHub Issues](https://github.com/i-soumya18/DockDrop/issues/new?template=bug_report.yml) |
| **Feature Ideas & Feedback** | [GitHub Discussions](https://github.com/i-soumya18/DockDrop/discussions) (Ideas category) |
| **Security Disclosures** | Follow instructions in [SECURITY.md](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/SECURITY.md) |

---

## 2. Common Hardware & Storage Troubleshooting

Because DockDrop interacts directly with physical USB drives and Android's Storage Access Framework (SAF), most common issues relate to physical drive power, cables, or filesystem format:

### A. SSD Is Not Detected When Plugged In
1. **OTG / Cable Quality**: Ensure your USB-C cable supports data transfer, not charging only. Passive adapters can occasionally drop connection under sustained transfer loads.
2. **Drive Power Requirements**: High-performance NVMe SSDs (especially in DIY enclosures) may draw more current (> 1.5A) than your phone's USB port can deliver. If the drive disconnects mid-transfer or fails to mount, test with an externally powered USB-C hub or a lower-power SATA/exFAT portable drive.
3. **Android Storage Notification**: Ensure Android shows the "USB drive attached" system notification in the notification shade before opening DockDrop.

### B. Supported Filesystem Formats
- **exFAT**: Highly recommended for compatibility across Android, Windows, macOS, and Linux, and supports files larger than 4GB.
- **FAT32**: Supported, but limited to individual files under 4GB (large 4K video recordings will fail if > 4GB).
- **NTFS**: Not natively supported by Android's internal storage manager unless your OEM firmware includes third-party drivers.

### C. Folder Permission Prompts (SAF)
- Android requires granting directory access through the system file picker.
- If you disconnect and reconnect your drive, Android retains permission across unplug/replug events for registered drives. If you reformat the SSD or change its volume UUID, you may need to re-select the Vault folder.

### D. Long-Running Transfers & Android 15 Service Limits
- On Android 14/15, background data sync services are allocated up to 6 hours within a 24-hour period before the OS requests the service to stop.
- For multi-hundred gigabyte initial backups, keep DockDrop open in the foreground or plug the phone into a charger/dock with passthrough power.

---

## 3. Submitting a Good Bug Report

Before opening a bug report:
1. Search existing [Issues](https://github.com/i-soumya18/DockDrop/issues) to verify if the issue has already been reported.
2. Provide your phone model (e.g. Samsung Galaxy A14, Google Pixel 8), Android OS version, and SSD details (model, capacity, filesystem).
3. If relevant, export the crash log or error screen text from the DockDrop Settings screen.

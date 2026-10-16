# SOP: Release Process

This document defines the Standard Operating Procedure (SOP) for preparing, validating, and releasing new versions of DockDrop.

---

## 1. Versioning Standard

DockDrop adheres to [Semantic Versioning 2.0.0](https://semver.org/):
- **MAJOR** (`X.0.0`): Incompatible manifest or on-disk format changes requiring automated migration tools.
- **MINOR** (`1.X.0`): New user features, UI additions, or new platform API adaptations (e.g. Android 16 support).
- **PATCH** (`1.0.X`): Bug fixes, crash resolutions, performance optimizations, and documentation updates.

---

## 2. Pre-Release QA Verification Checklist

Before tagging any release, the release coordinator must complete and document the following physical checks:

### Mandatory Automated Checks
- [ ] `./gradlew check` passes cleanly (all unit tests + `verifyZeroNetworkPermissions`).
- [ ] No banned permissions in source or merged manifests (`INTERNET`, `ACCESS_NETWORK_STATE`).
- [ ] `./gradlew assembleRelease` compiles with R8/ProGuard optimizations enabled without build warnings.

### Mandatory Hardware Checks (Reference Device: Galaxy A14 / Android 15)
- [ ] **Fresh Run**: Install cleanly, plug in SSD, create a Set, and execute a Drop. Verify "Sealed" receipt.
- [ ] **Second Run (Diff Check)**: Trigger a Drop with unchanged data; verify completion in seconds with 0 bytes transferred.
- [ ] **Pull-the-Cable Test**: Unplug the USB-C cable during an active copy at ~50%. Replug, verify clean state recovery, and verify resumption completes with zero corrupted `.part` files.
- [ ] **Restore Check**: Perform "Hold to Restore" on a test directory; verify restored files match SHA-256 and appear in the Android photo gallery via MediaStore rescan.
- [ ] **Screen Lock Test**: Start a Drop, turn off screen / lock phone, and verify foreground service completes without being killed by OEM battery optimizations.

---

## 3. Release Execution Steps

1. **Update Version in Gradle**:
   - In `app/build.gradle.kts`:
     - Increment `versionCode` (integer, e.g. `2`).
     - Update `versionName` (string, e.g. `"1.1.0"`).
2. **Update Release Notes**:
   - Add release notes entry to `CHANGELOG.md` or `docs/play/RELEASE_NOTES.txt`.
3. **Commit & Tag**:
   ```bash
   git commit -am "chore(release): prepare release v1.1.0"
   git tag -a v1.1.0 -m "Release v1.1.0"
   git push origin main --tags
   ```
4. **Build Release Artifacts**:
   ```bash
   ./gradlew bundleRelease assembleRelease
   ```
   Artifacts:
   - Android App Bundle (AAB): `app/build/outputs/bundle/release/app-release.aab`
   - APK: `app/build/outputs/apk/release/app-release-unsigned.apk`
5. **Publish to GitHub Releases**:
   - Create a new release referencing tag `v1.1.0`.
   - Attach the release APK and SHA-256 checksums.
   - Publish detailed release notes.
6. **Deploy to Google Play Console**:
   - Upload the signed `.aab` to Closed Testing / Production tracks.
   - Verify Data Safety declarations ("No data collected").

# ADR-005: `FLAG_SECURE` Scope on Encryption Setup (`S18`) and Unlock (`S19`)

- **Status:** Accepted
- **Date:** 2026-10-16
- **Phase:** Phase 4 (Policies & Encryption)
- **Owners:** A6 (Crypto Agent), A7 (UX & Motion Agent)

## Context

`SCREENS.md` specifies `WindowManager.LayoutParams.FLAG_SECURE` on `S18` (Encryption Setup) and `S19` (Unlock). Specifically:
- `S18` step (a) enters and confirms the passphrase.
- `S18` step (b) displays the 32-character Recovery Key (`7QKM-2D9F-...`) with explicit `"Copy"` and `"Save as file"` actions, followed by a 4-character verification gate.
- `S19` enters the passphrase or recovery key to unlock an encrypted Set.

We must decide whether `FLAG_SECURE` stays active across all steps of `S18` (including the Recovery Key step) and `S19`, and how it is cleaned up when navigating away.

## Decision

1. **Scoped `DisposableEffect` on `S18` and `S19` only:**
   - A reusable `@Composable fun SecureWindowEffect()` sets `activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)` on composition entry and calls `activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)` in `onDispose`.
   - This applies to the entirety of `S18` (Step A Passphrase, Step B Recovery Key & Gate, Step C Confirmation) and `S19` (Unlock Sheet).
   - Non-sensitive screens (`Home`, `Drop`, `Receipt`, `RestoreHub`) remain screenshot-capable so users can share or capture Receipts (`S10`) and progress.
2. **Recovery Key Export under `FLAG_SECURE`:**
   - Keeping `FLAG_SECURE` enabled during `S18` Step B prevents malicious background screen-recording or automatic cloud photo-sync apps (e.g., Google Photos / OneDrive backing up the `Screenshots/` folder) from silently exfiltrating the user's Recovery Key to the cloud (which would violate DockDrop's offline privacy promise).
   - Instead of taking a screenshot, `S18` Step B provides first-class `"Save as file"` (`ActivityResultContracts.CreateDocument("text/plain")`, defaulting to `DockDrop-Recovery-Key-<SetName>.txt`) and `"Copy"` (with Android 13+ `ClipDescription.EXTRA_IS_SENSITIVE` flag set on `ClipData` so the clipboard preview masks the key and auto-clears).
3. **Zero Key Logging Rule:**
   - Passphrases, Recovery Keys, `KEK`s, and `MK`s are never passed to `Log.*`, `println`, exception messages, or `toString()` (sensitive holders override `toString()` to return `"[REDACTED]"`), and byte/char arrays are zeroed after key derivation.

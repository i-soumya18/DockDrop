## Summary

<!-- Provide a brief overview of what this pull request does and why it is needed. -->

## Type of Change

- [ ] `feat`: New feature or user capability
- [ ] `fix`: Bug fix
- [ ] `refactor`: Code refactoring without behavior change
- [ ] `test`: New or updated tests
- [ ] `docs`: Documentation updates
- [ ] `chore`: Build scripts, dependencies, or tooling

## Work Domain Ownership (AGENTS.md)

- [ ] A1 — Storage (SAF, volume identity, tree permissions)
- [ ] A2 — Trigger & Service (USB attach, DropService, notifications)
- [ ] A3 — Engine (Scan, diff, copy, verify, commit)
- [ ] A4 — Manifest & Policy (Manifest NDJSON, HEAD pointer, retention)
- [ ] A5 — Restore (Restore plan, conflict resolution, new phone)
- [ ] A6 — Crypto (Encrypted Sets, AES-256-GCM, recovery keys)
- [ ] A7 — UX & Motion (Compose screens, design tokens, a11y)
- [ ] A8 — Quality & Release (CI gates, tests, Play readiness)

## Related Issues

<!-- Closes #123 -->

## Mandatory Pre-Flight Verification Checklist

Please verify each of the following before requesting review:

- [ ] **Rule 3 Zero Network**: No `android.permission.INTERNET` or `android.permission.ACCESS_NETWORK_STATE` declared or imported.
- [ ] **Rule 1 Read-Only**: Source files on the phone are strictly read-only; no code modifies or deletes phone files.
- [ ] **Rule 2 Honest Verification**: Receipt only reports "Sealed" if bytes were verified against hashes.
- [ ] **Rule 4 Plain Mirror**: Standard filesystem layout maintained; no proprietary lock-in.
- [ ] **Unit Tests**: Ran `./gradlew check` and all tests pass cleanly.
- [ ] **Zero Network Task**: Ran `./gradlew verifyZeroNetworkPermissions` and verified clean pass.
- [ ] **Glossary Adherence**: Uses canonical terms (Dock, Set, Drop, Seal, Receipt, Vault, Manifest).
- [ ] **Device Verification**: Tested on a physical device or emulator where applicable.

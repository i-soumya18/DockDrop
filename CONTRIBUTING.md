# Contributing to DockDrop

Thank you for your interest in contributing to DockDrop! DockDrop is an offline-first Android backup tool built on extreme trust, predictable throughput, and zero vendor lock-in.

To maintain the highest level of trust and code quality, please read through these guidelines before submitting any pull requests or opening issues.

---

## 1. Non-Negotiable Core Principles

Before writing any code, you **must** be familiar with the core tenets defined in [CLAUDE.md](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/CLAUDE.md):

1. **Sources Are Read-Only**: A Drop (backup) **never** modifies, moves, or deletes any file on the phone. Restore writes *only* to a destination confirmed on the preview screen.
2. **"Sealed" Must Be Honest**: A backup is only declared "Sealed" if all files were written via temporary files and atomically renamed, size-matched, and checksum-verified at the declared verification level. Receipts never exaggerate verification.
3. **Zero Network (Hard Enforcement)**: The project manifest must **never** declare `android.permission.INTERNET` or `android.permission.ACCESS_NETWORK_STATE`. CI automatically fails if either appears. DockDrop contains zero analytics, crash reporters, telemetry, accounts, ads, or remote config.
4. **Plain Mirror Is the Default Format**: Unencrypted Sets are stored as standard directory trees on the SSD so users can browse and restore their files on any computer without needing the DockDrop app.
5. **Atomic and Resumable**: Interrupted Drops (cable pulled, battery dead, process killed) resume from a persistent journal without corrupting prior manifest generations.
6. **Deletions Are Preserved**: Files removed from the phone move to `_deleted/<Set>/<YYYY-MM-DD>/` on the Dock. Mass-delete protection guards against unintended data loss.
7. **Storage Access Framework (SAF) Only**: All external volume operations use Android SAF tree permissions.

---

## 2. Work Domains (Architectural Ownership)

DockDrop's architecture is divided into eight narrow-scope domains described in [AGENTS.md](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/AGENTS.md):

| Domain | Scope | Key Rule |
|---|---|---|
| **A1 — Storage** | SAF trees, DocumentContract batch queries, URI I/O | Never touches copy planning or UI |
| **A2 — Trigger & Service** | USB attach, Foreground Service, Notifications, Tile | Never inspects file contents |
| **A3 — Engine** | Pipeline: Scan → Diff → Copy → Verify → Commit | Operates through `StorageGateway`, never touches raw URIs |
| **A4 — Manifest & Policy** | Manifest generations, HEAD pointer, Deletions, Room cache | Single manifest reader/writer protocol |
| **A5 — Restore** | Manifest browsing, conflict resolution, restore planning | Reads through A4's manifest reader |
| **A6 — Crypto** | AES-256-GCM chunked streams, Argon2id/scrypt, Keystore | Transparent stream wrapper |
| **A7 — UX & Motion** | Jetpack Compose screens, tokens, haptics, a11y, 60fps | Observes `StateFlow` only, never calls Engine |
| **A8 — Quality & Release** | CI lint gates, test matrix, Play Store declarations | Zero-network verification |

---

## 3. Canonical Glossary

Use these terms consistently across code, UI strings, documentation, and commit messages:

- **Dock**: An external SSD registered with DockDrop, identified by its unique drive ID.
- **Set**: A named group of source folders and policies backed up to a specific Dock.
- **Drop**: A single backup operation.
- **Seal**: A Drop that completed and passed verification.
- **Receipt**: The persistent summary record of a completed Drop.
- **Vault**: The root directory picked on the SSD (`DockDrop/`).
- **Manifest**: The on-SSD append-only index of Set contents per generation.

---

## 4. Development Environment

### Prerequisites
- **Android Studio**: Ladybug (2024.2.1) or Meerkat (2024.3+) recommended.
- **JDK**: Java 17 or Java 21 (Temurin / Android Studio bundled JDK).
- **Target SDK**: Android 15 (API 35). Minimum SDK: Android 8.0 (API 26).
- **Gradle**: Uses the Gradle Wrapper included in the repository (`./gradlew` or `gradlew.bat`).

### Building from Command Line
```bash
# Clone the repository
git clone https://github.com/i-soumya18/DockDrop.git
cd DockDrop

# Run unit tests
./gradlew testDebugUnitTest

# Run full verification (unit tests + zero-network permission assertion)
./gradlew check

# Build debug APK
./gradlew assembleDebug
```

---

## 5. Git Branching & Commit Conventions

### Branching Strategy
- `main`: Production/release-ready branch. All PRs target `main`.
- Feature branches: `feat/<short-description>` (e.g., `feat/saf-batch-listing`)
- Fix branches: `fix/<short-description>` (e.g., `fix/cable-disconnect-journal`)
- Docs branches: `docs/<short-description>` (e.g., `docs/restore-sop`)

### Commit Messages
We follow [Conventional Commits](https://www.conventionalcommits.org/):
```
<type>(<scope>): <short summary in present tense>

[optional body explaining context and rationale]

[optional footer(s), e.g., Closes #123]
```
Common types:
- `feat`: A new feature
- `fix`: A bug fix
- `docs`: Documentation only changes
- `refactor`: Code change that neither fixes a bug nor adds a feature
- `test`: Adding missing tests or correcting existing tests
- `chore`: Build scripts, dependencies, tooling updates

---

## 6. Coding & UI Guidelines

1. **Kotlin Style**: Follow official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
2. **Jetpack Compose**:
   - Ensure composables are stateless where possible with state hoisted to ViewModel.
   - All state exposed to Compose must be immutable `StateFlow` values.
   - Use stable types to prevent unnecessary recompositions.
   - Adhere to design tokens in `DESIGN.md` (no hard-coded colors or arbitrary padding).
   - Support `LocalContentColor` and accessible touch targets (minimum 48dp).
3. **Coroutines**:
   - Dispatch heavy I/O to `Dispatchers.IO` and ensure coroutines support cooperative cancellation (`ensureActive()` / `yield()`).
4. **No Network Imports**:
   - Do not import `java.net.*` (except purely offline URI utilities) or HTTP client libraries.

---

## 7. Submitting a Pull Request

Before opening a pull request:
1. Ensure your branch is rebased on latest `main`.
2. Verify that all automated checks pass locally:
   ```bash
   ./gradlew check
   ```
3. Verify that `verifyZeroNetworkPermissions` passes cleanly:
   ```bash
   ./gradlew verifyZeroNetworkPermissions
   ```
4. Fill out the [Pull Request Template](file:///.github/PULL_REQUEST_TEMPLATE.md) completely, including the compliance checklist.
5. Every pull request will trigger the GitHub Actions CI pipeline which runs automated test suites and permission audits.

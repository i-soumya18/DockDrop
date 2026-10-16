# ADR-004: Dependency Injection Architecture (Manual DI)

- **Status:** Accepted
- **Date:** 2026-10-16
- **Deciders:** DockDrop Team (A1 Storage, A3 Engine, A4 Manifest, A7 UX, A8 Quality)
- **References:** [CLAUDE.md](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/CLAUDE.md) §6, [AGENTS.md](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/AGENTS.md) Handoff Rules 1–4

---

## Context

DockDrop requires dependency management across its modules:
1. Core Storage Gateway (`StorageGateway`, `SafStorageGateway`).
2. Versioned Manifest Repository (`ManifestRepository`).
3. Local Room Database & Journal (`DropDatabase`, `SetDao`, `JournalDao`, `ReceiptDao`).
4. Pipeline Engine (`DropEngine`).
5. Foreground Service (`DropService`).
6. UI ViewModels (`HomeViewModel`, `WizardViewModel`, `DropViewModel`, `ReceiptViewModel`).

The candidate approaches considered:
1. **Dagger / Hilt**: Standard in Android, but introduces annotation processing (KAPT/KSP), generated code, reflection/service loaders, build-time penalties on the dev machine, and steep learning curves for fault-injection test harnesses.
2. **Koin**: Lightweight service locator, but runtime resolution can lead to runtime crashes (`NoBeanDefFoundException`) not caught at compile time.
3. **Manual Dependency Injection (`AppContainer`)**: A plain Kotlin object graph instantiated in the `Application` class, passed via constructor injection to ViewModels (via `ViewModelProvider.Factory`) and services.

---

## Decision

We adopt **Manual Dependency Injection via `AppContainer`** per [CLAUDE.md](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/CLAUDE.md) §6 (*"Keep it boring"*).

### Implementation Details:
- `DockDropApp` holds a single `lateinit var container: AppContainer` created in `onCreate()`.
- `AppContainer` lazily initializes core singletons:
  - `storageGateway: StorageGateway`
  - `manifestRepository: ManifestRepository`
  - `database: DropDatabase` (Room)
  - `engine: DropEngine`
  - `setRepository: SetRepository`
- ViewModels obtain dependencies via explicit `ViewModelProvider.Factory` implementations.
- Unit and fault-injection tests bypass `AppContainer` entirely and construct `DropEngine`, `ManifestRepository`, and `StorageGateway` directly with custom parameters, test scopes, or fault-injecting test doubles.

---

## Consequences

### Positive:
- **Zero build overhead:** No KAPT/KSP compiler plugins for DI, minimizing clean and incremental Gradle build times.
- **100% compile-time safety:** Missing dependencies produce straightforward Kotlin compiler errors.
- **Transparent debugging:** Stack traces go directly to constructor calls without reflection or generated proxies.
- **Simpler fault injection:** Fault injection tests for A3 (cable pulls) and A4 (process kill between commit protocol steps) instantiate components directly with test-controlled gateways and dispatchers.

### Negative:
- ViewModels require boilerplate `ViewModelProvider.Factory` companion objects or factory classes. Given the small number of screens (< 10 ViewModels), this cost is negligible compared to the architectural simplicity.

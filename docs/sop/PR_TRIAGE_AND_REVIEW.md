# SOP: Pull Request Triage and Review

This Standard Operating Procedure (SOP) defines the workflow and criteria for reviewing and triaging contributions to the DockDrop repository.

---

## 1. Review Objectives

The primary goal of every code review is to protect user data, preserve offline trust guarantees, maintain high performance on resource-constrained devices (e.g., Galaxy A14), and uphold architectural boundaries.

---

## 2. Reviewer Checklist

When reviewing any pull request, verify each of the following points:

### A. Non-Negotiable Core Tenets (Immediate Rejection Criteria)
- [ ] **Zero Network**: Does the PR introduce any network permissions (`INTERNET`, `ACCESS_NETWORK_STATE`), HTTP client libraries, sockets, or remote logging? If yes, **reject immediately**.
- [ ] **Read-Only Sources**: Does any code path delete, truncate, overwrite, or mutate source files on the phone? If yes, **reject immediately**.
- [ ] **Honest Verification**: Does any code path report a Drop as "Sealed" without computing checksums and committing a manifest generation?
- [ ] **Storage Access Framework**: Does the PR attempt to introduce `MANAGE_EXTERNAL_STORAGE` (All Files Access)? (v1 is SAF-only).

### B. Architectural Boundaries (AGENTS.md)
- [ ] **Storage Gateway (A1)**: UI or Engine code must not construct raw SAF URIs or call `DocumentFile` per-file.
- [ ] **Engine (A3)**: Pipeline steps must remain cancellable and resumable via coroutines.
- [ ] **UI Layer (A7)**: Composables must observe `StateFlow` only and never invoke engine operations directly.
- [ ] **Domain Separation**: Ensure PR changes belong within the specified domain without cross-layer coupling.

### C. Performance & Android Lifecycle
- [ ] **Batch Queries**: Are directory listings performed via `DocumentsContract` cursor queries rather than recursive `DocumentFile` traversal?
- [ ] **Buffer Sizes**: Streaming copy operations must use the designated 1MB buffer.
- [ ] **Compose Recomposition**: Are lambda parameters unstable? Are heavy calculations performed inside composable bodies without `remember`?
- [ ] **Foreground Service**: Does the service lifecycle respect Android 15 `dataSync` timeouts and handle `onTimeout()` gracefully?

---

## 3. Pull Request Triage Workflow

```
       New PR Opened
             │
      ┌──────▼──────┐
      │  CI Check   │ ──(Fails)──► Request contributor fix
      └──────┬──────┘
             │ (Passes)
      ┌──────▼────────┐
      │ Triage Labels │ (e.g. area:engine, area:ui, status:needs-review)
      └──────┬────────┘
             │
      ┌──────▼──────┐
      │ Code Review │ ──(Changes Requested)──► Contributor updates PR
      └──────┬──────┘
             │ (Approved)
      ┌──────▼──────┐
      │ Squash/Merge│
      └─────────────┘
```

### Review SLA
- **First Response**: Within **48 hours** of opening.
- **Merge Strategy**: **Squash and Merge** to maintain a linear, clean commit history on `main`.
- **Commit Format**: Ensure the squash commit title conforms to Conventional Commits (`type(scope): summary`).

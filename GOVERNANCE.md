# DockDrop Governance Model

DockDrop is an open-source, community-driven project dedicated to private, reliable offline backup for Android. This document outlines how the project is governed, how architectural decisions are made, and how contributors can assume leadership roles.

---

## 1. Project Roles

### Contributors
Anyone who submits code, documentation, bug reports, feature suggestions, or participates in discussions:
- Can submit issues and pull requests.
- Can participate in discussions and reviews.
- Must adhere to the [Code of Conduct](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/CODE_OF_CONDUCT.md).

### Core Contributors
Contributors who have demonstrated sustained, high-quality contributions across DockDrop's 8 work domains:
- Triage issues and review PRs.
- Assist with release validation and testing on physical hardware.
- Gain write access to repository branches.

### Maintainers
Project leaders responsible for the overall technical roadmap, security, and release delivery:
- Final approval on architectural changes and pull requests.
- Authorization and signing of official Google Play / GitHub releases.
- Enforcement of the Code of Conduct and security vulnerability triage.

---

## 2. Decision-Making & Architectural Changes (ADRs)

To ensure the project remains faithful to its core philosophy (Zero Network, Read-Only Sources, Honest Verification, Atomic Resumability), significant architectural changes must follow the **Architecture Decision Record (ADR)** process:

1. **Proposal**: Open an issue or draft an ADR document under `docs/adr/ADR-XXX-<name>.md` using the standard format (Status, Context, Decision, Consequences).
2. **Review**: Community and maintainers discuss trade-offs, performance impact on low-end hardware (e.g. Galaxy A14), and compliance with `CLAUDE.md`.
3. **Consensus**: Changes impacting core guarantees require maintainer consensus before merging.
4. **Non-Negotiables**: Proposals requesting network permissions (`INTERNET`), telemetry, proprietary cloud sync, or automated deletion of source files will be respectfully declined without prolonged debate.

---

## 3. Becoming a Maintainer

Contributors who show consistent commitment, thoughtful reviews, respectful communication, and deep understanding of Android storage internals can be invited to become maintainers upon nomination and approval by existing maintainers.

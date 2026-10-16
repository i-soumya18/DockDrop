# Security Policy

DockDrop takes security and user data integrity with the utmost seriousness. Because DockDrop handles private user data (photos, personal documents, and financial files), our security posture is designed for extreme trust and offline safety.

---

## 1. Zero Network Philosophy & Attack Surface

DockDrop contains **zero network permissions** (`INTERNET` and `ACCESS_NETWORK_STATE` are banned in the manifest and blocked by CI). Because DockDrop never connects to the internet:
- There is **no remote server attack surface**, no cloud API keys, and no telemetry to intercept.
- Security concerns in DockDrop primarily revolve around:
  1. **Data Loss / Data Corruption**: Accidental modification or deletion of source files (violating Rule 1).
  2. **Storage Sandboxing / Path Traversal**: Ensuring files written to external storage cannot escape the designated Vault (`DockDrop/`) or overwrite arbitrary external directories.
  3. **Cryptographic Integrity**: AES-256-GCM chunked stream implementations, Argon2id/scrypt key derivation, and recovery key generation for Encrypted Sets.
  4. **Permission Persistence**: Safe handling of Android Storage Access Framework (SAF) URI grants.

---

## 2. Supported Versions

We provide security updates for the following versions:

| Version | Supported | Notes |
|---|---|---|
| `1.0.x` / `main` | :white_check_mark: | Active release & development |
| `< 1.0.0` | :x: | Experimental / Alpha pre-releases |

---

## 3. Reporting a Vulnerability

If you discover a security vulnerability in DockDrop, **please do not disclose it publicly in an open GitHub issue or public forum.**

### Preferred Method: GitHub Private Vulnerability Reporting
Please submit your finding through GitHub's built-in **Private Vulnerability Reporting**:
1. Navigate to the [DockDrop Security tab](https://github.com/i-soumya18/DockDrop/security).
2. Click **Advisories** → **Report a vulnerability**.
3. Provide a detailed summary, including:
   - Vulnerability type (e.g., path traversal, crypto flaw, SAF URI escalation).
   - Steps to reproduce or proof-of-concept code.
   - Affected Android versions or device configurations.
   - Potential impact on user data or privacy.

### Alternative Method: Direct Security Contact
If you cannot use GitHub Advisories, you can email our security team directly:
- **Email**: [security@soumyaranjansahoo.in](mailto:security@soumyaranjansahoo.in)
- Please use the subject line: `[VULNERABILITY] DockDrop - <Short Summary>`.

---

## 4. Response Timeline & SLA

We are committed to handling all vulnerability reports quickly and transparently:
- **Acknowledgment**: Within **48 hours** of receiving your report.
- **Initial Assessment**: Within **5 business days**, detailing validity and severity.
- **Remediation & Patch**: We aim to release a fix within **14 business days** for high/critical severity issues.
- **Coordinated Disclosure**: We request that you give us up to **30 days** to issue a patched release before disclosing details publicly. We will gladly credit you in our release notes and GitHub Security Advisory (unless you prefer to remain anonymous).

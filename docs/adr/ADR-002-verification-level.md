# ADR-002: Default Verification Level & Streaming Buffer Size

- **Status:** Accepted
- **Date:** 2026-10-16
- **Deciders:** DockDrop Team (A3 Engine Agent & Owner)
- **Evidence:** [PHASE0_RESULTS.md](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/docs/spikes/PHASE0_RESULTS.md) §5 (`B1`–`B5`) · [phase0_raw_runs.csv](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/docs/spikes/phase0_raw_runs.csv)

---

## Context

Per [CLAUDE.md](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/CLAUDE.md) §4 Rule 2 (*"'Sealed' must be honest"*), a Drop is only marked **Sealed** when every planned file has been written via `.part` temp-file + atomic rename, size-matched, hash-checked at the declared verification level, and committed to a new manifest generation.

[PHASE_PROMPTS.md](file:///c:/Users/sahoo/AndroidStudioProjects/DockDrop/PHASE_PROMPTS.md) established the decision rule and owner-confirmed threshold:
> *"If the extrapolated first-Drop time with a full destination re-read is **≤ 45 minutes** for 20 GB, default to `'size + SHA-256 on copy + re-read of copied files'`. Otherwise default to `'size + SHA-256 on copy'`, with re-read only for incremental Drops."*

### Measured Throughput on Galaxy A14 (`SM-A146B`) + Hikvision 128 GB exFAT SSD

Using a `1 MB` (`1,024 KB`) streaming buffer with on-the-fly `SHA-256` and `.part` → final atomic rename across 3 runs each:

| Benchmark | Workload | Run 1 | Run 2 | Run 3 | **Median** |
|---|---|---|---|---|---|
| **B1** | 1 GB Single File Write | `36.14 MB/s` | `35.84 MB/s` | `35.90 MB/s` | **`35.90 MB/s`** (`28.52 s`) |
| **B2** | 2,000 × 100 KB Files Write | `2.89 MB/s` (`29.6/s`) | `2.22 MB/s` (`22.7/s`) | `2.24 MB/s` (`23.0/s`) | **`2.24 MB/s`** (`22.97 files/s`) |
| **B3** | Mixed Write (`200 × 1–20 MB` + `2,000 × 100 KB` = `2.41 GB`) | `14.76 MB/s` (`14.2/s`) | `14.61 MB/s` (`14.0/s`) | `14.54 MB/s` (`13.9/s`) | **`14.61 MB/s`** (`14.00 files/s`) |
| **B4-B1** | 1 GB Single File Read-Back | `35.70 MB/s` *(uncached)* | `448.53 MB/s` *(VFS cache)* | `457.96 MB/s` *(VFS cache)* | **`35.70 MB/s`** *(physical USB)* |
| **B4-B2** | 2,000 × 100 KB Files Read-Back | `5.12 MB/s` (`52.5/s`) | `7.04 MB/s` (`72.0/s`) | `7.37 MB/s` (`75.5/s`) | **`7.03 MB/s`** (`72.04 files/s`) |
| **B4-B3** | Mixed Read-Back (`2.41 GB`, `2,200 files`) | `23.03 MB/s` (`22.1/s`) | `23.10 MB/s` (`22.1/s`) | `28.00 MB/s` (`26.8/s`) | **`23.10 MB/s`** (`22.14 files/s`) |

### 20 GB First-Drop Extrapolation (`20,480 MB` Mixed Dataset)

1. **Copy + SHA-256 on Copy (at `B3` median `14.61 MB/s`):**
   $$\frac{20,480\text{ MB}}{14.61\text{ MB/s}} = 1,401.8\text{ s} \approx \mathbf{23.4\text{ minutes}}$$
2. **Full Destination Re-Read + SHA-256 Verification (at `B4-B3` median `23.10 MB/s`):**
   $$\frac{20,480\text{ MB}}{23.10\text{ MB/s}} = 886.6\text{ s} \approx \mathbf{14.8\text{ minutes}}$$
3. **Total 20 GB First-Drop Time (Copy + Full Destination Re-Read):**
   $$23.4\text{ min} + 14.8\text{ min} = \mathbf{38.1\text{ minutes}} \le 45.0\text{ minutes}$$

---

## Decision

1. **Default Verification Level:** **`"size + SHA-256 on copy + re-read"`** (full destination re-read of all copied files) for **both** first Drops and incremental Drops.
   - The Receipt (S10) will explicitly display: `"Verified: size + SHA-256 on copy + re-read"`.
2. **Deferred Re-Read Order (VFS Page Cache Mitigation):**
   - `B4-B1` revealed that re-reading a file from its SAF URI immediately after writing/reading it hits the Linux kernel VFS page cache in RAM (`448.53 MB/s`) if the file has not yet been evicted.
   - Therefore, the Engine **must not** re-read each file immediately after writing it. Instead, the `Dropping` stage writes all planned files in FIFO order (computing the source/write SHA-256 on the fly and recording sizes/hashes in the working journal), and only after the entire copy pass finishes does the Engine transition to `Verifying` and re-read the destination files in FIFO order (so earlier files have been evicted from RAM by subsequent writes).
3. **Streaming Buffer Size:** **`1 MB` (`1,048,576` bytes)** reusable single buffer per copy coroutine, keeping heap allocation flat (`< 5 MB` delta across 2,200 files).
4. **A1 Directory Listing Budget:** **`≤ 1,500 ms` for 5,000 files** using `DocumentsContract.buildChildDocumentsUriUsingTree` batch cursor queries (observed median: **`285 ms`** for 5,000 files; per-file `DocumentFile` is prohibited).

---

## Consequences

- **Positive:** Every Sealed backup has its destination bytes re-read and SHA-256 verified before the manifest generation is committed, fulfilling Rule 2 with zero compromise while staying within `~38.1 minutes` for a 20 GB initial backup and `< 1 minute` for typical incremental backups.
- **Negative / Mitigation:** Adds `~14.8 minutes` to a 20 GB first Drop compared to on-copy hashing alone; mitigated by clear progress during the `Verifying` stage (`S09` / `SCREENS.md` §3: `"Verifying {folder} · {a} of {b} folders"`).

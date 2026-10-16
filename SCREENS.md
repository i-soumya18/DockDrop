# SCREENS.md — DockDrop Per-Screen Specs

> All screens apply DESIGN.md tokens (colors `bg/surface/dock/seal/attention/danger`, radii `sm/md/lg/sheet`, motion `Snap/Settle/Flow/Exit/Breathe`).
> **Reference status:** no mockups or screenshots exist yet. Every screen below is **spec-only**.
> Copy is final-draft; tone is calm, precise, no hype, no emoji.

## 1. Navigation map

```
Onboarding (S01) → Keep-alive helper (S02) → Home (S03)
Home ─ bottom bar: Home · Restore · Settings
Home → Set wizard (S05-S08) → Home (morph) → [Dock connected?] → Drop (S09) → Receipt (S10)
USB attach → Plug-in sheet (S04) → Drop (S09)
Drop → Attention sheets (S11)
Restore (S12) → Browser (S13) → Preview (S14) → Restore progress/receipt (S15)
Restore (S12) → New-phone flow (S16)
Home → Set detail (S17) → History (S21)
Wizard/Set detail → Encryption setup (S18); Restore → Unlock (S19)
Settings (S20)
Debug only: Spike Lab (S22)
```
Predictive back everywhere. Bottom bar is hidden on Drop, wizard, Receipt, Restore progress (full-focus screens).

## 2. Global layout rules

- Edge-to-edge; 20 dp gutters; primary CTA bottom-anchored, 56 dp, `dock` fill, `onDock` text.
- One primary action per screen. Secondary actions are text buttons.
- Every list has an empty state and a loading placeholder (breathing, content-shaped).
- Technical detail (paths, hashes, generation numbers) is behind a "Details" disclosure.

## 3. State → copy & notification mapping (single source for UI + notifications)

| State | Home / screen headline | Sub-line | Notification (title / text / actions) |
|---|---|---|---|
| `NoDock` (Set exists) | Plug in your SSD | Last sealed {relative time} · {n} files | none |
| `DockDetected` | {Dock label} connected | {free} free · {fs} | **SSD detected · {label}** / "Tap to back up {Set}" / [Back up now] |
| `AwaitingConfirm` | Ready to back up | {n} new or changed files · about {x} min | same as above |
| `Preflight` | Checking your SSD… | Testing write access | "Checking SSD…" |
| `Dropping` | Backing up {folder} | {a} of {b} folders · about {x} min left | **Backing up · {pct}%** / "{folder} · {a} of {b} · about {x} min left" / [Pause] [Stop] |
| `Verifying` | Verifying {folder} | {a} of {b} folders | **Verifying · {a} of {b}** / "Almost there" / [Stop] |
| `Sealed` | Sealed | {n} files verified · nothing deleted from your phone | **Sealed · {n} files verified** / "All writes finished. Safe to unplug." |
| `Partial` | Finished with {k} skipped | {k} files couldn't be read | **Finished with {k} skipped** / "Tap to review" / [Review] (amber, never green) |
| `Paused` | Paused | {reason: battery below {x}% / you paused} | **Paused** / "{reason}" / [Resume] [Stop] |
| `Interrupted` | Backup interrupted | SSD was disconnected at {pct}% | **Backup interrupted** / "Plug the SSD back in to resume" |
| `Resumable` | Resume backup | {pct}% done · {n} files left | **Resume backup?** / "{pct}% done" / [Resume] |
| `Attention(NoSpace)` | SSD is full | Needs {need}, {free} free | **Backup stopped** / "SSD is full. Needs {need}, {free} free." |
| `Attention(PermissionLost)` | DockDrop lost access to {folder} | Grant it again to continue | **Needs your attention** / "Access to {folder} was removed." / [Fix] |
| `Attention(MassDelete)` | Mirror mode wants to remove {n} files | Nothing has been removed | **Review needed** / "Mirror mode would remove {n} files from the SSD." / [Review] |

`Partial` is a terminal state: finished, but with skipped files. It is amber, never green, and never says "Sealed" (CLAUDE.md Rule 2).

---

## 4. Screens

### S01 — Welcome (onboarding, 3 pages, skippable)
**Purpose:** set the mental model in 20 seconds and establish trust.
**Layout:** full-bleed `bg`, hero Dock glyph (absent → detected animation loops once per page change), headline, 2-line body, page dots, bottom CTA.
- Page 1: "Your phone. Your SSD. Nothing in between." / "Plug in a drive you own, tap once, and your photos, videos and documents are copied and verified."
- Page 2: "Verified, not just copied." / "Every backup ends with a Receipt telling you exactly what was checked."
- Page 3: "Fully offline." / "DockDrop has no internet permission. Your files never leave your phone and your drive." + chip `No network access` + link "How to check".
**States:** none. **Interactions:** swipe or CTA "Continue"; last page CTA "Get started". The Dock cable latches with `Snap` on each page change.

### S02 — Keep DockDrop running (Samsung battery helper)
**Purpose:** prevent One UI from killing long backups.
**Layout:** title, illustration-free 3-step list with checks that update live where detectable (battery optimization exemption via system dialog; "Sleeping apps" step is a guided deep link + manual confirm).
- Title: "Let backups finish with the screen off"
- Steps: 1) Allow notifications. 2) Don't restrict battery for DockDrop. 3) Remove DockDrop from Sleeping apps (Samsung).
**States:** each step `todo / done / can't detect (manual confirm)`. **CTA:** "Done" (enabled after steps 1-2; step 3 can be skipped with "Remind me later").

### S03 — Home (Dock dashboard)
**Purpose:** answer "is my data safe right now?" in one glance.
**Layout (top → bottom):**
1. Top bar: wordmark "DockDrop" left, no actions.
2. **Hero Dock** (~40% height, centered): glyph + ring per DESIGN.md 5.2.1; below it the state headline (Headline style) and sub-line (Body small, `textSecondary`) from section 3.
3. **Resume/Attention card** (only if applicable): `surface` card, amber/`dock` leading bar, one action.
4. **Set cards** (`surface`, radius `md`): Set name (Title), folder summary ("Camera, WhatsApp, Documents"), "Last sealed 3 days ago", status dot (green only if the last Drop is Sealed). Tap → S17.
5. Footer microcopy: "Offline by design" with lock icon (tap → S20 Privacy).
6. Bottom CTA (state-dependent): `NoSet`: "Create your first backup" · `DockConnected`: "Back up now" · `NoDock`: disabled "Plug in your SSD" with helper text.
7. Bottom bar: Home · Restore · Settings.
**States**
- *First run, no Set:* hero absent animation, headline "Back up your phone to an SSD you own", CTA creates a Set.
- *Set, no Dock:* absent animation, last-sealed info.
- *Dock connected, idle:* solid Dock, free-space bar, "Back up now".
- *Dropping / Verifying:* hero becomes a compact live Dock; whole hero is tappable → S09.
- *Resumable / Attention:* card above Set list; hero ring shows amber partial.
**Interactions:** pull nothing; no refresh gestures. Cable animation reacts live to attach/detach (latch / retract).

### S04 — Plug-in sheet (launched by USB attach)
**Purpose:** the "magic moment"; get from cable to Drop in one tap.
**Layout:** bottom sheet (radius `sheet`) over the previous app or Home. Dock glyph latching in (cable `Snap`), tick haptic, text "Samsung T7 · 412 GB free · exFAT", line "{Set} · {n} files to back up · about {x} min", CTA "Start backup", text button "Not now".
**States:** *Unknown Dock* (not registered): "New drive detected" → CTA "Set up this SSD" → S06. *Registered Dock, no changes:* "Everything is already backed up. Last sealed {time}." CTA "Done".
**Interactions:** tapping "Start backup" starts the foreground service (user interaction requirement) and transitions the sheet into S09 with a shared-element Dock. Never auto-starts.

### S05 — Wizard 1/4: What should we protect?
**Purpose:** choose source folders with minimal effort.
**Layout:** progress dots (4), title "What should we protect?", list of **suggestion cards** (checkbox, folder name, estimated size, last modified), then "Choose another folder" (outlined button, opens SAF picker).
- Suggestions (pre-checked where grantable): Camera (DCIM/Camera), Pictures, WhatsApp media (Android/media/com.whatsapp/WhatsApp/Media), Documents, Download subfolders.
- Each card triggers the system folder picker pre-navigated to that location (`EXTRA_INITIAL_URI`); on grant, the size estimate fades in (placeholder breathes while scanning).
**Constraint (Phase 0 verifies):** Android 11+ does not allow selecting some roots (e.g. the Download root, `Android/data`). Cards for blocked locations explain it in one line ("Android doesn't allow backing up the whole Downloads folder. Choose a sub-folder instead.") and offer the sub-folder picker. WhatsApp's chat database and other apps' private data are not backed up; the screen states this once, at the bottom ("App data and chat databases can't be backed up by any app on Android").
**States:** *scanning size*, *granted*, *blocked*, *permission error*. **CTA:** "Next · {total size}" disabled until ≥ 1 folder.

### S06 — Wizard 2/4: Where do we back up?
**Purpose:** pick the Dock and the Vault directory; prove it works.
**Layout:** title "Choose your SSD". If connected: **Dock card** (glyph latched, name, capacity ring with used/free, filesystem chip `exFAT ✓`). Then "Where on the SSD?" row showing path `SSD / DockDrop` with "Change" (SAF picker on the SSD; default suggestion creates/uses a `DockDrop` folder). Then a **write test** line.
**Write test micro-interaction:** on location confirm, "Testing write access…" → writes and deletes a ~16 MB temp file → "Write test passed · 38 MB/s". The measured speed feeds the ETA on S08.
**States:** *No Dock:* absent animation, "Plug in your SSD to continue", text button "Do this later" (saves a draft Set). *Unsupported/Read-only filesystem:* amber card "This drive is read-only on your phone. NTFS drives often are. exFAT works everywhere." *Not enough space:* amber "Needs {need}, {free} free." **CTA:** "Next".

### S07 — Wizard 3/4: Choose how it behaves
**Purpose:** three decisions, all with safe defaults.
**Layout:** three `surface` cards, each with a live consequence line beneath ("Result: …").
1. **When files are deleted on your phone:** radio list. *Keep them on the SSD* (recommended, default) · *Keep for N days* (stepper 7/30/90) · *Mirror exactly* (`danger` tint; selecting it opens a confirmation sheet: "Mirror mode removes files from the SSD when you delete them on your phone. Deleted files can't be recovered. Keep this off unless you're sure.").
2. **Protect with a passphrase** (toggle, off by default). Subtext: "Encrypts this backup. If you lose both the passphrase and the recovery key, the backup can't be opened. This can't be turned on later without a new backup." Turning on → S18.
3. **Only back up when battery is above** (chips: 15% · 20% · 30% · Charging only; default 20%).
**CTA:** "Next".

### S08 — Wizard 4/4: Review and create
**Purpose:** confirm and own it (IKEA effect).
**Layout:** editable Set name field (default "My phone"), summary card (folders as chips, Dock, location, policy), estimate card: "First backup: about 14.2 GB · roughly 9-14 min (based on your 38 MB/s write test). Later backups usually take under a minute."
**CTA:** "Create backup set". **Interaction:** the folder chips fly into the new Set card on Home (shared-element). If the Dock is connected, a sheet follows: "Start the first backup now?" [Start backup] [Later].

### S09 — Drop (live backup)
**Purpose:** make a long operation feel alive, honest and interruptible.
**Layout (top → bottom):**
1. Top bar: back arrow (leaves running), Set name, "Details" icon.
2. **The Drop visual** (~50% height): phone glyph at top, curved particle flow, Dock glyph with ring and liquid fill at the bottom (DESIGN.md 5.2.1-5.2.3).
3. Status headline: "Backing up Camera"; mono counters `1,204 / 4,812 files · 6.1 / 14.2 GB`; ETA line "about 7 min left" (range when volatile).
4. **Segment chips** row (Camera ✓ · Videos ● · Documents · WhatsApp), the active one pulses.
5. Reassurance caption: "You can lock your phone. Backup keeps going."
6. Bottom: text buttons "Pause" and "Stop" (never styled as primary).
7. "Details" disclosure: current file path (Mono, middle-ellipsized), speed, verification level, skipped count.
**States:** *Preflight* (placeholders breathing, "Checking your SSD…") · *Dropping* · *Verifying* (ring → scan line) · *Paused* (particles halt, ring dims, amber label) · *Interrupted* (cable retracts, "SSD disconnected at 63%. Plug it back in to resume.") · *Attention* (S11). 
**Interactions:** Stop opens a light sheet: "Stop backup? Files copied so far are kept. You can resume later." [Stop] [Keep going]. Completing transitions into S10 with the Dock latch moment. Reduced motion: numeric "files/s" replaces particles.

### S10 — Receipt (Sealed)
**Purpose:** the peak moment; proof, not celebration.
**Layout:** Receipt card printing up from the Dock (perforated top edge, `surface`, radius `lg`): large `seal` check (path-drawn), title "Sealed", count-up numbers (files, size, duration), lines:
- "1,248 files verified" · "14.2 GB" · "11 min 20 s"
- "Verified: size + SHA-256 on copy + re-read" (the actual level used)
- "Nothing was deleted from your phone."
- "Kept from earlier: 12 deleted files in _deleted/"
- "All writes finished. Safe to unplug."
"Details" expands generation number, average speed, skipped files (none), manifest checksum.
**CTA:** "Done". Secondary: "Share receipt" (plain text via system share) and "Test a restore" (S12 drill).
**States:** *Partial* variant: amber icon, title "Finished with 3 skipped", list of skipped files with reasons, CTA "Retry skipped". Never shows green.

### S11 — Attention sheets (one component, many causes)
**Layout:** bottom sheet, amber icon, one-line cause (section 3 copy), one-line consequence ("Nothing was lost. Files copied so far are safe."), primary action, text button.
| Cause | Primary action | Secondary |
|---|---|---|
| SSD full | "Choose what to back up" | "Free up space" |
| Permission lost | "Grant access again" (opens picker at the folder) | "Remove this folder" |
| Mass-delete guard | "Review {n} files" (list of files that would be removed) | "Keep all files" (switch Set to Keep) |
| Read errors | "Retry" | "Skip and continue" |
| Cable pulled | none (state shows on S09; resume on replug) | "Stop backup" |
| Battery low | "Resume when charging" | "Resume anyway" |
Motion: single 2-cycle shake on the Dock + soft reject haptic; reduced motion = color/icon only.

### S12 — Restore hub
**Purpose:** make restore feel as safe and obvious as backup.
**Layout:**
1. Title "Restore".
2. **Backup summary card:** "Latest backup · {Set} · {date} · {size}" with Dock chip (connected / not connected). If the manifest belongs to another phone: "Backup from {device label}".
3. **Hold button** (hero, full-width, 72 dp): "Hold to restore everything". Under it, three info lines that make the hold an informed act: "To original folders · Keep both if a file already exists · {n} files, {size}". If permissions are missing, the button becomes "Grant {k} folders first".
4. "Or restore part of it": three rows: *Choose folders or files* (→ S13) · *Restore to a different folder* · *Restore to a new phone* (→ S16).
5. Small row: "Test restore (10 random files)", a restore drill into a temporary folder with verification, to build trust.
**States:** *No Dock:* absent animation, "Plug in the SSD that has your backup." · *Dock without DockDrop data:* "No DockDrop backup found on this drive." · *Encrypted Set:* hold becomes "Unlock to restore" → S19.
**Interactions:** The Hold per DESIGN.md 5.2.5, then S15.

### S13 — Restore browser
**Purpose:** find and pick things without typing paths.
**Layout:** top: search field (searches names in the manifest), breadcrumb chips. Filter chips: All · Photos · Videos · Documents · Deleted. Body: folder tree; image/video folders switch to a **thumbnail grid** (thumbnails read from the SSD, placeholders breathing). Long-press enters multi-select with a bottom bar "Restore {n} items · {size}" (primary).
**States:** *Loading manifest* (content-shaped placeholders) · *Empty search* ("Nothing matches '{q}'") · *Deleted filter:* items show the date they were removed from the phone.
**Interactions:** tap a file → preview sheet (thumbnail, name, size, date, "Restore this file"); tap "Restore {n} items" → S14.

### S14 — Restore preview
**Purpose:** show exactly what will happen before it happens.
**Layout:** summary header "{n} files · {size}"; **Destination** (segmented: Original folders / Choose folder; default Original); **If a file already exists:** segmented (Keep both · Skip · Overwrite) where Overwrite is `danger` tinted; **Permissions needed:** rows "Camera folder · Grant" for missing access; **Space check:** "Needs {size}, {free} free on your phone ✓" (amber if not).
**CTA:** "Restore {n} files". **States:** *Conflicts found* shows "38 files already exist"; *Space insufficient* blocks the CTA with the reason.

### S15 — Restore progress & receipt
**Purpose:** mirror of S09 in the other direction.
**Layout:** same structure as S09 but the flow is Dock → phone (particles travel upward; ring in `dock`). Final step "Updating your gallery…" (media rescan) then a **Restore receipt** in the S10 format: "Restored 1,204 files · 38 kept both · 0 failed", "Verified: SHA-256 against backup", CTA "Done", secondary "Open Files".
**States:** *Attention* variants reuse S11 (permission lost, no space on phone, SSD removed). **Partial** is amber like S10.

### S16 — New-phone restore
**Purpose:** a guided path from empty app to restored phone.
**Trigger:** first launch with no Sets and a Dock whose Vault contains DockDrop data (or entered from S12).
**Flow (progress dots):**
1. **We found a backup:** "{device label} · last sealed {date} · {n} Sets · {size}". CTA "Restore from this backup".
2. **Map your folders:** for each source folder, a row "Camera → needs access to DCIM/Camera" with "Grant" (picker opens pre-navigated). Optional per row: "Restore somewhere else".
3. **Review:** S14 summary.
4. **Hold to restore:** S12 hero.
**States:** *Encrypted:* unlock first (S19). *Several backups on the drive:* choose one (cards by device and date).

### S17 — Set detail
**Layout:** editable name; folders list (add/remove); policy summary rows (tap to edit: deletion, encryption shown read-only, battery condition); Dock row; **History** (latest 5 Receipts with seal/partial icons, tap → receipt detail, "See all" → S21); "Back up now" CTA when Dock connected.
**Danger zone (bottom, `danger` text):** "Delete this Set. Backup files stay on your SSD."
**Interaction:** shared-element entry from the Home card.

### S18 — Encryption setup
**Purpose:** enable encryption with eyes open.
**Flow:** (a) *Passphrase:* field + confirm, strength meter (plain words: Weak/OK/Strong), `FLAG_SECURE` on, copy: "We can't recover this for you." (b) *Recovery key:* grouped characters (e.g. `7QKM-2D9F-…`), actions "Copy" and "Save as file"; a gate that asks for the last 4 characters before continuing ("Let's check you saved it"). (c) Confirmation: "Encrypted backups can only be restored with the passphrase or this key."
**States:** *mismatch*, *too weak (blocks only if < minimum)*, *key check failed*.

### S19 — Unlock
**Layout:** compact sheet: "This backup is encrypted", passphrase field (`FLAG_SECURE`), "Use recovery key instead", CTA "Unlock". Wrong passphrase: shake + "That doesn't match. Nothing was changed." No lockout beyond a brief delay.

### S20 — Settings
**Sections:** **Backup:** battery condition (default), notifications. **Appearance:** theme (System/Dark/Light), Reduce motion. **Privacy:** card "Offline by design. Network permission: none" with "How to check" (steps to system app permissions). **Keep running:** reopen S02. **Support:** "Export log" (writes a plain-text log the user shares manually), "About" (version, licenses). **Danger zone:** "Forget this SSD" (doesn't touch its files).

### S21 — History
Chronological list of Receipts (sealed / partial / interrupted), tap → full Receipt with Details. Empty: "No backups yet."

### S22 — Spike Lab (debug builds only; Phase 0; functional, not polished)
**Purpose:** gather the evidence Phase 0 needs. Plain Compose, monospace logs, no motion work.
**Tabs**
1. **Trigger log:** live, timestamped list of every event: `USB_DEVICE_ATTACHED/DETACHED`, volume mount/unmount callbacks, `MEDIA_MOUNTED/UNMOUNTED`, with the app state (foreground / background / cached / process started by event) and how the app was launched. Buttons: clear, copy as Markdown.
2. **SAF persistence:** list persisted tree permissions with their URIs; "Probe" button (list a folder, write+delete a tiny file); stored probe history table (cycle number, event: replug/reboot/kill, result).
3. **Throughput bench:** workload picker (1 GB single file · 2,000 × 100 KB · mixed), buffer sizes (256 KB / 1 MB / 4 MB), run write, run read-back, show MB/s and files/s, export CSV.
4. **Listing bench:** list 5,000 files via `DocumentsContract` batch query vs per-file `DocumentFile`; show elapsed times.
5. **Grantability matrix:** buttons that launch the picker at DCIM, Pictures, Documents, Download, `Android/media/com.whatsapp`, SSD root; record granted / blocked / error.
6. **Foreground service lab:** start a test service as `connectedDevice` (after a `UsbManager.requestPermission` attempt) or as `dataSync`, with a debug short-timeout switch to exercise `onTimeout()`.
7. **Export:** one button generating a Markdown report ready to paste into `docs/spikes/PHASE0_RESULTS.md`.

### S23 — Notifications & Quick Settings tile
- **Notification channels:** "Backup progress" (low importance, silent, ongoing), "Backup results" (default importance, no sound by default), "Needs attention" (high importance).
- Progress notification is determinate, shows the segment text from section 3, and never shows animation beyond the system progress bar.
- **Quick Settings tile (fallback trigger, Phase 2):** label "Back up now"; states: *unavailable* (no Dock) · *ready* · *active* (progress %) · *attention*. Tapping when ready opens S04.

---

## 5. Suggested build order

1. **S22 Spike Lab**, then DESIGN tokens + the Dock glyph component (the shared object).
2. **S03 Home** (establishes the visual language, states and the hero).
3. **S09 Drop** and **S10 Receipt** (the signature motion; reuses the Dock glyph; do the frame-time budget here).
4. **S05-S08 Wizard** (reuses cards, chips, sheets).
5. **S11 Attention sheets**, **S04 Plug-in sheet**, **S23 notifications/tile** (Phase 2).
6. **S12-S16 Restore** (reuses S09/S10 components in reverse), **S17/S21** Set detail and History.
7. **S18-S19 Encryption**, then **S20 Settings**, **S01-S02 Onboarding polish** (Phases 4-5).

# DESIGN.md — DockDrop Visual & Interaction System

> Applies to every screen in SCREENS.md. Token names are the source of truth for code.

## 1. Direction

**Mood: calm hardware-grade confidence.** DockDrop should feel like a precision instrument that happens to be friendly: quiet when idle, vivid only when something is physically happening (a cable arrives, bytes flow, a seal closes).

**References (existing products, for feel not imitation)**
- **Linear**: dense-but-calm dark UI, crisp type, restrained accent.
- **Apple Time Machine / Files**: trust through plain language and a clear "last backup" truth.
- **Nothing OS / Teenage Engineering**: one honest dot-matrix-like motif, physical metaphors, tabular numbers.
- **Stripe Dashboard receipts**: the Receipt card is a designed object, not a log.

**One idea runs through everything: the Dock.** A drive glyph with a port, a cable that arrives, a ring that closes. The user learns one visual object and recognizes its state at a glance from across the room.

Dark-first (default follows system; both themes ship).

## 2. Color tokens

### Dark (default)
| Token | Hex | Use |
|---|---|---|
| `bg` | `#0A0F1C` | App background |
| `surface` | `#121A2B` | Cards, sheets |
| `surfaceRaised` | `#1A2338` | Selected/pressed, nested cards |
| `outline` | `#28334D` | 1dp borders, dividers |
| `textPrimary` | `#E8EEF9` | Main text |
| `textSecondary` | `#9AA7C0` | Supporting text |
| `textTertiary` | `#66738F` | Hints, disabled |
| `dock` (primary) | `#4F8CFF` | Primary actions, progress, the Dock glyph |
| `onDock` | `#06122B` | Text/icons on primary |
| `seal` | `#34D399` | Verified/Sealed only (see rule below) |
| `attention` | `#FBBF24` | Needs the user, paused, warnings |
| `danger` | `#FB7185` | Destructive: Mirror mode, overwrite, delete |

### Light
| Token | Hex |
|---|---|
| `bg` | `#F6F8FC` |
| `surface` | `#FFFFFF` |
| `surfaceRaised` | `#EEF2FA` |
| `outline` | `#D5DCEB` |
| `textPrimary` | `#0F172A` |
| `textSecondary` | `#475569` |
| `dock` | `#2F6BEA` |
| `seal` (fill) / `sealText` | `#10B981` / `#047857` |
| `attention` (fill) / `attentionText` | `#F59E0B` / `#B45309` |
| `danger` | `#E11D48` |

### Rules
- **Green is earned.** `seal` appears only after verification passes. Never use it for "done copying", never for decoration. This is the visual half of CLAUDE.md Rule 2.
- **Amber means "you can fix this"**, red means "this can lose data". Never swap them.
- One accent (`dock`) per screen region. Large color fills are reserved for the hero Dock and the Seal moment.
- **Drop particle tints** (data-driven, subtle, 70% alpha): photos `#4F8CFF`, videos `#A78BFA`, documents `#2DD4BF`, other `#9AA7C0`.
- Text on colored fills uses the `on*` token; verify 4.5:1 in both themes.

## 3. Typography (bundled fonts only; the app has no network)

| Role | Font | Size/Line | Weight |
|---|---|---|---|
| Display | Manrope | 34/40 | 700 |
| Headline | Manrope | 28/34 | 700 |
| Title | Manrope | 20/26 | 600 |
| Body | Inter | 16/24 | 400 |
| Body small | Inter | 14/20 | 400 |
| Label | Inter | 12/16 | 600, +2% tracking |
| Numeric/Path | JetBrains Mono | 13/18 | 500 |

- All counters, sizes, speeds and ETAs use **tabular figures** so digits don't jiggle while counting.
- Paths and file names are always Mono, middle-ellipsized (`DCIM/Cam…/IMG_4021.jpg`).
- Sizes use binary-friendly human units (`14.2 GB`), never raw bytes in primary UI.

## 4. Geometry & spacing

| Token | Value |
|---|---|
| Radius `sm` / `md` / `lg` / `sheet` | 12 / 20 / 28 / 32 dp |
| Spacing scale | 4, 8, 12, 16, 24, 32, 48 dp |
| Screen gutter | 20 dp |
| Min touch target | 48 dp |
| Primary CTA | 56 dp tall, full-width minus gutter, bottom-anchored |
| Elevation | None. Depth comes from tonal layers + 1 dp `outline`. Active objects get a soft `dock` glow (blur 24 dp, 14% alpha). |
| Icons | 24 dp, 2 dp stroke, rounded caps (custom set, no filled+outlined mixing) |

## 5. Motion language

Motion exists to answer one of four questions: *What just happened? What is happening now? Is it safe? What can I do?* If an animation answers none, delete it.

### 5.1 Tokens
| Token | Spec | Use |
|---|---|---|
| `Snap` | spring(damping 0.72, stiffness 700) | Cable latch, button press, seal stamp |
| `Settle` | spring(damping 0.85, stiffness 380) | Cards, sheets, hold-release unwind |
| `Flow` | tween 320 ms, cubic-bezier(0.2, 0, 0, 1) | Enter / emphasized decelerate |
| `Exit` | tween 200 ms, cubic-bezier(0.3, 0, 1, 1) | Leave / accelerate |
| `Breathe` | infinite 2400 ms ease-in-out, alpha 0.35↔0.9 | Waiting states (never faster) |
| Durations | micro 120 · small 200 · medium 320 · large 500 · hero 900 ms | |

### 5.2 Signature moments

**1. The Dock (hero object, Canvas-drawn vector, no bitmaps)**
| State | Animation | Haptic |
|---|---|---|
| Absent | Dashed outline drive, a ghost cable slowly drifts toward the port and fades, loop (`Breathe` pacing) | none |
| Detected | Cable slides in 400 ms and latches with `Snap` overshoot (6 dp); drive goes dashed → solid with a 200 ms sharpen; model and free space fade in | tick |
| Confirming | Free-space bar previews the Drop as a reserved segment ("fits: needs 14.2 GB of 412 GB") | none |
| Dropping | Progress ring around the drive; inner liquid fill rises with the Dock's used space; reserved segment fills `dock` as bytes land | none |
| Verifying | Ring turns into a thin sweeping scan line (single pass per folder segment) | light tick per folder |
| Sealed | Ring closes; 4 dp inward "latch" pulse; ring → `seal`; checkmark draws via path trim 380 ms | confirm |
| Attention | Ring segments → `attention`; one 2-cycle 4 dp lateral shake; icon + one-line cause appear | reject (soft) |

**2. The Drop (live data flow; the most distinctive screen in the app)**
- Particles leave the phone glyph at the top and fall along a gentle curve into the Dock's port.
- **Emission rate is real**: derived from actual files/s on a log scale, clamped to 2-24 particles on screen. Size encodes file size bucket; tint encodes type (section 2). If throughput stalls, particles stop. The user can *see* a stall.
- Particles shrink as they enter the port; each arrival adds a tiny `Snap` pulse to the fill level.

**3. Segments (goal-gradient effect)**
The ring is divided by source folder (e.g. Camera | Videos | Documents | WhatsApp). Each segment lights as it completes. Under the ring: `2 of 4 folders · about 7 min left`. The ETA is smoothed (exponential average) and shows a range (`about 6-8 min`) when throughput is volatile. No false precision.

**4. The Seal Receipt (peak-end design)**
After Sealed, the Receipt "prints" up out of the Dock: slide 320 ms `Flow`, perforated top edge, counters count up (600 ms, tabular figures) to final values, a "Verified" stamp scales 1.15 → 1 with `Snap`. This is the last thing the user sees, so it gets the most care.

**5. The Hold (one-click restore)**
Press-and-hold 1.2 s on "Restore everything". A ring fills around the button, the label progresses `Hold to restore everything` → `Keep holding…`, and at completion a burst pulse + confirm haptic fires. Release early: the ring unwinds with `Settle` and a soft haptic. The hold replaces "Are you sure?" dialogs: friction proportional to risk, with zero modal stacking. TalkBack users get a confirm dialog instead (double-tap), reduced-motion users get a determinate fill with no burst.

**6. Wizard → Set card morph (ownership)**
At the end of Set creation, the selected folder chips fly (shared-element transition) into the new Set card on Home. The user watches their own choices become the object they now own.

**7. Plug-in sheet**
When launched by a USB attach, a bottom sheet rises (`Settle`) with the Dock glyph latching in. The primary action ("Start backup") sits in the thumb zone. The sheet never auto-starts a Drop.

**8. Empty/loading without spinners**
Content-shaped placeholders breathing at `Breathe` alpha, no shimmer sweeps (cheaper, calmer). Any wait > 3 s explains itself in words ("Reading 4,812 files on your SSD…").

**9. Navigation**
Predictive back on every screen (Android 15 back preview scales/translates). Shared-element transitions between Set card → Set detail and Receipt list → Receipt detail. Bottom bar has 3 destinations only: Home, Restore, Settings.

### 5.3 Reduced motion
If the system animator scale is 0 (or the in-app "Reduce motion" toggle is on): no particles (replaced by a numeric "files/s" line), no shakes/bursts, the cable simply appears, the ring fill remains (it is information), the Receipt fades in. Every motion above has this equivalent; shipping without one is a bug.

## 6. Haptic language

| Event | Pattern |
|---|---|
| SSD detected | single crisp tick |
| Drop starts | double tick |
| Folder segment complete | light tick |
| Sealed | confirm (system CONFIRM on API 30+, short pulse fallback) |
| Attention | soft reject |
| Hold-to-restore | ticks every 200 ms rising in strength, burst on completion |
| Everything else | none (haptics are rare so they mean something) |

No sound by default. Respect the system haptics setting; no haptics when the screen is off.

## 7. Compose theme sketch

```kotlin
object DockTokens {
    // Dark
    val Bg = Color(0xFF0A0F1C); val Surface = Color(0xFF121A2B)
    val SurfaceRaised = Color(0xFF1A2338); val Outline = Color(0xFF28334D)
    val Dock = Color(0xFF4F8CFF); val OnDock = Color(0xFF06122B)
    val Seal = Color(0xFF34D399); val Attention = Color(0xFFFBBF24); val Danger = Color(0xFFFB7185)
}

object DockMotion {
    val Snap   = spring<Float>(dampingRatio = 0.72f, stiffness = 700f)
    val Settle = spring<Float>(dampingRatio = 0.85f, stiffness = 380f)
    val Flow   = tween<Float>(320, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
    val Exit   = tween<Float>(200, easing = CubicBezierEasing(0.3f, 0f, 1f, 1f))
}

val DockShapes = Shapes(
    small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(32.dp)
)
```
Material 3 is used as the component base, but color, shape and typography are fully overridden. Dynamic color is **off** (the brand colors carry meaning: green/amber/red).

## 8. Psychology-driven rules

| Principle | Where it applies in DockDrop |
|---|---|
| **Fitts's Law** | Primary actions (Start backup, Restore) are full-width, 56 dp, in the bottom thumb zone of a 6.6" screen. Destructive actions are never next to primary ones. |
| **Hick's Law** | Set creation is 4 steps; each step has one decision. v1 exposes 3 settings total (deletion policy, encryption, battery condition). |
| **Smart defaults** | Folder suggestions (Camera, Pictures, WhatsApp media, Documents, Downloads) pre-checked with size estimates; deletion = keep; encryption = off; battery ≥ 20%. |
| **Goal-gradient effect** | Segmented ring + folder counter + ETA range make progress feel near and real. |
| **Peak-end rule** | The Seal Receipt is the designed peak; error states end with a clear next action, not a dead end. |
| **IKEA / ownership effect** | The user names the Set, picks the folders, and watches them morph into their Set card. |
| **Loss aversion** | Keep-deleted is default; Mirror mode is red, previewed, and guarded. The Receipt shows "nothing was deleted from your phone". |
| **Recognition over recall** | Browse Restore as a folder tree with thumbnails/search; nobody types a path. |
| **Zeigarnik effect** | An interrupted Drop leaves a visible "Resume · 63% done" card on Home until sealed. |
| **Friction proportional to risk** | Hold-to-restore, Mirror confirmation, and preview sheets replace repeated "Are you sure?" dialogs. |
| **Trust signals** | "No internet permission" badge, plain-language verification level, "nothing leaves your devices" copy. |

## 9. Performance & motion budget (Galaxy A14 is the reference device)

- Target 60 fps. P90 frame time on the Drop screen ≤ 16.6 ms during an active Drop with particles on.
- Particles ≤ 24 on screen; one Canvas for the hero; animate using draw-phase state reads / `graphicsLayer`, never recomposition per frame.
- Throttle UI progress updates to 4-10 Hz even if the engine emits faster.
- All infinite animations pause when the activity is not `RESUMED` or the screen is off.
- No `RenderEffect` blur on API < 31; use alpha-only glow fallback.
- No bitmap assets in hero visuals (vector/Canvas only); APK target < 15 MB.
- Baseline Profile for startup and the Home → Drop path. Cold start to interactive Home ≤ 1.5 s on the A14.
- Perf checks run in Phase 1 (placeholder UI) and Phase 5 (final).

## 10. Trust & accessibility baseline

- Contrast ≥ 4.5:1 for text, 3:1 for large text/icons, in both themes.
- Never color-only: every state has an icon and words.
- TalkBack: each state has a spoken phrase (e.g. "Backing up. Camera folder, 3 of 4. About 7 minutes left."). Progress announcements every 10%, not every tick.
- Font scale to 200% without clipped or overlapping content; layouts reflow.
- Touch targets ≥ 48 dp, 8 dp apart.
- Edge-to-edge with proper insets (required on Android 15).
- Privacy copy lives in onboarding and Settings: what is read, that nothing is sent anywhere, and how to confirm it ("Settings → Apps → DockDrop → Permissions shows no network access").
- Primary UI avoids jargon (manifest, SAF, hash). Technical details live behind "Details".

## 11. What NOT to do

- No neon, no cyber-grid, no sci-fi hardware photography, no mascot.
- No confetti. The Seal is a latch, not a party.
- No fake or indeterminate progress. If we don't know, we say what we're doing.
- Never show green before verification passes.
- No modal stacking and no "Are you sure?" spam; use previews and holds.
- No jargon in primary copy; no emoji in system states.
- No runtime-downloaded fonts or assets (offline-only rule).
- Don't let animation block action: every animation is skippable or non-blocking.

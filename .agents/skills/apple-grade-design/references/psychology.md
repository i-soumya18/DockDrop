# Psychology-driven experience design

Read at step 3. Pairs with the `psychology-driven-ux` skill, which holds the six core principles and the pre-ship checklist. This file adds the Apple-lens guardrails and the order of operations.

## Order of operations

1. **Map the journey** at the level of moments: first touch, first value, commitment, habit, upgrade. Mark where drop-off risk is highest.
2. **Run the six principles** (smart defaults, goal-gradient, reciprocity, endowment/IKEA effect, loss aversion, contrast/anchoring) only at the moments where they apply. A settings screen does not need loss framing.
3. **Rewrite, don't annotate.** Change the defaults, sequence, copy and framing in the design itself.
4. **Run the honesty check** below before shipping.

## The Apple-grade filter on persuasion

Apple-level design persuades through *removal of friction and clarity of value*, rarely through pressure. Apply this filter to every technique:

| Technique | Apple-grade use | Not acceptable |
|---|---|---|
| Smart defaults | Pre-fill the likely answer and show the result ("See 12 results") | Pre-checking boxes that benefit you at the user's expense |
| Goal-gradient | Credit real steps already done | Fake 20% progress for doing nothing |
| Reciprocity | Show a real partial result before any gate | A gift that is useless without signing up |
| Endowment | Let people customize before account creation | Holding their created work hostage |
| Loss aversion | Name a genuine, specific loss (file deletion, expiring access) | Invented countdowns or scarcity |
| Anchoring | Show a real comparison tier or reference cost | Fake "was" prices |

If a screen only works because the user is misled, it fails this filter, and also tends to fail App Store review and user trust.

## Behavior design beyond the six

Use these when the six do not explain the problem:

- **Hick's law**: fewer simultaneous choices; progressive disclosure over settings dumps.
- **Peak-end rule**: design the best moment and the last moment of a flow (completion screen, post-purchase) with extra care.
- **Fitts's law and thumb reach**: primary actions in the natural thumb zone on phones; destructive actions away from it.
- **Feedback and control**: every action gets an immediate, proportionate response; every consequential action is undoable or confirmable.
- **Recognition over recall**: show options instead of asking people to remember them.

## Delight, done restrained

The awards treat delight as its own category, but delight in utility apps comes from speed, polish and thoughtfulness, not mascots. Good delight moments: a haptic that confirms a completed action, a transition that preserves spatial context, copy that anticipates the next question. Budget at most one signature moment per core flow.

## Output of this step

For each key flow: the sequence of screens, the principle applied at each moment (one line each), the default values, the gating decision (what is free, what is gated, why), and the honesty-check result.

## Pre-ship checklist

Use the six-item checklist from the `psychology-driven-ux` skill, plus: *"Would I be comfortable explaining this screen's persuasion technique to the user's face?"*

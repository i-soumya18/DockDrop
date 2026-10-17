# Critique

Read at step 6. Pairs with the `design-critique` skill (output template).

## Apple's cadence, as reported

Secondhand accounts describe these habits (treat as reported practice):

- **Many options, then fewer.** Explore around ten genuinely different directions, narrow to about three, then pick one ("10 to 3 to 1").
- **Show work early and often.** Weekly and even multiple-times-weekly reviews with the whole design team, then with senior leadership, where the point is decisions, not approval theater.
- **Pair designers with engineers** early to find out whether an idea can actually work.
- **Live on the product.** The team uses what it builds, every day, and judges it by use rather than by mockup.

You can borrow all four at any scale. Solo founder version: make three really different versions of the key screen before polishing any, use the build daily on your own phone, and ask one outsider to react weekly.

## Critique in layers (do not mix them)

Run these in order. Mixing layers is why critiques feel unfocused.

1. **Purpose and first impression (2 seconds).** What draws the eye first, is that correct, can a stranger say what this is for?
2. **Flow and usability.** Can the user finish the task? Where would they hesitate, backtrack or quit? Count taps and decisions.
3. **Hierarchy.** Reading order, emphasis, whitespace, typographic contrast. Squint test.
4. **Consistency and system fit.** Tokens, components, behavior parity.
5. **States.** Empty, loading, error, offline, first run, long text, huge text.
6. **Craft pass.** Alignment, optical balance, icon weight, radius nesting, shadow and blur restraint, motion timing and easing, haptics, copy rhythm.
7. **Accessibility.** Fold in the findings from `accessibility.md`.

## How to give feedback

- **Specific:** "The primary button and the tab bar compete for the same bottom edge", not "layout is busy".
- **Reasoned:** tie each note to a principle (clarity, deference, hierarchy, consistency) or a user need.
- **Constructive:** propose a fix with each problem.
- **Balanced:** name what works; it protects it from being "improved" away.
- **Staged:** exploration gets feedback on concept and flow; final polish gets feedback on craft. Never nitpick pixels on a concept that is still wrong.

## Severity scale (shared with accessibility)

- **Critical:** blocks task completion or excludes users; fix before any release.
- **Major:** causes confusion, error or clear quality loss; fix this cycle.
- **Minor:** polish; batch it.

## The "taste" checklist for Apple-level craft

- One clear hero per screen; everything else supports it.
- Generous, consistent spacing; density only where the task demands it.
- No more than two or three type sizes carrying the hierarchy on any one screen.
- Color used for meaning (action, status), not decoration; mostly neutral surfaces.
- Motion explains cause and effect and is interruptible.
- Touch feedback is immediate; nothing feels laggy or ambiguous.
- Every empty state is designed, with a next action.
- Nothing is there "because we could".

## Output

Use the `design-critique` template. Add a **Principle restored** column to the priority recommendations (which principle each fix returns the design to) and end with the three changes that would move the design furthest.

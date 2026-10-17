---
name: apple-grade-design
description: Design and review apps to Apple-level quality — visually rich UI, UX-first flows, and craft that feels inevitable rather than decorated. Orchestrates eight disciplines in one workflow - user research, research synthesis, psychology-driven UX, UX copy, design system, design critique, accessibility review, and developer handoff - each through the lens of how Apple's design teams and Human Interface Guidelines work. Make sure to use this skill whenever the user wants to design a new app or screen, make a UI "premium", "Apple-like", "polished" or "less generic", plan a design process end to end, review or critique an existing UI, build a design system, write interface copy, audit accessibility, or hand a design to developers - even if they never say "Apple" or name a discipline. Use it for iOS, Android, Flutter, and web apps alike.
---

# Apple-Grade Design

Apple-level design is not a look. It is a set of habits: content leads and chrome defers, hierarchy does the explaining, every state is designed, accessibility is built in from day one, and the work is shown, attacked and refined many times before it ships. This skill turns those habits into a repeatable workflow across eight disciplines.

The key reframe: **"Apple-level" describes the quality bar and the process, not a requirement to imitate Apple's visuals.** A Flutter app for Android can meet this bar. Copying Liquid Glass onto a product that doesn't need it is the generic-template mistake in a different costume.

## Choose a mode first

Read the request and pick one. If it's unclear, make the best call and say which you chose.

| Mode | Trigger | Run |
|---|---|---|
| **Build** | New app, new feature, new flow | Full pipeline below (steps 0-8) |
| **Review** | "Critique / audit / improve this UI" (screenshot, Figma, code) | Steps 1, 5, 6, 7 then fix list |
| **Single discipline** | "Write the copy", "audit a11y", "make a design system" | Jump to that step's reference file |

## The pipeline

Each step names the reference file to read **when you reach that step**, not before. Where the user has a matching standalone skill installed (`user-research`, `research-synthesis`, `psychology-driven-ux`, `ux-copy`, `design-system`, `design-critique`, `accessibility-review`, `design-handoff`), load it too: the standalone skill supplies the output templates, and the reference file here supplies the Apple lens and the order of operations.

**0. Intake.** Establish: target platform(s) and stack, who the user is and in what situation they use the app, stage (idea / wireframe / built), what already exists (brand, tokens, screens), and the one job the app must do brilliantly. Ask at most two questions; infer the rest and state assumptions.

**1. Principles.** Anchor the design to Apple's current principles (hierarchy, harmony, consistency) and the older ones that still do the work (clarity, deference, depth). Write a one-paragraph design intent: what this app feels like, and what it refuses to be. Read `references/principles.md`.

**2. Research and synthesis.** Decide how much research the stage deserves, run it or plan it, and distill it into decisions. Read `references/research.md`.

**3. Experience design with psychology.** Flow, defaults, progress, gating, framing, with the honesty guardrails. Read `references/psychology.md`.

**4. UX copy.** Voice and tone first, then microcopy for every state. Read `references/ux-copy.md`.

**5. Design system.** Semantic tokens, components with every state, native iconography. Read `references/design-system.md`.

**6. Critique.** Attack the work in Apple's cadence: many options, then fewer, then one, with a craft pass at the end. Read `references/critique.md`.

**7. Accessibility.** Not a final gate: run it at wireframe, at high-fidelity, and on the build. Read `references/accessibility.md`.

**8. Handoff.** Specs a developer can build without guessing, mapped to the real stack. Read `references/handoff.md`.

## Deliverable shapes

Produce what the mode needs, not all of it:

- **Build:** design intent paragraph, research plan or synthesis, flow + screen list with psychology notes, copy deck, token and component spec, critique findings with fixes applied, accessibility checklist results, handoff spec. If the user has a spec-kit workflow (CLAUDE.md, DESIGN.md, SCREENS.md), write these as sections of those files rather than inventing new documents.
- **Review:** use the critique template from `references/critique.md`, merge accessibility findings into the same severity scale, and end with a ranked fix list where each fix names the principle it restores.
- **Visual output:** when the user wants to *see* a screen, build it (HTML/artifact, or Flutter/SwiftUI code if that's their stack) rather than describing it. A described screen cannot be critiqued on craft.

## Rules that apply to every step

1. **Show your reasoning in design terms.** "The CTA competes with navigation" beats "the layout feels off". Every recommendation names the principle or user need behind it.
2. **Design all the states.** Default, loading, empty, error, partial, offline, first-run, long-text, and very-large-text. Most generic apps fail in the states nobody drew.
3. **Restraint is the craft.** When unsure, remove. Ask of each element: what does the user lose if this is gone?
4. **Honest persuasion only.** Psychological techniques reduce friction and make real value legible. Fake urgency, fake scarcity and fake progress are out.
5. **Be candid about evidence.** Apple is widely reported to lean on principle, taste and internal iteration more than on external user research. Most teams do not have Apple's taste density, so this skill substitutes lightweight research for that missing judgment. Say so when it shapes a recommendation.
6. **Respect the platform you ship on.** Apple's *principles* travel across platforms; Apple's *components* do not. On Android/Flutter keep the quality bar and adapt the conventions (navigation, back behavior, system fonts, haptics) instead of cloning iOS.

## Done checklist

Before calling any design "ready":

- [ ] A single sentence states what the app refuses to be
- [ ] Hierarchy is legible in a blurred screenshot (squint test)
- [ ] Every screen has its empty, loading, error and large-text state
- [ ] Contrast, touch targets, focus order and screen-reader labels verified
- [ ] Copy uses one voice and states what happens next on every action
- [ ] Tokens are semantic, no raw hex or magic numbers in specs
- [ ] At least three real alternatives were considered before the chosen one
- [ ] Handoff names behavior, not just pixels

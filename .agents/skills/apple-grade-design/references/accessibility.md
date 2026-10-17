# Accessibility

Read at step 7, and run it three times: wireframe, high-fidelity, and built app. Pairs with the `accessibility-review` skill (output template).

## Apple's stance

Apple's inclusive-design guidance treats accessibility as design practice, not compliance: design with a *spectrum* of ability in mind (a useful reminder from Apple's own sessions is to say "some people" rather than "people with disabilities", which keeps the full range of permanent, temporary and situational limits in view), support multiple ways to interact, and adopt the platform accessibility APIs. The Apple Design Awards include an Inclusivity category for exactly this, and Apple has introduced accessibility "nutrition labels" in the App Store for declaring supported features. Inclusion also covers language, imagery and marketing, not only controls.

## Core requirements

| Area | Requirement | Note |
|---|---|---|
| Touch targets | **44 x 44 pt** minimum for tappable elements (Apple HIG) | Spacing of about 8 pt between targets prevents mis-taps |
| Contrast | **4.5:1** normal text, **3:1** large text and UI components | Check in light, dark and increased-contrast modes |
| Text size | Support the user's text size setting, including the largest accessibility sizes | Layout must reflow, never clip or truncate meaning; text can scale to roughly 200% |
| Screen reader | Every interactive element has a meaningful label, role and value | Decorative images hidden from the reader |
| Color | Never the only carrier of meaning | Pair with icon, text or shape |
| Motion | Respect Reduce Motion; no essential info in animation alone | Offer a crossfade fallback |
| Transparency | Respect Reduce Transparency / Increase Contrast | Blur and glass need opaque fallbacks |
| Input | Operable by switch, voice control and keyboard where the platform allows | Logical focus order, visible focus indicator |
| Errors | Identify the error in text and say how to fix it | Never color alone |
| Timing | No unexpected timeouts; allow extension | |

**A note on target sizes:** the 44 pt figure is Apple's platform guidance and WCAG's *enhanced* (AAA) target-size criterion. WCAG 2.2's AA criterion is a lower floor (24 x 24 CSS px with spacing). If a template in the `accessibility-review` skill labels 44 px as an AA requirement, correct it when reporting: design to 44, but classify a 24-44 px target as a "major" miss rather than an AA failure.

## Process

1. **Wireframe stage:** check hierarchy, reading and focus order, target sizes, and whether any meaning depends on color or hover.
2. **High-fidelity stage:** compute every text and component contrast pair (list the pairs, do not eyeball them), test dark mode, simulate largest text size on the three densest screens.
3. **Build stage:** run the platform inspector or automated scan (it catches only a fraction of issues), then **manually** test with the screen reader, largest text, Reduce Motion, Increase Contrast, keyboard or switch navigation, and 200% zoom.
4. **Regress:** re-test after each release; add the checks to the definition of done.

## Contrast calculation

Contrast ratio = (L1 + 0.05) / (L2 + 0.05) with L1 the lighter relative luminance. When asked to verify colors, compute the ratios in code and list foreground, background, ratio, requirement and pass/fail, instead of estimating.

## Screen reader label patterns

- Name what it does, not what it looks like: "Delete note", not "Trash icon".
- Do not include the role in the label ("Save", not "Save button").
- Group related elements so one swipe covers one concept (a card reads as a single item).
- Announce dynamic changes (toasts, validation, loading finished).
- Make custom controls expose value and state ("Volume, 40 percent, adjustable").

## Output

Use the `accessibility-review` template with severity (Critical / Major / Minor), and add: the contrast table with computed ratios, the screen-reader announcement table for key screens, and a "tested with" line listing which assistive technologies were actually used. State plainly which findings are design-review inferences and which were verified on a device; an inferred pass is not a verified pass.

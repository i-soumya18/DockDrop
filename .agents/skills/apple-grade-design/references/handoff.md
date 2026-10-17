# Handoff

Read at step 8. Pairs with the `design-handoff` skill (spec template).

## Principle: specify behavior, not just pixels

The developer will guess at anything unspecified, and guesses are where Apple-level quality leaks away. Reference tokens, not values; describe *why*, so developers can make good judgment calls in cases the spec missed.

## What a complete spec contains

1. **Overview:** user context and the job of the screen.
2. **Layout:** grid, safe areas, adaptive behavior across sizes and orientations, scroll behavior, pinned elements.
3. **Tokens used:** role names (color, type, spacing, radius, motion), not hex or pixel numbers.
4. **Components:** variant, props, and notes for each instance.
5. **States matrix for every interactive element:** default, pressed, focused, disabled, loading, error, selected, success.
6. **Content rules:** character limits, truncation (what is never truncated), empty, partial and error content, localization expansion, dynamic text sizes.
7. **Interaction:** gestures, haptics (which feedback type, when), transitions with duration and curve, interruptibility (can the user cancel mid-animation), keyboard and pointer behavior where relevant.
8. **Motion:** table of element, trigger, animation, duration, easing, and the Reduce Motion alternative.
9. **Accessibility:** focus order, labels, roles, traits, announcements, minimum target sizes.
10. **Edge cases:** zero, one, many (hundreds), very long strings, offline, slow network, permission denied, interrupted flows, first run vs returning user.
11. **Analytics events** (if tracked), named consistently.
12. **Acceptance criteria:** testable statements the developer and designer will both check ("At the largest text size the primary button remains fully visible without horizontal scroll").

## Mapping to the stack

State the user's stack at the top and translate once, so no one has to:

- **SwiftUI:** color roles as named assets, type as text styles that scale, spacing constants, SF Symbols by name (never by character code), system components wherever they fit (custom only where the product's identity requires it).
- **Flutter:** ThemeData and ThemeExtension for roles, TextTheme for type, const spacing and radius classes, one icon source, respect `MediaQuery` text scaling and `disableAnimations`.
- **Web:** CSS custom properties for tokens, `prefers-color-scheme`, `prefers-reduced-motion`, `prefers-contrast`, fluid type with `clamp()`, focus-visible styles.

If design tooling is connected (a design file with variables and components), pull measurements and tokens from it instead of retyping.

## Handoff ritual

- Walk the developer through the spec live for any flow that includes motion or gesture; paste a short recorded prototype for each.
- Agree a "design QA" pass on the built app before release: designer checks states, motion, text scaling and contrast on a real device.
- Keep the spec next to the code (or linked from the ticket) and update it when behavior changes.

## Output

A **handoff spec per screen or flow** using the `design-handoff` template, extended with the content rules, haptics and motion tables, Reduce Motion alternatives, acceptance criteria, and the stack mapping. If the user keeps a SCREENS.md / DESIGN.md convention, write the spec as sections in those files.

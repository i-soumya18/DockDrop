# Design system

Read at step 5. Pairs with the `design-system` skill (audit / document / extend templates).

## How Apple frames it

Apple does not publish a token repository the way some design systems do; its system is the Human Interface Guidelines plus system components, system fonts and a native icon library, with downloadable design kits for common design tools. The transferable lesson: **design to semantic roles, not raw values.** Apple platforms express colors as roles (label, secondary label, background, system blue) that adapt automatically to light, dark and high-contrast modes. Do the same in any stack.

## Build order

1. **Foundations (tokens), semantic first**
   - *Color roles:* background (primary, secondary, grouped), label (primary, secondary, tertiary), separator, accent, and status (success, warning, destructive). Define each for light, dark and increased contrast. Specs reference roles, never hex.
   - *Type scale:* a small named scale (large title, title, headline, body, callout, subheadline, footnote, caption) that scales with the user's text size setting. Use the platform system font unless brand requires otherwise.
   - *Spacing:* one base unit (4 or 8) and a short named scale. Consistent multiples make spacing decisions automatic.
   - *Shape:* a few corner radii, nested concentrically (inner = outer minus padding).
   - *Elevation and material:* two or three levels at most.
   - *Motion:* a small set of durations and curves, each tied to a meaning (appear, move, dismiss), with a Reduce Motion alternative.
2. **Iconography:** one family with consistent weight and optical alignment, matched to text weight. On Apple platforms use SF Symbols and refer to them by name. On other platforms pick one set and never mix.
3. **Components:** build the small set the product needs, not a library. For each, define variants, sizes and **every state**: default, pressed, focused, disabled, loading, error, selected, and (for content components) empty and long-text.
4. **Patterns:** forms, navigation, lists and detail, feedback (toast, banner, sheet, alert), onboarding, permission request, paywall.
5. **Documentation:** description, when to use, anatomy, props, states, accessibility notes, do/don't, code reference.

## Rules

- **80% coverage beats a perfect 10 components.** Document as you build.
- **Fewer, stronger components.** If two components differ only slightly, merge them.
- **Hit-target size lives in the component, not the screen.** Bake the 44 pt minimum into buttons and rows so designers cannot violate it.
- **Dark mode and large text are first-class**, not variants tacked on later.
- **Constraint over flexibility.** A system exists so teams do not reinvent; add a prop only when two real screens need it.
- **Version and migrate.** Breaking changes ship with a migration note.

## Audit mode checklist

Scan screens or code for: hardcoded colors, arbitrary spacing values, custom font sizes that bypass the scale, inconsistent component names, components missing states, and icons outside the chosen family. Report counts, then rank fixes by how many screens each affects.

## Stack mapping (so the system survives implementation)

| Token type | SwiftUI | Flutter | Web |
|---|---|---|---|
| Color role | named asset / Color extension | ThemeData ColorScheme + ThemeExtension | CSS custom properties |
| Type | Font text styles | TextTheme | CSS variables + clamp() |
| Spacing | constants enum | const spacing class | CSS variables |
| Radius | constants | BorderRadius constants | CSS variables |
| Icon | SF Symbols by name | one icon font or SVG set | one SVG sprite |

## Output of this step

A **design-system spec**: token tables (role, light, dark, high-contrast), type scale, spacing and radius scales, motion table, component inventory with states, and the stack mapping above for the user's stack.

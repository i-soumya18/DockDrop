# Principles: the Apple quality bar

Read at step 1. Output: a one-paragraph **design intent** and a **"refuses to be"** sentence.

## Two generations of principles, both useful

| Era | Principles | What they mean in practice |
|---|---|---|
| Classic HIG | **Clarity, Deference, Depth** | Text legible at every size, icons precise, adornment subtle. The UI serves content and never competes with it. Layers and motion convey hierarchy and give the interface life. |
| Liquid Glass era (WWDC25) | **Hierarchy, Harmony, Consistency** | Controls and navigation form a distinct functional layer floating above content. UI shapes echo the curvature of the hardware (concentricity). Same things look and behave the same, across platforms, without forcing uniformity. |

Use the classic set to judge any screen on any platform. Use the newer set when designing for current Apple platforms or when the user asks about Liquid Glass.

## How to turn principles into decisions

- **Clarity test:** Blur or squint at the screen. Can you still tell what the primary thing is and what to do next? If not, hierarchy is failing; fix with size, weight, spacing and position before adding color.
- **Deference test:** List every non-content element on the screen (borders, cards, badges, gradients, shadows). For each, ask what breaks if it is removed. Remove those that fail.
- **Depth test:** Does motion or layering explain *where you are* and *what just happened*? Transitions that only decorate fail this test.
- **Consistency test:** Do two things that look alike behave alike? Do two things that behave alike look alike?
- **Harmony test:** Do corner radii nest concentrically (inner radius = outer radius minus padding)? Does content respect the safe areas and device shape?

## If you use glass or translucency

Reported guidance from the Liquid Glass material documentation: reserve it for elements that float above content (toolbars, tab bars, sheets, controls); do not put it inside scrollable content such as list rows; do not stack glass on glass; make sure content behind it keeps text legible. It adapts automatically to Reduce Transparency and Increase Contrast on Apple platforms; custom implementations elsewhere must do this themselves. Outside Apple platforms, only use a glass effect if it carries hierarchy meaning, not as a style statement.

## What the Apple Design Awards reward (a useful rubric)

The awards recognize six qualities, each a lens you can score a design against:

1. **Delight and fun**: memorable, satisfying experiences
2. **Inclusivity**: a great experience across backgrounds, abilities and languages
3. **Innovation**: novel use of platform technology that sets the product apart
4. **Interaction**: intuitive interfaces and effortless controls suited to the platform
5. **Social impact**: improving lives in a meaningful way
6. **Visuals and graphics**: beautiful imagery, skilled interface drawing, top-notch animation with a cohesive theme

Pick the two or three that matter most for this app and design toward them deliberately. An app trying to win all six reads as unfocused.

## Writing the design intent

Template (keep it to five sentences):

> [App] is [what it feels like, in two or three concrete adjectives] for [specific person] when they are [specific situation]. Its one job is [job]. Content leads: [what the hero content is]. It refuses to be [the generic thing it will not become]. We will know it works when [observable behavior, not a feeling].

"Refuses to be" is the most useful line: it gives the team a veto against the default patterns (generic cards, gradient hero, dashboard of everything).

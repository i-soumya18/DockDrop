# UX copy

Read at step 4. Pairs with the `ux-copy` skill (templates for CTAs, errors, empty states, confirmations).

## Apple's frame: voice is constant, tone adapts

Apple's own UX-writing guidance (Human Interface Guidelines "Writing", the WWDC "Add personality to your app through UX writing" session, and the Apple Style Guide) teaches one idea that resolves most copy debates: **voice** is the personality that stays the same everywhere; **tone** is how that voice adapts to the user's situation (celebrating, failing, waiting, being warned).

## Step 1: Define voice before writing any microcopy

Pick three to five voice attributes and give each a *do / don't* pair. Abstract adjectives alone fail because two writers will interpret "friendly" differently.

| Attribute | Do | Don't |
|---|---|---|
| Plain | "Your photos are backing up." | "Initiating synchronization of media assets." |
| Calm | "Couldn't connect. Check your internet and try again." | "Oops!! Something went wrong!!!" |
| Respectful | "Turn on notifications to get reminders." | "Don't miss out!" |

## Step 2: Build a tone map

Define tone for each situation, at minimum: **success, error, destructive action, empty state, loading, permission request, first run.**

| Situation | Tone | Example |
|---|---|---|
| Success | Warm, brief | "Backup complete." |
| Error | Calm, practical, blameless | "Couldn't save. You're offline. We'll retry when you're back." |
| Destructive | Serious, precise | "Delete 3 files? This can't be undone." |
| Empty | Inviting, instructive | "No habits yet. Add your first to start a streak." |
| Permission | Specific, honest | "Allow notifications so we can remind you at 8 PM." |

## Rules that hold up under review

1. **Verb-first, specific CTAs.** "Save changes", "Create account", never "Submit" or "OK".
2. **Buttons name the outcome.** Destructive dialogs use "Delete files / Keep files", not "Yes / No".
3. **Errors answer three questions:** what happened, why (if known), what to do next. Never blame the user.
4. **Every action states what happens next.** Reduces anxiety, especially around payment, deletion and permissions.
5. **One idea per unit; one term per concept.** If it is "project" on one screen, it is never "workspace" on another.
6. **Sentence case, active voice, address the user as "you".** Pick one convention for the product's own name for itself ("we" vs the product name) and keep it.
7. **Write inclusively.** Plain language, no idioms that do not translate, no assumptions about gender, ability or culture. Prefer universal experiences to culture-specific references.
8. **Read it aloud and run it through VoiceOver mentally.** Labels must make sense out of visual context ("Delete note", not "Delete").
9. **Design for expansion.** Translated strings can be considerably longer; leave room and never truncate the meaningful part.
10. **Copy is designed with the screen, not after.** Retrofit copy is almost always worse.

## Permission prompts

Apple platforms require a usage description for sensitive permissions, and the best ones are concrete: say what the feature does and what data it uses. Ask in context, at the moment of need, after showing the value, not on first launch.

## Output of this step

A **copy deck**: voice attributes with do/don't, tone map, then a table of every string (screen, element, copy, character budget, notes). Include all states, not only the happy path. Add a short localization-notes column where strings are idiomatic or length-sensitive.

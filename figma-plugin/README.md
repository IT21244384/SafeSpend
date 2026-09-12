# SafeSpend Design Builder — a Figma plugin

Generates the entire SafeSpend design file in one run: colour styles grouped by their role in the 60‑30‑10 rule, text styles, a component page, eight screen frames, a design‑system board, and the prototype links between the screens.

Everything it creates is ordinary, editable Figma — frames, auto‑layout, styles, components. Nothing is flattened or locked, so you can rearrange, restyle and extend it afterwards.

Building it from code rather than by hand means the file and the app agree: the hex values, type sizes, corner radii and spacing here are the same ones in `app/src/main/java/com/safespend/app/ui/theme/`.

---

## Running it

Local plugins can only be loaded by the **Figma desktop app** — the browser version has no way to import a manifest from disk. The desktop app is free, and the file it produces is a normal cloud file you can open in the browser afterwards.

1. Install the Figma desktop app — <https://www.figma.com/downloads/> — and sign in.
2. **File → New design file.** Run this on an empty file; it creates its own pages.
3. Menu (top‑left) → **Plugins → Development → Import plugin from manifest…**
4. Select `figma-plugin/manifest.json` from this repository.
5. Menu → **Plugins → Development → SafeSpend Design Builder**.

It takes a few seconds. When it finishes you'll see a confirmation message and the viewport will zoom to the screens.

---

## What you get

**Pages**

| Page | Contents |
|---|---|
| `Screens` | The design‑system board plus eight 412 × 915 screen frames, wired for prototyping |
| `Components` | Twelve named components — badge, progress track, stat tile, chips, transaction row, field, buttons, section header |

**Screens** — Home, Ledger, Add transaction, Paste a bank message, Budgets, Insights, Savings goals, Settings.

**Styles** — 21 colour styles and 11 text styles. The colour styles are deliberately named by their role in the ratio, so the 60‑30‑10 decision is visible in the styles panel itself rather than something a reviewer has to infer:

```
60 Neutral/Mist, Surface, Ink, Muted, Line
30 Brand/Deep Teal, Teal Container, Teal Deeper
10 Accent/Coral, Coral Container, Coral on Brand
Data/Income, Expense, Category 1–8
```

**Prototype** — 28 links: the four bottom‑bar tabs interconnect, the FAB opens Add transaction, the paste button opens the sheet, back arrows return home, and Home's section links reach Insights, the Ledger, Goals and Settings. Home is set as the flow starting point, so **Present** works immediately.

---

## After it runs

A few things are worth doing by hand, because they're judgement calls rather than geometry:

- **Check the emoji glyphs.** Category icons are emoji standing in for Material Symbols. If you want the real icons, install the *Material Symbols* plugin and swap them — it'll look sharper, though emoji are perfectly acceptable for a prototype.
- **Present it once** (▶ top right) and click through every link, so you know the flow holds together before anyone else does.
- **Screenshot the design‑system board** for the report — it's the clearest single piece of evidence for the 60‑30‑10 criterion.
- **Add a dark‑theme page** if you want to show it: duplicate the `Screens` page and swap the five neutral colour styles for the dark values in `docs/02-ui-design-spec.md` §2.4. The brand and accent roles stay where they are.

## Re-running it

The plugin always creates new pages, so running it twice gives you two copies. If you want a clean rebuild, delete the `Screens` and `Components` pages first — or just run it in a fresh file.

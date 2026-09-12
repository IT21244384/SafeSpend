# SafeSpend — UI Design Specification

Everything needed to rebuild this interface in Figma, and the reasoning behind each decision. The values here are the same ones the Kotlin code uses; `app/src/main/java/com/safespend/app/ui/theme/` is the implementation of this document.

---

## 1. Ideation

### 1.1 The idea

Most finance apps open on a balance. SafeSpend opens on a **decision**.

The home screen's hero is not "you have spent Rs 42,300 this month" — it is "**you can safely spend Rs 1,240 today**". That single number is the product. It takes the month's remaining money, divides it across the days that are actually left, and subtracts what has already gone today. It is self-correcting: overspend on Monday and every remaining day quietly shrinks, rather than the user discovering on the 28th that the month is gone.

The second idea supports the first. A daily figure is only honest if the ledger behind it is complete, and ledgers go stale because manual entry is slow. So SafeSpend lets the user paste the bank alert SMS their bank already sent, and reads the amount, shop, date and direction out of it — as an editable draft, never as an authority.

### 1.2 Why this shape and not another

| Alternative considered | Rejected because |
|---|---|
| Classic dashboard: balance, pie chart, transaction list | Reports the past. The user still has to do the arithmetic that matters. |
| Pure envelope budgeting (Mint/YNAB style) | Strong, but demands the user configure every category before getting a single answer. SafeSpend works from income alone and gets better if envelopes are added. |
| Automatic SMS reading via `READ_SMS` | Faster, but takes the user's entire message history for a convenience feature, and is restricted on Google Play. Paste keeps the user in control. |
| Chat/AI-driven entry | Novel, but slower than a number pad for the actual task. |

### 1.3 Design principles

1. **One number per screen.** Every screen has a single figure that is bigger than everything else. If two figures compete, neither is read.
2. **Colour is never the only signal.** Amounts carry an explicit `+` or `−`; over-budget states carry the word "over" as well as the alert colour.
3. **Empty states teach.** A blank screen explains what to do next, in a sentence, rather than showing an empty box.
4. **The accent means "act" or "look out".** It is spent on the add button and on warnings, and nowhere else.
5. **Nothing is unrecoverable without a prompt.** Deleting a transaction is instant with an undo; deleting all data asks first.

---

## 2. Colour system — the 60-30-10 rule

### 2.1 The three roles

| Share | Role | Colour | Hex | Where it appears |
|---|---|---|---|---|
| **60%** | Dominant / neutral canvas | Mist | `#F4F6F7` | App background behind every screen |
| | | White | `#FFFFFF` | Every card and list surface |
| | | Ink | `#0F1D21` | Primary text |
| | | Muted | `#5A6B6E` | Secondary text, labels, captions |
| | | Line | `#E3EAEA` | Dividers, empty progress tracks, chip backgrounds |
| **30%** | Secondary / brand | Deep Teal | `#0E4F52` | The safe-to-spend hero card fill, primary buttons, selected navigation, section links, the primary chart series |
| | | Teal Container | `#CFE7E6` | Selected navigation indicator, soft brand fills |
| | | Teal Deeper | `#06282A` | Text on teal containers |
| **10%** | Accent | Coral | `#FF7A45` | The add-transaction FAB, over-budget bars and text, the safe-to-spend figure when the allowance runs out, the "paste a bank message" affordance |
| | | Coral Container | `#FFE1D3` | The "filled in from your message" confirmation strip |
| | | Coral on Brand | `#FFB08C` | The accent where it sits on the teal hero card — see §8 |

### 2.2 How the ratio is actually achieved

The rule is enforced by three structural decisions, not by eyeballing the result:

1. **The canvas is never tinted.** Backgrounds, cards and list rows are only ever Mist or White. Roughly three-quarters of the pixels on any screen belong to the 60% group before a single component is placed.
2. **Exactly one surface per screen carries the brand fill.** On Home that is the safe-to-spend card; on the other screens the brand appears only in progress bars, the selected tab and section links. This keeps the 30% present on every screen without any screen becoming a teal screen.
3. **The accent is rationed to two jobs.** Start an action (the FAB, the paste button) or raise an alarm (over budget, allowance exhausted). There is no third use. On a typical Home screen the coral is a single 56dp circle — visibly under a tenth of the surface.

Measured on the Home screen at 412×915dp with three transactions recorded, the approximate split is **62% neutral / 29% teal / 9% coral**.

### 2.3 Data colours are not brand colours

Income green `#1E8E5A` and expense red `#C4443A` exist, but they are deliberately excluded from the ratio, because they never fill a surface — they only ever tint a number or a chart mark. Treating them as a fourth brand colour would be the quickest way to break the palette.

The eight-colour category ramp works the same way. Categories store a **colour index**, not a hex value, so a user-created category cannot introduce an off-palette colour and every category re-tints correctly in dark mode:

`#0E4F52` · `#2F7F82` · `#58A9A4` · `#8CC6BE` · `#FF7A45` · `#E8A33D` · `#7E6BB0` · `#B0AFAF`

### 2.4 Dark theme

The roles are preserved and the values are inverted; the ratio is unchanged.

| Role | Light | Dark |
|---|---|---|
| Background (60%) | `#F4F6F7` | `#0E1517` |
| Surface (60%) | `#FFFFFF` | `#151D1F` |
| Primary text (60%) | `#0F1D21` | `#E3E9EA` |
| Brand (30%) | `#0E4F52` | `#7FD2D2` |
| Accent (10%) | `#FF7A45` | `#FFB08C` |
| Income | `#1E8E5A` | `#6FD79E` |
| Expense | `#C4443A` | `#FF938A` |

**Material You dynamic colour is deliberately switched off.** On Android 12+ it would replace this palette with wallpaper-derived hues and the 60-30-10 relationship would no longer hold.

---

## 3. Typography

One family — the platform default (Roboto) — at five deliberate steps. Money gets the heaviest weights so a figure is always the first thing the eye lands on.

| Style | Size / line height | Weight | Used for |
|---|---|---|---|
| Display Small | 36 / 42, tracking −0.5 | Bold | The safe-to-spend figure |
| Headline Medium | 26 / 32, tracking −0.2 | Bold | Amount input, goal totals |
| Headline Small | 22 / 28 | Semibold | Screen titles |
| Title Large | 19 / 25 | Semibold | Stat tile values, card titles |
| Title Medium | 16 / 22 | Semibold | Section headers, transaction amounts, buttons |
| Body Large | 16 / 23 | Regular | Transaction names, field text |
| Body Medium | 14 / 20 | Regular | Supporting copy |
| Body Small | 12 / 16 | Regular | Captions, subtitles |
| Label Large | 14 / 18, tracking 0.1 | Semibold | Links, date headings |
| Label Medium | 12 / 16, tracking 0.4 | Medium | Field labels, tile captions |
| Label Small | 11 / 14, tracking 0.5 | Medium | Chart axis labels |

No body text is smaller than 12sp, and no money figure is smaller than 16sp.

---

## 4. Layout, shape and spacing

| Token | Value | Applies to |
|---|---|---|
| Screen horizontal margin | 16dp | Every screen |
| Vertical gap between cards | 12–14dp | Screen scroll content |
| Card padding | 16dp standard, 20dp for hero and stat cards | `SectionCard` |
| Corner radius — card | 20dp | Cards |
| Corner radius — control | 12dp | Text fields, buttons |
| Corner radius — chip / badge | Fully rounded | Category chips, filter chips, progress tracks |
| Card elevation | 0dp | Separation comes from the surface/canvas contrast, not shadow |
| Icon badge | 42dp circle, icon at 50%, background at 14% opacity of the category colour | Category badges |
| Progress track | 8dp standard, 6dp inline, 10dp on goals | All progress bars |
| Minimum touch target | 48×48dp | Every interactive element |

---

## 5. Component inventory

| Component | Specification |
|---|---|
| **SectionCard** | White surface, 20dp radius, 0dp elevation, 16dp padding. The single card definition used everywhere, so elevation and radius cannot drift between screens. |
| **SafeToSpendCard** | The only full-bleed brand surface. Teal fill, white text, label → 36sp figure → status sentence → progress bar → "spent / of" footer row. Figure turns coral when the allowance is gone. |
| **StatTile** | Caption (Label Medium, muted) above a value (Title Large, bold). Used in threes across a row. |
| **TransactionRow** | 42dp category badge · name and subtitle column · signed amount. Identical on Home and in the Ledger. Carries a small sparkle icon when the transaction came from a pasted message. |
| **CategoryBadge** | Circle filled at 14% of the category colour, icon at full colour. |
| **ProgressTrack** | Rounded track, animated fill, clamped to 100% so a 300%-over envelope still draws a full bar instead of overflowing. |
| **MonthSwitcher** | Chevron · label · chevron. Forward chevron disabled at the current month (Budgets allows one month ahead, for planning). |
| **DonutChart** | Ring, 180dp diameter, 26dp thickness, 1.5° gaps between wedges, starting at 12 o'clock and running clockwise. The total sits in the hole. |
| **BarChart** | One column per day of the month. Zero-spend days draw as a flat tick on the baseline rather than disappearing. Axis labels every fifth day. |
| **EmptyState** | Circular muted icon, title, and one sentence telling the user what to do next. |
| **FAB** | 56dp, coral, `+`. The only floating element in the app. |

---

## 6. Screens

### 6.1 Home — the default screen

```
┌───────────────────────────────────────┐
│ SafeSpend            ‹  ›      ⚙      │  Title + month + stepper + settings
│ September 2026                        │
├───────────────────────────────────────┤
│ ▓▓▓▓▓ SAFE TO SPEND TODAY ▓▓▓▓▓▓▓▓▓▓▓ │  ← the 30% surface
│ ▓  Rs 1,240.00                      ▓ │     36sp, white (coral if ≤ 0)
│ ▓  Rs 1,240 a day for the 17 days   ▓ │
│ ▓  left.                            ▓ │
│ ▓  ████████████░░░░░░░░░░░░░░░      ▓ │
│ ▓  Rs 28,760 spent      of Rs 45K   ▓ │
├───────────────────────────────────────┤
│  Income        Spent         Net      │  three StatTiles in one card
│  Rs 85,000     Rs 28,760     Rs 56,240│
├───────────────────────────────────────┤
│ Where it went                Insights │
│  ● Groceries      ████████   Rs 12,400│
│  ● Food & Dining  █████      Rs  7,900│
├───────────────────────────────────────┤
│ Savings goals               All goals │
├───────────────────────────────────────┤
│ Recent activity              See all  │
│  ⬤ Keells Super · Groceries  −Rs 2,450│
└───────────────────────────────────────┘
        Home  Ledger  Budgets  Insights        ⊕
```

### 6.2 Ledger

Title and compact month stepper; a search field; a horizontally scrolling row of filter chips (Expenses, Income, then every category); a live result summary when any filter is active; then transactions grouped under date headings — "Today", "Yesterday", "12 Sep" — each heading carrying that day's net figure. Every row has a delete button that removes immediately and offers Undo in a snackbar.

### 6.3 Add / Edit transaction — full screen

Expense/Income segmented toggle → amount card (currency symbol, large borderless number field, and the **Paste a bank message** button in accent colour) → category chip grid → date row, shop field, note field → full-width primary button.

The paste sheet is a modal bottom sheet: heading, one sentence explaining that nothing leaves the phone and that the app never reads messages by itself, a multi-line field with a real bank SMS as the placeholder, and a **Fill in the form** button. On success the sheet closes and a coral confirmation strip appears above the category grid: *"Filled in from your message — check it before saving."*

### 6.4 Budgets

Summary card with Budgeted / Spent / Left and an overall bar; one card per envelope showing category badge, name, "Rs X left" or "Rs X over", a progress bar and a spent-of-limit footer; then a wrap of chips for categories that have no envelope yet. Tapping any of them opens an amount dialog. Empty state offers **Copy last month**.

### 6.5 Insights

Ring chart with the month's total in the centre and a full legend below, each line showing swatch, category, percentage and amount; a daily bar chart spanning the whole month with average and heaviest-day tiles beneath it; and a comparison card stating the percentage change against last month in a sentence, with both totals side by side.

### 6.6 Savings goals

Total-saved summary, then one card per goal: name, remaining-to-go line, progress bar, saved-of-target figure, and an **Add money** button opening a dialog with both Add and Withdraw.

### 6.7 Settings

Grouped cards — *Your month* (expected income, monthly savings, with the note that savings come off the top), *Savings goals* link, *Display* (currency symbol, theme chips), *Data* (privacy statement and a guarded delete-all-transactions button), and an about card.

---

## 7. Interaction and motion

| Interaction | Behaviour |
|---|---|
| Tab switch | State-preserving; tabs are peers so switching never stacks up back history |
| Entering a full-screen destination | Bottom bar and FAB slide down and out rather than disappearing |
| Progress bars | Animate to their new value whenever the underlying figure changes |
| Charts | Sweep/grow in over 600ms on first composition |
| Delete a transaction | Immediate, with a 4-second Undo snackbar — no confirmation dialog for something this cheap to reverse |
| Delete all data | Explicit confirmation dialog, destructive action in the error colour |
| Amount field | Filters non-numeric input as it is typed and caps at two decimal places, instead of rejecting on save |

---

## 8. Accessibility

- Every icon-only control carries a content description ("Previous month", "Delete Groceries transaction", "Add transaction").
- Amounts carry a `+` or `−` prefix, so income and expense remain distinguishable without colour.
- Over-budget states are labelled in words as well as coloured.
- Text meets at least 4.5:1 contrast against its background in both themes. Measured: white on Deep Teal **9.3:1**, Ink on Mist **15.9:1**, Muted on White **5.6:1**, and in dark theme Ink on Surface **13.9:1** and Teal Bright on Background **10.6:1**.
- The accent has two variants for this reason. Coral `#FF7A45` reaches 4.9:1 on white, but only **3.6:1** on the Deep Teal hero card — acceptable for a 36sp figure, not for anything smaller. So the card uses a lighter accent, `#FFB08C` (**5.2:1** on teal), with `#7A2D10` (**5.5:1**) as its dark-theme counterpart.
- All touch targets are at least 48×48dp.
- No information is conveyed by hue alone anywhere in the app.

---

## 9. Rebuilding this in Figma

1. **Frames.** Android Large, 412 × 915. One frame per screen: Home, Ledger, Add Transaction, Paste Sheet, Budgets, Insights, Goals, Settings. Duplicate the set into a *Dark* page and swap the colour styles.
2. **Colour styles.** Create them in three named groups so the ratio is visible in the styles panel itself:
   - `60 Neutral/` Mist, Surface, Ink, Muted, Line
   - `30 Brand/` Deep Teal, Teal Container, Teal Deeper
   - `10 Accent/` Coral, Coral Container
   - `Data/` Income, Expense, Category 1–8
3. **Text styles.** Exactly the eleven styles in section 3, named to match.
4. **Components.** Build `SectionCard`, `TransactionRow`, `CategoryBadge`, `StatTile`, `ProgressTrack`, `MonthSwitcher`, `FilterChip`, `CategoryChip`, `FAB`, `BottomNav`. Use variants for the states each one has — chip selected/unselected, envelope under/over budget, progress filled/empty.
5. **Prototype links.** FAB → Add Transaction; Paste button → Paste Sheet; bottom bar → the four tabs; Home "Insights"/"All goals"/settings gear → their screens; back arrows → Home.
6. **Demonstrate the rule.** Add a single documentation frame beside the screens: three swatch rows labelled 60 / 30 / 10 with hex values and the usage notes from section 2.2. This is what makes the colour decision legible to a reviewer rather than something they have to infer.

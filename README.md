# SafeSpend

**A personal finance tracker that answers the question people actually ask: *can I afford this today?***

Built for **SE4041 — Mobile Application Design and Development, Assignment 01 (Kotlin)**
Allocated topic: *Personal Finance and Expense Tracking Mobile Application*

Kotlin · Jetpack Compose · Room · Android 8.0+ · No network, no accounts, no permissions.

---

## The idea

A conventional expense tracker tells you what you already spent. That's a true statement about money you no longer have.

SafeSpend leads with a different number: **safe to spend today**. It takes what's left of the month's budget, divides it across the days that are actually left, and subtracts what's gone today. Overspend on Monday and Tuesday through Sunday each quietly shrink — the figure self-corrects instead of letting the month collapse in the last week.

The second feature keeps that number honest. Ledgers go stale because manual entry is slow, so SafeSpend lets you **paste the bank alert SMS** your bank already sent. It reads the amount, shop, date and direction, and fills in the form for you to check.

It does this **without the `READ_SMS` permission** — that would hand the app your entire message history for a convenience feature. Paste keeps you in control of exactly which message the app ever sees. The manifest declares no permissions at all, `INTERNET` included.

---

## Features

| | |
|---|---|
| **Safe-to-spend engine** | A daily allowance derived from budget or income, minus savings, spread across the days remaining — with a plain-language status line and a pace projection |
| **Smart paste capture** | Turn a bank SMS into a reviewed draft transaction in two taps |
| **Ledger** | Month-scoped, grouped by day with daily net totals, free-text search across note/shop/category, type and category filters, delete with undo |
| **Envelope budgets** | A monthly ceiling per category with progress bars, over-budget alerts, and one-tap carry-forward from last month |
| **Insights** | Category ring chart with legend, a daily bar chart spanning the whole month, and a month-on-month comparison |
| **Savings goals** | Named targets with contributions and withdrawals |
| **Settings** | Expected income, monthly savings, currency symbol, light/dark/system theme, guarded data reset |

---

## Architecture

```
UI (Compose)  →  ViewModel (StateFlow)  →  FinanceRepository  →  Room DAOs  →  SQLite
                        ↑
                  domain/ (pure Kotlin: SafeToSpendCalculator, SmsTransactionParser)
```

MVVM, unidirectional data flow. ViewModels never touch a DAO — the repository is the only door to storage, so swapping the persistence layer is one file's worth of change. The two pieces of real logic live in `domain/` as pure functions with no Android dependency, which is why they are unit-testable without a device.

```
app/src/main/java/com/safespend/app/
├── data/
│   ├── entity/        Room entities (transactions, categories, budgets, goals)
│   ├── dao/           Queries — joins and aggregates computed in SQL, not Kotlin
│   ├── local/         Database, type converters, seeded default categories
│   ├── model/         Query-result types and domain enums
│   ├── prefs/         DataStore-backed settings
│   └── repository/    FinanceRepository — the single door to storage
├── domain/            SafeToSpendCalculator, SmsTransactionParser
├── di/                AppContainer (hand-rolled; no Hilt for an app this size)
├── ui/
│   ├── theme/         The 60-30-10 palette, type scale, shapes
│   ├── components/    SectionCard, TransactionRow, charts, progress, empty states
│   ├── navigation/    Routes and the four top-level destinations
│   └── home | transactions | entry | budgets | insights | goals | settings
└── util/              Money (minor units), MonthPeriod (date maths)
```

### Design decisions worth naming

- **Money is a `Long` count of cents, never a `Double`.** A ledger that drifts by a cent per entry is worse than useless. `BigDecimal` with `HALF_UP` is used at the single parse boundary, in `util/Money.kt`.
- **Dates are stored as epoch day**, so month ranges are plain integer comparisons in SQL.
- **Category colours are indexes into the theme's chart ramp**, not hex strings — a user-created category cannot go off-palette, and everything re-tints correctly in dark mode.
- **Budget spend is a correlated subquery**, not a `JOIN` + `GROUP BY`: joining would multiply budget rows before aggregation, and an envelope with no spending yet must still appear at zero.
- **Categories are archived, never deleted.** Transactions reference them with `ON DELETE RESTRICT`, and history has to stay readable.
- **Material You dynamic colour is off**, deliberately — it would replace the 60-30-10 palette with wallpaper-derived hues.

---

## Colour: the 60-30-10 rule

| Share | Role | Light | Dark |
|---|---|---|---|
| **60%** Canvas | Backgrounds, cards, most text | `#F4F6F7` / `#FFFFFF` / `#0F1D21` | `#0E1517` / `#151D1F` / `#E3E9EA` |
| **30%** Brand | The hero card, primary buttons, selected nav, primary chart series | Deep Teal `#0E4F52` | Teal Bright `#7FD2D2` |
| **10%** Accent | The add button, over-budget warnings, an exhausted allowance | Coral `#FF7A45` | `#FFB08C` |

The ratio is structural, not eyeballed: the canvas is never tinted, exactly one surface per screen carries the brand fill, and the accent is rationed to two jobs — start an action, or raise an alarm. Full rationale and measured contrast figures in [`docs/02-ui-design-spec.md`](docs/02-ui-design-spec.md).

---

## Build and run

**Requires:** Android Studio (Ladybug or newer), JDK 17, Android SDK 35.

```bash
git clone <this-repo-url>
cd SafeSpend
```

Open the folder in Android Studio and let it sync — it writes `local.properties` with your SDK path automatically. Then run the `app` configuration on a device or emulator running Android 8.0 (API 26) or later.

From the command line:

```bash
./gradlew :app:assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`.

---

## Tests

**41 unit tests** — pure logic, no device needed:

```bash
./gradlew :app:testDebugUnitTest
```

| Suite | Covers |
|---|---|
| `MoneyTest` | Parsing, formatting, rounding, symbol handling, round-tripping |
| `SafeToSpendCalculatorTest` | Pool selection, allowance maths, all four status states, division-by-zero on the last day |
| `SmsTransactionParserTest` | Real bank-alert formats, symbol-first and amount-first amounts, three date orders, unparseable input |
| `MonthPeriodTest` | Month boundaries, days remaining, year rollover, relative date labels |

**28 integration tests** — real Room, real SQLite, real Compose; needs a connected device or emulator:

```bash
./gradlew :app:connectedDebugAndroidTest
```

| Suite | Covers |
|---|---|
| `FinanceRepositoryIntegrationTest` | Seeded categories, joins, aggregate queries, filters and search, budget subquery including zero-spend and over-budget envelopes, carry-forward, archived categories, goal clamping |
| `AddEditTransactionViewModelTest` | The entry flow end to end: validation, type switching, SMS paste → save, editing in place, deletion |
| `AddTransactionScreenTest` | The same flow driven through the actual Compose tree |

---

## Documentation

| | |
|---|---|
| [`docs/01-planning-and-requirements.md`](docs/01-planning-and-requirements.md) | Problem, target audience, feasibility study, scope, FR1–FR15 and NFR1–NFR9 |
| [`docs/02-ui-design-spec.md`](docs/02-ui-design-spec.md) | Ideation, the 60-30-10 colour system, type scale, components, screen-by-screen layouts, accessibility, and a Figma rebuild guide |

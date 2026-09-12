# SafeSpend — Planning & Requirement Analysis

**Module:** SE4041 Mobile Application Design and Development — Assignment 01 (Kotlin)
**Allocated topic:** Personal Finance and Expense Tracking Mobile Application (registration number ending in 4)
**Platform:** Android 8.0 (API 26) and above, Kotlin + Jetpack Compose

---

## 1. The problem

People do not abandon expense trackers because the apps are bad at arithmetic. They abandon them because of two specific failures.

**The apps answer the wrong question.** A conventional tracker reports what has already been spent. That is a true statement about money the user no longer has. The question people actually ask — standing at a counter, phone in hand — is *can I afford this right now?* Answering it from a list of past transactions requires the user to do the mental arithmetic the app was supposed to do for them.

**Recording is slower than spending.** Manual entry takes fifteen to twenty seconds per purchase: open app, tap add, type amount, choose category, choose date, save. A person who buys lunch, a bus ticket and a coffee is asked for a minute of data entry a day. Adherence studies of budgeting apps consistently show most users stop inside the first month, and the gap is filled with guesswork.

There is a local dimension too. In Sri Lanka, day-to-day spending is a mix of cash and card, prices moved sharply over 2022–2024, and every bank already sends an SMS alert for card transactions. The information needed to fill a ledger is already arriving on the phone — it is simply not being used.

**SafeSpend addresses both failures directly:** it converts the month's remaining money into a single daily figure the user can act on, and it lets a bank alert be turned into a transaction by pasting it.

### 1.1 Problem statement

> Individuals managing a monthly income have no reliable, low-effort way to know how much they can safely spend today without jeopardising the rest of the month, and existing trackers demand more data-entry effort than the insight they return.

---

## 2. Target audience

| | Primary | Secondary |
|---|---|---|
| **Who** | Young working professionals and undergraduates, 20–35, earning or receiving a predictable monthly amount | Households running a single shared monthly budget |
| **Income pattern** | One salary or allowance per month, spent down over 30 days | Combined income, several fixed commitments |
| **Current method** | Mental arithmetic, a notes app, or checking the bank balance and hoping | A spreadsheet updated irregularly |
| **Pain** | Money disappears by the third week with no clear account of where | No shared view; overspending is discovered after the fact |
| **What they need** | A single number, updated daily, that is safe to act on | Category ceilings and a visible record |
| **Technical profile** | Android phone, comfortable with apps, unwilling to spend a minute per purchase | Mixed confidence; needs an interface that explains itself |

**Explicitly not the audience:** investors and traders (SafeSpend gives no investment advice), businesses needing invoicing or tax reporting, and anyone wanting automatic bank-account linking — which would require open-banking agreements that do not exist for this use case.

---

## 3. Feasibility study

### 3.1 Technical feasibility — **Feasible**

Every capability the product needs is available in the standard Android toolchain, with no third-party service:

| Need | Solution | Risk |
|---|---|---|
| Local structured storage | Room over SQLite | None — first-party, stable |
| Reactive UI | Jetpack Compose + Kotlin Flow | None |
| Charts | Compose `Canvas`, drawn in-project | Low — avoids an abandoned chart library |
| Bank-message parsing | Regex over pasted text | Medium — formats vary between banks; mitigated by treating every parse as an editable draft, never an authority |
| Settings storage | DataStore Preferences | None |

The heaviest technical risk is SMS format variation. It is contained by design: the parser fills a form the user reviews, so a wrong parse costs a correction, not a wrong ledger.

### 3.2 Operational feasibility — **Feasible**

The app works entirely offline with no account, no sign-up and no server. There is nothing to provision, nothing to keep running, and no dependency on connectivity — which matters for an app expected to be opened at a till.

### 3.3 Economic feasibility — **Feasible**

| Item | Cost |
|---|---|
| Android Studio, Kotlin, Jetpack libraries | Free |
| Backend infrastructure | None — the app has no server |
| Third-party APIs | None |
| Google Play developer account (only if published) | USD 25, one-off |
| Development effort | Approx. 60 hours across the assignment period |

Total marginal cost to build and run: effectively zero.

### 3.4 Schedule feasibility — **Feasible**

| Phase | Work | Share of effort |
|---|---|---|
| Planning & requirements | Problem, audience, feasibility, scope, FR/NFR | 10% |
| Design | Palette, typography, components, screen layouts, Figma prototype | 20% |
| Development | Data layer, domain logic, seven screens | 50% |
| Testing | Unit tests, integration tests, manual pass | 15% |
| Documentation | Report, screenshots, evidence | 5% |

### 3.5 Legal and ethical feasibility — **Feasible, with one deliberate decision**

The obvious way to automate capture is the `READ_SMS` permission. SafeSpend does **not** use it, for three reasons: Google Play restricts the permission and rejects most apps that request it; it would grant the app the user's entire message history in exchange for a convenience feature; and it removes the user's control over which messages the app ever sees. Paste achieves most of the benefit with none of that, so the app declares **no permissions at all** — not even `INTERNET`. No personal data can leave the device, which makes the app trivially compliant with any data-protection regime.

---

## 4. Scope

### 4.1 In scope

- Recording, editing and deleting income and expense transactions
- Capturing a transaction from a pasted bank SMS
- Category management with a seeded default set
- Monthly per-category budgets (envelopes), carried forward between months
- The safe-to-spend daily allowance engine
- Category and daily-pattern charts, and month-on-month comparison
- Savings goals with contributions and withdrawals
- Local persistence, light and dark themes, configurable currency symbol
- Unit and integration testing

### 4.2 Out of scope

| Excluded | Why |
|---|---|
| Bank account linking / automatic import | Requires open-banking agreements unavailable for this context |
| Cloud sync, accounts, multi-device | Contradicts the no-server, no-permission privacy position |
| Multi-currency with live conversion | Needs a network rate feed; the target user holds one currency |
| Receipt OCR | Scope; the paste feature covers the same need more cheaply |
| Investment tracking or advice | Regulated activity, and outside the assigned topic |
| Deployment and maintenance | Explicitly excluded by the assignment brief |

---

## 5. Functional requirements

Each requirement lists the acceptance criterion used to verify it and the code that implements it.

| ID | Requirement | Acceptance criterion | Implemented in |
|---|---|---|---|
| **FR1** | Record a transaction with amount, type, category, date, optional shop and note | A saved transaction appears in the ledger under its date with the correct signed amount | `ui/entry/AddEditTransactionViewModel.kt` |
| **FR2** | Capture a transaction by pasting a bank alert | Pasting a debit alert fills amount, direction, shop, date and a suggested category, all editable | `domain/SmsTransactionParser.kt` |
| **FR3** | Edit or delete an existing transaction | Editing updates the same row; deleting offers an undo that restores it | `AddEditTransactionViewModel.kt`, `TransactionsViewModel.kt` |
| **FR4** | Browse transactions one month at a time, grouped by day | Each day shows a heading with that day's net figure; month arrows move the window | `ui/transactions/TransactionsScreen.kt` |
| **FR5** | Search and filter the ledger | Free text matches note, shop or category; type and category chips narrow further; the result count and total update live | `TransactionDao.observeFiltered` |
| **FR6** | Provide categories, seeded on first run, archivable without losing history | 16 categories exist before the first transaction; archiving hides a category from the picker but its past transactions still display its name | `data/local/SafeSpendDatabase.kt` |
| **FR7** | Set a monthly spending ceiling per category, and copy a month's envelopes forward | Setting the same category twice updates rather than duplicates; copying does not overwrite an existing envelope | `BudgetDao`, `ui/budgets/BudgetsViewModel.kt` |
| **FR8** | Show consumption against each envelope and flag overspending | The bar fills proportionally and clamps at full; over-budget envelopes switch to the alert colour and report the amount over | `data/model/QueryResults.kt`, `BudgetsScreen.kt` |
| **FR9** | Compute a safe-to-spend figure for today | Remaining pool divided across remaining days, minus today's spend; recalculates whenever any input changes | `domain/SafeToSpend.kt` |
| **FR10** | Visualise spending by category and across the days of the month | A ring chart shows category share with a legend; a bar chart shows every day including zero-spend days | `ui/components/Charts.kt`, `InsightsScreen.kt` |
| **FR11** | Compare this month's spending with last month's | States the percentage change and both totals, or says there is nothing to compare | `ui/insights/InsightsViewModel.kt` |
| **FR12** | Create savings goals and move money in or out | Progress updates immediately; a withdrawal cannot take a goal below zero | `GoalDao`, `ui/goals/GoalsViewModel.kt` |
| **FR13** | Configure expected income, monthly savings, currency symbol and theme | Each change takes effect immediately across every screen | `data/prefs/UserPreferences.kt` |
| **FR14** | Persist everything locally across restarts | Data entered before a force-stop is present on relaunch | Room + DataStore |
| **FR15** | Clear all transactions without losing categories, budgets or goals | After clearing, the ledger is empty and the other three are intact | `SettingsViewModel.clearAllTransactions` |

## 6. Non-functional requirements

| ID | Requirement | How it is met | How it is verified |
|---|---|---|---|
| **NFR1 Performance** | Screens respond within one second on a mid-range device | Indexed columns on `date`, `category_id` and `type`; aggregates computed in SQL, not in Kotlin; Flow-driven updates recompose only what changed | Manual timing; aggregate queries covered by integration tests |
| **NFR2 Offline operation** | Full functionality with no network | The manifest declares no permissions, `INTERNET` included | Airplane-mode pass |
| **NFR3 Privacy** | No personal data leaves the device | No network, no accounts, no analytics, no `READ_SMS` | Manifest inspection |
| **NFR4 Monetary accuracy** | No rounding drift | All amounts are `Long` minor units; `BigDecimal` with `HALF_UP` at the single parse boundary | `MoneyTest` |
| **NFR5 Usability** | Recording a transaction takes at most three interactions beyond the amount | FAB → amount → category → save; paste path collapses it further | Manual task timing |
| **NFR6 Accessibility** | Usable without relying on colour, at enlarged text sizes | Amounts carry an explicit `+`/`−`; every icon-only control has a content description; body text ≥14sp; text contrast meets 4.5:1 in both themes | Contrast check, TalkBack pass |
| **NFR7 Compatibility** | Android 8.0+ in light and dark themes | `minSdk 26`; theme tokens defined for both schemes; `values-night` resources | Tested at API 26 and 35 |
| **NFR8 Maintainability** | A change in one layer does not ripple through the others | MVVM with a single repository as the only door to storage; ViewModels never see a DAO | Layering visible in package structure |
| **NFR9 Reliability** | No crash or nonsense figure on empty or extreme data | Aggregates return 0 rather than null; progress values clamped; division guarded when no days remain | Integration and unit tests cover the empty and over-budget cases |

---

## 7. Features and functionalities

| Feature | What the user gets |
|---|---|
| **Safe-to-spend card** | One figure for today, a plain-language status line, and a bar showing the month consumed so far |
| **Smart paste capture** | A bank SMS becomes a reviewed draft transaction in two taps |
| **Ledger** | Month-scoped, grouped by day with daily net figures, searchable and filterable, swipe-free delete with undo |
| **Envelope budgets** | A ceiling per category, progress bars, over-budget alerts, and one-tap carry-forward from last month |
| **Insights** | Category ring chart with legend, daily bar chart across the whole month, and a month-on-month comparison |
| **Savings goals** | Named targets with progress, contributions and withdrawals |
| **Settings** | Expected income, monthly savings, currency symbol, light/dark/system theme, and a guarded data reset |

---

## 8. SDLC process followed

| Stage | What was produced |
|---|---|
| Planning & requirement analysis | This document — problem, audience, feasibility, scope |
| Defining requirements | FR1–FR15 and NFR1–NFR9 above, each with an acceptance criterion |
| Design | `02-ui-design-spec.md` — palette under the 60-30-10 rule, type scale, components, screen layouts; Figma prototype built from it |
| Development | Kotlin + Jetpack Compose app in this repository, committed incrementally |
| Testing | 41 unit tests (`app/src/test`) and 28 integration tests (`app/src/androidTest`), plus a manual pass |

Deployment and maintenance are excluded by the assignment brief.

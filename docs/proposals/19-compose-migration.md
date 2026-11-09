# Proposal #19: UI direction — Jetpack Compose or ViewBinding + ListAdapter

- **Issue:** #19
- **Date:** 2026-11-09
- **Author:** Claude Code (Fable 5)
- **Status:** Proposed (awaiting owner decision)

## Context

The UI is XML Views wired with `findViewById`. `docs/AUDIT.md` proposes both ViewBinding + `ListAdapter`
(§4 item 7) and Compose (§4 item 8). Doing both in sequence is double work, so one direction must be picked.
The decision also settles #18 (navigation) and the timing of #45 (`targetSdk 36`).

### Size of the current UI (measured on `main` at `e970c92`)

| What | Size |
|---|---|
| View-layer Kotlin: 4 fragments, 3 adapters, `MainActivity` | 8 files, 1,471 lines |
| XML layouts | 9 files, 1,039 lines |
| `findViewById` calls | 69, in all 8 files |
| Lines that construct Views in code (`LinearLayout(...)`, `TextView(...)`, …) | 45; 39 of them in `TeamDetailsFragment` (23) and `MatchDetailsFragment` (16) |
| `notifyDataSetChanged()` calls | 4, in all 3 adapters |
| Hardcoded user-visible strings | at least 14 in layouts and 14 in Kotlin; `strings.xml` holds only `app_name` (#13) |
| Framework-independent presentation code: 4 ViewModels + UiState classes | 280 lines — **stays as is in every option** |
| UI tests | 1 instrumented test tied to Views (`SystemBarInsetsTest`); no screen tests |

About 2,500 lines of view code are in play. The ViewModels already expose `StateFlow<XxxUiState>`, which
Compose consumes directly with `collectAsStateWithLifecycle()`. The app has 4 screens and 2 tabs: small enough
that a full migration is a matter of days, not weeks.

### Toolchain constraint (verified by a throwaway build, 2026-11-09)

Adding Compose to the current build **fails** `checkDebugAarMetadata`:

- Compose UI depends on `lifecycle-runtime-compose`. The project is on Lifecycle 2.11.0, and Lifecycle
  publishes alignment constraints, so `lifecycle-runtime-compose` resolves to 2.11.0 as well.
- `lifecycle-runtime-compose` 2.11.0 requires `compileSdk 37`. The project compiles against 36, and
  AGP 9.1.0 reports 36 as its maximum recommended compile SDK.

Two ways out:

| Path | Change | Status |
|---|---|---|
| **A. Stay on `compileSdk 36` / AGP 9.1.0** | Pin Lifecycle 2.11.0 → **2.10.0**; use Compose BOM **2026.06.01** (Compose UI 1.11.4, Material3 1.4.0), `activity-compose` 1.13.0, Kotlin Compose compiler plugin **2.2.10** (must equal the Kotlin bundled in AGP 9.1.0) | **Verified:** `assembleDebug` and `assembleRelease` pass with a sample composable |
| **B. Raise `compileSdk` to 37** | Needs an AGP upgrade first (9.1.0 → a release that supports 37). A newer AGP bundles a newer Kotlin, which moves the Hilt/KSP pins from D-006 | Not tested; a separate toolchain task |

Version ceilings under path A (from AAR metadata on Google Maven):

| Library | Newest that fits `compileSdk 36` | First that needs 37 |
|---|---|---|
| Compose BOM | 2026.06.01 | 2026.08.00 |
| `lifecycle-*-compose` | 2.10.0 | 2.11.0 |
| `navigation-compose` | 2.9.8 | 2.10.x |
| `navigation3-runtime` / `-ui` | 1.2.1 (latest stable) | — |
| `hilt-lifecycle-viewmodel-compose` | 1.3.0 | 1.4.0 |
| `fragment-compose` | 1.8.9 | 1.9.x |
| `coil-compose` (Coil 2 line, matches D-006) | 2.7.0 | — |

Dependabot needs matching ignore rules for these until `compileSdk` is raised.

### Portfolio value of Compose (assessment, not measured data)

- Compose is Google's recommended UI toolkit, and new Jetpack UI work (adaptive layouts, Navigation 3,
  Material 3 Expressive) ships Compose-first. A junior candidate in 2026 is expected to show it.
- Views knowledge is still asked for, because most production apps contain legacy Views. A repository whose
  history shows a working Views app **migrated screen by screen through interop** demonstrates both skills
  and the migration skill itself, which is what teams actually do.
- `README.md` already lists "Jetpack Compose migration" in its roadmap and wrongly claims ViewBinding.
  A reviewer who opens the code sees `findViewById` first.
- A ViewBinding + `ListAdapter` refactor is correct engineering but adds little that stands out in a portfolio.

## Options Considered

### Option 1: ViewBinding + ListAdapter/DiffUtil (stay on Views)
- **Description:** Enable `viewBinding`, replace 69 `findViewById` calls, convert 3 adapters to `ListAdapter`.
- **Pros:**
  - Lowest risk, size M, no new dependencies, no toolchain constraint.
  - Makes the `README.md` claim true.
- **Cons:**
  - The 39 lines of hand-built Views in the details fragments stay unless new item layouts and adapters are written too.
  - Weakest portfolio result; the README roadmap item stays open.
  - If Compose is done later, all of this work is thrown away.
- **Impact on Stack & Dependencies:** `buildFeatures { viewBinding = true }` only. #18 → Navigation Component
  (fragments). #45 keeps the Views inset code from #10.

### Option 2: Permanent hybrid (details screens on Compose, tabs on ViewBinding + ListAdapter)
- **Description:** `ComposeView` inside `MatchDetailsFragment` and `TeamDetailsFragment`; ViewBinding and
  `ListAdapter` for the two tab screens.
- **Pros:**
  - Removes the worst code (hand-built rows) with Compose, where the gain is largest.
  - Shows interop.
- **Cons:**
  - Two UI toolkits, two theme definitions and two list approaches to maintain forever.
  - Pays the Compose setup cost and the toolchain constraint, and still does the ViewBinding work on the tabs.
  - Navigation stays fragment-based, so the app never reaches a single clean stack.
- **Impact on Stack & Dependencies:** everything from option 3 plus `viewBinding`. Nothing is removed.

### Option 3a: Full Compose, one big-bang rewrite
- **Description:** One branch replaces all fragments, adapters, layouts and navigation.
- **Pros:**
  - No intermediate interop state; shortest calendar time.
- **Cons:**
  - A single PR of roughly 2,500 deleted and 1,000+ added lines: hard to review, and a regression cannot be
    traced to a screen.
  - Blocks every other UI-touching issue (#12, #13, #14) while it is open; conflicts with `AGENTS.md` §12
    (parallel tasks must not touch the same files).
  - No screen tests exist, so the whole app must be verified by hand at once.
- **Impact on Stack & Dependencies:** same end state as 3b.

### Option 3b: Full Compose, screen by screen with interop (recommended)
- **Description:** Add Compose and a theme first. Then migrate one screen per PR by hosting a composable in
  the existing fragment through `ComposeView`. Fragments, manual navigation and `MainActivity` stay until the
  last step, which replaces the shell with a Compose `Scaffold` and the navigation library chosen in #18,
  and deletes fragments, layouts and Views-only dependencies.
- **Pros:**
  - Each PR is size M, has one screen to verify, and leaves `main` shippable.
  - No intermediate ViewBinding step: every `findViewById`, adapter and hand-built row is deleted, not refactored.
  - The ViewModels and `UiState` classes are not touched.
  - Best portfolio result: full Compose end state, with the interop migration visible in history.
- **Cons:**
  - For the duration of the migration there are two theme definitions (XML and Kotlin) that must be kept in sync.
  - The navigation bugs from #18 stay until the last step.
  - More PRs than 3a (6 instead of 1); APK size and build time grow (R8 is already on for release).
- **Impact on Stack & Dependencies:**
  - Added: Kotlin Compose compiler plugin 2.2.10, Compose BOM 2026.06.01 (`ui`, `material3`,
    `ui-tooling-preview`, debug `ui-tooling`), `activity-compose`, `lifecycle-runtime-compose`,
    `lifecycle-viewmodel-compose`, `coil-compose` 2.7.0; a navigation library per #18 in the last step.
  - Changed: Lifecycle 2.11.0 → 2.10.0 (path A). `buildFeatures { compose = true }`.
  - Removed in the last step: `constraintlayout`, and `appcompat` / `material` (Views) if the theme toggle
    no longer needs `AppCompatDelegate`.
  - Detekt: `FunctionNaming` and `MagicNumber` need the standard Compose exceptions
    (`ignoreAnnotated: ['Composable']`, `['Preview']`); there is no `detekt.yml` yet, only a baseline.

## Recommendation

**Option 3b**, on toolchain **path A**.

It matches the issue's own agent recommendation and adds what that recommendation lacked: a verified
toolchain path and a fixed order. Path A is chosen because it builds today; path B hides an AGP and Kotlin
upgrade inside a UI task. Path B becomes a separate follow-up issue and does not block the migration.

### Steps (each is one issue and one PR)

| # | Step | Size | Notes |
|---|---|---|---|
| 1 | Compose foundation | S | Plugin, BOM, Lifecycle pin, Dependabot ignore rules, Detekt exceptions, `FootballPulseTheme` (light/dark `ColorScheme` from `colors.xml`), shared `LoadingState` / `ErrorState` / crest image composables with `@Preview`. No screen changes |
| 2 | Match details screen | M | `ComposeView` in `MatchDetailsFragment`; delete `fragment_match_details.xml` |
| 3 | Team details screen | M | Same pattern; delete `fragment_team_details.xml`. Can run in parallel with step 2 (disjoint files) |
| 4 | Standings screen + league selector | M | `LazyColumn` replaces `StandingsAdapter`; shared league selector composable |
| 5 | Matches screen | M | `LazyColumn` with date headers replaces `MatchesAdapter`; delete `LeaguesAdapter` (used by both tabs, so after step 4) |
| 6 | Compose shell and navigation | M | `setContent` + `Scaffold` + `NavigationBar`, navigation per #18, delete fragments, `activity_main.xml`, Views-only dependencies; rewrite `SystemBarInsetsTest`; fix `README.md` |

Rules for every step: user-visible strings of the migrated screen go to `strings.xml` (`stringResource`);
each screen gets light and dark `@Preview`; state is collected with `collectAsStateWithLifecycle()`.

### Interaction with other issues

- **#18 (navigation):** take its option 3 — defer, and close it with step 6. Until then fragments remain
  the hosts and manual navigation is unchanged, so the duplicate-fragment bug after Activity recreation stays
  for the duration of the migration. Introducing Navigation Component for fragments now would be thrown away
  in step 6. The library for step 6 (Navigation Compose 2.9.8 or Navigation 3 1.2.1, both fit
  `compileSdk 36`) is still #18's question and must be answered before step 6 starts.
- **#45 (`targetSdk 36`):** schedule it right after step 6. Android 16 removes the edge-to-edge opt-out,
  enables predictive back by default, and ignores orientation and resizability limits on large screens.
  The app already calls `enableEdgeToEdge()` and has no `onBackPressed` overrides or orientation locks, so
  the risk is visual. Step 6 replaces the root inset listener from #10 with `Scaffold` insets; doing #45
  after it means one manual verification pass on API 36 instead of two. During steps 2–5 the root listener
  keeps padding the content, so composables inside fragments need no inset handling.
- **#14 (logic out of adapters and fragments):** do it **before** steps 2–5, or fold each screen's part into
  that screen's step. Otherwise grouping, date formatting and league rules get ported into composables and
  moved again later. Needs re-scoping: its adapter-related parts disappear.
- **#13 (strings):** re-scope to "whatever is left after step 6"; extracting strings from layouts that are
  about to be deleted is wasted work.
- **#12 (typed errors):** independent; landing it before step 1 lets the shared `ErrorState` composable be
  built once against `AppError`.
- **#15 (ViewModel tests):** independent and the main safety net, since ViewModels do not change.
- **#34 (Hilt):** independent. If it lands first, composables in step 6 use `hiltViewModel()` from
  `hilt-lifecycle-viewmodel-compose` 1.3.0.
- **AUDIT §4 item 7 (ViewBinding + ListAdapter):** rejected by this decision; no issue should be created for it.

### Risks

- The theme toggle uses `AppCompatDelegate.setDefaultNightMode`, which recreates the Activity. Compose's
  `isSystemInDarkTheme()` follows the resulting configuration, so it keeps working in steps 2–5; step 6
  must decide how to keep it without AppCompat.
- Crest images rely on the app-wide Coil `ImageLoader` with `SvgDecoder` (`FootballPulseApp`).
  `coil-compose` 2.7.0 uses the same loader, so SVG crests keep working; `CrestImageLoaderTest` stays valid.
- Lifecycle 2.10.0 was only build-verified. Step 1 must also run unit tests and a manual smoke test.

## Proposed Entry for docs/DECISIONS.md

```markdown
## D-007: UI migrates fully to Jetpack Compose, screen by screen

- **Date:** YYYY-MM-DD
- **Status:** Accepted
- **Issue / PR:** #19
- **Decided by:** owner

**Context.** The UI is XML Views with 69 `findViewById` calls, 3 adapters on `notifyDataSetChanged()` and
details screens that build rows in code (`docs/AUDIT.md` §3.4, §4 items 7–8). Doing ViewBinding and then
Compose is double work. Compose cannot be added to the current build as is: with Lifecycle 2.11.0,
`lifecycle-runtime-compose` requires `compileSdk 37`, and AGP 9.1.0 supports up to 36. See architectural
proposal: [docs/proposals/19-compose-migration.md](proposals/19-compose-migration.md).

**Options.**
1. ViewBinding + ListAdapter/DiffUtil — low risk, size M / weakest portfolio result, discarded if Compose comes later.
2. Permanent hybrid (details on Compose, tabs on ViewBinding) — fixes the worst code / two toolkits forever.
3a. Full Compose in one rewrite — shortest / one unreviewable PR, blocks other UI issues.
3b. Full Compose screen by screen via `ComposeView` in existing fragments, shell and navigation last — small
    PRs, `main` always shippable / two theme definitions and the #18 bugs remain during the migration.

**Decision.** Option 3b on `compileSdk 36` / AGP 9.1.0: Kotlin Compose compiler plugin 2.2.10, Compose BOM
2026.06.01, Lifecycle pinned back to 2.10.0, `coil-compose` 2.7.0. Order: foundation → match details →
team details → standings → matches → Compose shell and navigation. No intermediate ViewBinding step.

**Why.**
- ViewModels already expose `StateFlow<UiState>` and stay untouched; only ~2,500 lines of view code change.
- Every `findViewById`, adapter and hand-built row is deleted instead of refactored twice.
- One screen per PR keeps regressions traceable while no UI tests exist.
- Full Compose with a visible interop migration is the strongest portfolio outcome.
- The pinned versions were build-verified; raising `compileSdk` to 37 would drag an AGP and Kotlin upgrade into a UI task.

**Consequences.**
- #19 is split into six sequential issues (`agent:codex`, 1 × `size:S`, 5 × `size:M`).
- Lifecycle goes 2.11.0 → 2.10.0, amending D-006 step 3. Dependabot gets ignore rules for Compose BOM
  ≥ 2026.08.00, Lifecycle ≥ 2.11.0, `navigation-compose` ≥ 2.10.0, `hilt-lifecycle-viewmodel-compose` ≥ 1.4.0
  and `fragment-compose` ≥ 1.9.0 until `compileSdk` is raised.
- Detekt gets a config file with the standard Compose exceptions for `FunctionNaming` and `MagicNumber`.
- #18 is deferred and closed by the last step; its library choice must be made before that step starts.
- #45 (`targetSdk 36`) is scheduled right after the last step.
- #14 runs before the screen steps or is folded into them; #13 is re-scoped to what remains after the last step.
- AUDIT §4 item 7 (ViewBinding + ListAdapter) is rejected.
- New follow-up issue: AGP upgrade + `compileSdk 37` (removes the pins and ignore rules above).
```

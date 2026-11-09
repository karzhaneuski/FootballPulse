# Decision Log

Every architectural or process decision is recorded here: choice of library or approach,
rejecting something. New entries go on top. A decision number is never reused.

Statuses: `Accepted` · `Rejected` · `Superseded by D-XXX` · `Proposed` (awaiting the owner's decision).

## Template

```markdown
## D-XXX: <short title>

- **Date:** YYYY-MM-DD
- **Status:** Proposed | Accepted | Rejected | Superseded by D-YYY
- **Issue / PR:** #N
- **Decided by:** <owner / agent + who approved>

**Context.** What problem or fork in the road, what constraints.

**Options.**
1. <option A> — pros / cons
2. <option B> — pros / cons

**Decision.** What was chosen.

**Why.** Key arguments.

**Consequences.** What changes, what must be done, what we give up.
```

---

## D-007: UI migrates fully to Jetpack Compose, screen by screen

- **Date:** 2026-11-09
- **Status:** Accepted
- **Issue / PR:** #19
- **Decided by:** owner

**Context.** The UI is XML Views with 69 `findViewById` calls, 3 adapters on `notifyDataSetChanged()` and
details screens that build rows in code (`docs/AUDIT.md` §3.4, §4 items 7–8). Doing ViewBinding and then
Compose is double work. Compose cannot be added to the current build as is: with Lifecycle 2.11.0,
`lifecycle-runtime-compose` requires `compileSdk 37`, which is unsupported by AGP 9.1.0. See architectural
proposal: [docs/proposals/19-compose-migration.md](proposals/19-compose-migration.md).

**Options.**
1. ViewBinding + ListAdapter/DiffUtil — low risk, size M / weakest portfolio result, discarded if Compose comes later.
2. Permanent hybrid (details on Compose, tabs on ViewBinding) — fixes the worst code / two toolkits forever.
3a. Full Compose in one rewrite — shortest / one unreviewable PR, blocks other UI issues.
3b. Full Compose screen by screen via `ComposeView` in existing fragments, shell and navigation last — small
    PRs, `main` always shippable / two theme definitions and the #18 bugs remain during the migration.

**Decision.** Option 3b on `compileSdk 36` / AGP 9.1.0: full migration to Jetpack Compose, screen by screen via `ComposeView` inside existing fragments, no intermediate ViewBinding step. Kotlin Compose compiler plugin 2.2.10, Compose BOM 2026.06.01, `coil-compose` 2.7.0. Lifecycle is downgraded 2.11.0 → 2.10.0 (amends D-006) because `lifecycle-runtime-compose` 2.11 requires `compileSdk 37`, unsupported by AGP 9.1.0; Dependabot must ignore Lifecycle ≥ 2.11 until `compileSdk 37`. Order: foundation and theme → match details → team details → standings → matches → Compose shell and navigation. Navigation (#18) is resolved in the last migration step; `targetSdk 36` (#45) right after the migration.

**Why.**
- ViewModels already expose `StateFlow<UiState>` and stay untouched; only ~2,500 lines of view code change.
- Every `findViewById`, adapter and hand-built row is deleted instead of refactored twice.
- One screen per PR keeps regressions traceable while no UI tests exist.
- Full Compose with a visible interop migration is the strongest portfolio outcome.
- Pinned versions were build-verified; raising `compileSdk` to 37 would drag an AGP and Kotlin upgrade into a UI task.
- **Rejected:** big-bang rewrite (Option 3a; unreviewable PR, blocks concurrent UI work, difficult to localize regressions), ViewBinding as an intermediate step (Option 1; throws away refactoring effort once Compose arrives).

**Consequences.**
- #19 is split into six sequential issues (`agent:codex`, 1 × `size:S`, 5 × `size:M`).
- Lifecycle goes 2.11.0 → 2.10.0, amending D-006 step 3. Dependabot gets ignore rules for `androidx.lifecycle:*` ≥ 2.11.0 (with the reason recorded) as well as Compose BOM ≥ 2026.08.00, `navigation-compose` ≥ 2.10.0, `hilt-lifecycle-viewmodel-compose` ≥ 1.4.0, and `fragment-compose` ≥ 1.9.0 until `compileSdk 37`.
- Detekt gets a config file with the standard Compose exceptions for `FunctionNaming` and `MagicNumber`.
- Navigation (#18) is deferred and resolved by the last migration step (step 6); its library choice must be made before step 6 starts.
- `targetSdk 36` (#45) is scheduled right after the migration (step 6).
- #14 runs before the screen steps or is folded into them; #13 is re-scoped to what remains after the migration.
- AUDIT §4 item 7 (ViewBinding + ListAdapter) is rejected.
- New follow-up issue created: "Upgrade AGP to 9.5 and compileSdk to 37" (`agent:codex`, `size:M`), scheduled after migration step 6 (removes the Lifecycle pin and Dependabot ignore rules).

## D-006: Dependency upgrades in grouped sequential steps, then Dependabot

- **Date:** 2026-11-08
- **Status:** Accepted
- **Issue / PR:** #20
- **Decided by:** owner

**Context.** Libraries are 1–3 years behind (`docs/AUDIT.md` §1.2, §3.1 item 3) and `compileOptions` targets Java 11. DI step 2 (#34, D-005) needs Hilt ≥ 2.59.2 and KSP ≥ 2.3.6 on AGP 9. AGP 9.1.0 bundles Kotlin 2.2.10. See architectural proposal: [docs/proposals/20-dependency-upgrades.md](proposals/20-dependency-upgrades.md).

**Options.**
1. Everything in one PR — fast / regressions are hard to localize across layers.
2. Grouped PRs in a fixed sequential order, then Dependabot — small reviewable steps with isolated failure causes / more PRs.
3. Dependabot only — no manual work / proposes versions that break on older SDKs with no control over upgrade order.

**Decision.** Option 2, executed after #9, in strict sequential order:
0. **compileSdk 36** (keep `targetSdk` at 35) — #38
1. **Java 17** (`compileOptions` 11 → 17) — #39
2. **Retrofit 3.0.0** (`retrofit` + `converter-gson` 2.9.0 → 3.0.0; `logging-interceptor` stays 4.12.0) — #40
3. **Coroutines 1.11.0 + Lifecycle 2.11.0** — #41 *(amended by D-007: Lifecycle downgraded 2.11.0 → 2.10.0)*
4. **AndroidX UI** (`core-ktx`, `activity`, `appcompat`, `material`, `constraintlayout` — latest compatible with `compileSdk 36`; test libraries `androidx.test.ext:junit`, `espresso` upgraded alongside) — #42
5. **Coil 2.7.0** (stay on 2.x) — #43
6. **Dependabot** (`.github/dependabot.yml` for `gradle` and `github-actions`, weekly) — #44

Room 2.6.1 stays until decided in #17. Pinned for #34: Hilt 2.59.2 and KSP 2.3.13. Raising `targetSdk` to 36 is a separate task (#45), scheduled with #19.

**Why.**
- Step 0 (`compileSdk 36`) only affects which APIs we compile against without runtime behavior change, and removes the `minCompileSdk` ceiling on `core-ktx`/`activity` so Dependabot needs no exclusions for them.
- Isolates risks into single-cause, easily reviewable PRs.
- Unblocks #34 (Hilt + KSP) early without premature AGP/Kotlin upgrades.
- Avoids wasted work on libraries that #17 (Room) or #19 (UI framework) might replace.
- Raising `targetSdk` to 36 involves runtime behavior changes and requires manual UI verification alongside the UI architecture decision (#19).

**Consequences.**
- Implementation is split into sequential issues (#38–#44, `agent:codex`, `size:S` / `size:M`), each blocked by the previous step.
- Every step requires `./gradlew assembleDebug testDebugUnitTest lintDebug` and `./gradlew assembleRelease` to verify R8/ProGuard compatibility.
- Separate `needs-decision` issue created: #45 ("Raise targetSdk to 36", `size:M`), scheduled with #19.
- *Note:* Lifecycle in step 3 was amended by D-007 (downgraded 2.11.0 → 2.10.0 for Compose compatibility on compileSdk 36).

## D-005: Dependency injection with Hilt, introduced in two steps

- **Date:** 2026-11-08
- **Status:** Accepted
- **Issue / PR:** #16
- **Decided by:** owner

**Context.** ViewModels create `FootballRepository()` themselves, the repository accesses the global `object RetrofitInstance` directly, and details screens use hand-written `ViewModelProvider.Factory` classes. No unit tests can be written (#15). The Hilt 2.50 plugin declared in `build.gradle.kts` is unused and incompatible with AGP 9. See architectural proposal: [docs/proposals/16-dependency-injection.md](proposals/16-dependency-injection.md).

**Options.**
1. Hilt — industry standard for Android, compile-time validation / requires Hilt ≥ 2.59.2 and KSP ≥ 2.3.6 on AGP 9, largest toolchain footprint.
2. Koin — simple Kotlin DSL, no code generation / runtime errors, weaker portfolio signal for junior Android roles.
3. Manual DI (`AppContainer`) — zero new dependencies, transparent architecture / manual lifecycle and scoping boilerplate grows with the app.

**Decision.** Hilt, introduced in two steps:
- **Step 1:** Repository interface in `domain` + constructor injection for all ViewModels and the repository (via `viewModelFactory`/`initializer`, no new dependencies).
- **Step 2:** Migrate to Hilt + KSP, only after the dependency-upgrade decision (#20), because Hilt ≥ 2.59.2 and KSP ≥ 2.3.6 are required for AGP 9.

**Why.**
- Step 1 unblocks testing (#15) regardless of framework.
- Hilt is the industry-standard DI expected in a junior Android portfolio.
- Splitting isolates the risky version upgrade from architectural decoupling.
- Manual DI remains a fallback if Hilt does not work with AGP 9.
- **Rejected:** Koin (adds a dependency without the portfolio recognition of Hilt or the zero-dependency simplicity of manual DI).

**Consequences.**
- Implementation is split into two issues:
  - Step 1: #33 (DI step 1: repository interface and constructor injection, `agent:codex`, `size:M`). Unblocks #15.
  - Step 2: #34 (DI step 2: migrate to Hilt + KSP, `agent:codex`, `size:M`), blocked by #20 and #33.
- Unit tests (#15) can be written as soon as step 1 lands without waiting for Hilt or toolchain changes.
- KSP introduced in step 2 can be shared with Room if selected in #17.

## D-004: GEMINI.md delegates to AGENTS.md to avoid rule duplication in Antigravity

- **Date:** 2026-11-08
- **Status:** Accepted
- **Issue / PR:** branch `chore/antigravity-setup`
- **Decided by:** owner (task definition)

**Context.** `GEMINI.md` previously contained `@AGENTS.md`. In Antigravity, `GEMINI.md` is loaded as a project rule and expanded inline while `AGENTS.md` is also loaded, causing duplicate context and precedence conflicts/drift.

**Decision.** `GEMINI.md` explicitly designates `AGENTS.md` as the single source of truth and contains only Antigravity-specific notes (empty for now). `AGENTS.md` remains the authoritative source for all agents.

**Consequences.** Prevents context duplication and rule precedence ambiguity across Antigravity, Claude Code, and Codex.

## D-003: CI checks build, unit tests and Android Lint on every PR

- **Date:** 2026-11-07
- **Status:** Accepted
- **Issue / PR:** branch `chore/agents-setup`
- **Decided by:** owner (task definition), implementation — agent

**Context.** PRs from agents need automated verification.

**Decision.** GitHub Actions `.github/workflows/android-ci.yml`:
`./gradlew assembleDebug testDebugUnitTest lintDebug` on JDK 21. Lint runs with default settings:
errors fail the build, warnings do not (at the time of introduction: 0 errors, 97 warnings).

**Consequences.** No lint baseline. Stricter lint and Detekt/ktlint are a separate task.

## D-002: API key: `local.properties` → environment variable → empty string

- **Date:** 2026-11-07
- **Status:** Accepted
- **Issue / PR:** branch `chore/agents-setup`
- **Decided by:** owner (task definition), implementation — agent

**Context.** `app/build.gradle.kts` read `local.properties` unconditionally, so a clean clone and CI
failed at configuration time (`docs/AUDIT.md` §2, §3.3).

**Decision.** The key is taken from `local.properties`, then from the `FOOTBALL_API_KEY` env variable
(a repository secret in CI), otherwise an empty string. The build does not fail without a key.

**Consequences.** CI is green without a secret (e.g. PRs from forks). A clear user-facing message
about an empty/invalid key belongs to the typed-errors task.

## D-001: `AGENTS.md` as the single source of agent instructions

- **Date:** 2026-11-07
- **Status:** Accepted
- **Issue / PR:** branch `chore/agents-setup`
- **Decided by:** owner

**Decision.** All rules live in `AGENTS.md`. `CLAUDE.md` and `GEMINI.md` contain only the `@AGENTS.md` import.

**Consequences.** Rules are changed in one place, no divergence between agents.

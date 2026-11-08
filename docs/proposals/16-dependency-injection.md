# Proposal #16: Dependency injection approach

- **Issue:** #16
- **Date:** 2026-11-08
- **Author:** Claude Code (Opus 5.5)
- **Status:** Proposed (awaiting owner decision)

## Context

Verified against the code on `main` (`18aa3b4`):

- All 4 ViewModels create the repository themselves: `private val repository = FootballRepository()`
  (`MatchesViewModel`, `StandingsViewModel`, `MatchDetailsViewModel`, `TeamDetailsViewModel`).
- `FootballRepository` is a concrete class that calls the global `object RetrofitInstance` directly
  and reads the clock via `Calendar.getInstance()`. There is no repository interface.
- `MatchDetailsViewModel` and `TeamDetailsViewModel` take an `Int` id through hand-written
  `ViewModelProvider.Factory` classes; `MatchesViewModel` and `StandingsViewModel` are obtained with
  `ViewModelProvider(requireActivity())` (activity scope, so data survives tab switches).
- There is no `Application` subclass (`AndroidManifest.xml` has no `android:name`).
- The Hilt plugin `2.50` is declared `apply false` in the root `build.gradle.kts` and used nowhere.
  Neither `kapt` nor `ksp` is configured.
- `lifecycle-viewmodel-ktx 2.7.0` is already on the classpath, so the `viewModelFactory { initializer { } }`
  DSL and `createSavedStateHandle()` are available without new dependencies.

**What actually blocks tests (#15):** the ViewModels cannot receive a fake repository, and the
repository cannot receive a fake API. That is fixed by **constructor injection + a repository
interface**, which every option below needs. The DI container only decides *who* calls the constructors.

**Stack constraint:** AGP 9 requires Hilt **≥ 2.59.2** and KSP **≥ 2.3.6**
([AGP 9 migration guidance](https://developer.android.com/agents/skills/build-system/agp/agp-9-upgrade/skill)).
The declared `2.50` will not work, so option 1 is a version upgrade plus a new annotation processor.

## Common part (all options)

- `domain/repository/FootballRepository` interface; `data/repository/FootballRepositoryImpl(api: FootballApiService)`.
- `MatchdayResult` moves to `domain/model` (it is returned to the UI today but lives in `data`).
- ViewModels take the repository (and `SavedStateHandle` for ids) as constructor parameters.
- `RetrofitInstance` becomes a provider of `FootballApiService` instead of a global `object`.
- A `FootballPulseApp : Application` is added (also needed by #8 for the shared Coil `ImageLoader`).

## Options Considered

### Option 1: Hilt (Dagger)

- **Description:** `@HiltAndroidApp` application, `@AndroidEntryPoint` on `MainActivity` and the 4 fragments,
  `@HiltViewModel` + `@Inject constructor` on ViewModels, `di/NetworkModule` + `di/RepositoryModule`.
  Ids arrive via `SavedStateHandle` from fragment arguments, so both manual factories are deleted.
  Activity-scoped ViewModels keep working via `by activityViewModels()`.
- **Pros:**
  - Industry standard for Android; the most recognisable choice in a junior portfolio and in interviews.
  - Compile-time graph validation: a missing binding fails the build, not the app at runtime.
  - Standard Android scopes and `SavedStateHandle` wiring out of the box; `hilt-android-testing` for later UI tests.
- **Cons:**
  - Heaviest setup for an app with ~1 repository and 4 ViewModels: plugin upgrade, KSP, annotations on 6+ classes.
  - Build time grows (annotation processing); generated-code errors are harder for a junior to read.
  - Moves the project onto KSP — fine, but it is a second toolchain to keep in sync with AGP/Kotlin.
- **Impact on Stack & Dependencies:** `libs.versions.toml`: `hilt ≥ 2.59.2`, `ksp ≥ 2.3.6`,
  `hilt-android`, `hilt-compiler`, (`fragment-ktx` for `by viewModels()`); root plugin `2.50` → catalog alias;
  `app/build.gradle.kts`: apply `hilt` + `ksp`. The KSP setup is reusable by Room if #17 chooses it.

### Option 2: Koin

- **Description:** `startKoin { modules(appModule) }` in `Application`, `single { }` / `viewModel { }` definitions,
  `by viewModel()` / `by activityViewModel()` in fragments, ids via `parametersOf` or `SavedStateHandle`.
- **Pros:**
  - Plain Kotlin DSL, no code generation, no KSP — zero risk with AGP 9 / built-in Kotlin.
  - Smallest diff and fastest to learn; one module file covers the whole app.
- **Cons:**
  - Missing bindings surface at runtime (mitigated by `verify()` in a unit test, which must be written).
  - Service locator under the hood; less weight in a portfolio aimed at Android roles where Hilt dominates.
  - A new library whose knowledge transfers less to typical company codebases.
- **Impact on Stack & Dependencies:** `koin-android` (+ `koin-test` for tests) in `libs.versions.toml`;
  the unused Hilt plugin declaration is removed.

### Option 3: Manual DI (`AppContainer`)

- **Description:** `AppContainer` built in `FootballPulseApp` holds `FootballApiService` and the repository.
  ViewModels expose a `companion val Factory = viewModelFactory { initializer { ... } }` reading the container
  via `APPLICATION_KEY` and ids via `createSavedStateHandle()` (the pattern from the official Android
  "manual dependency injection" guide).
- **Pros:**
  - Zero new dependencies, no codegen, nothing to upgrade; fits AGENTS.md §7 best.
  - Fully transparent: shows the candidate *understands* DI rather than just using a framework.
  - Easy migration path to Hilt later — constructors are already injectable, only wiring changes.
- **Cons:**
  - Boilerplate grows with each new ViewModel/dependency; scopes and lifetimes are managed by hand.
  - Not what most job descriptions list; on its own it looks less "production-like" in a portfolio.
- **Impact on Stack & Dependencies:** none; the unused Hilt plugin declaration is removed.

## Trade-offs at a glance

| | Hilt | Koin | Manual |
|---|---|---|---|
| New deps / version bumps | Hilt 2.50→2.59.2+, add KSP | Koin | none |
| Error detection | compile time | runtime (`verify()` test) | compile time |
| Effort for this app (~6 classes wired) | highest | low | low |
| Portfolio signal (junior Android) | strongest | medium | medium (strong if explained) |
| Unblocks #15 | yes | yes | yes |

## Recommendation

**Option 1 — Hilt**, plus the repository interface, implemented in two PRs:

1. *Refactor, no new deps* (`agent:codex`, S/M): repository interface + `FootballRepositoryImpl`,
   constructor injection, `RetrofitInstance` → provider. This alone unblocks #15.
2. *Hilt wiring* (`agent:antigravity`, M): upgrade Hilt to ≥ 2.59.2, add KSP ≥ 2.3.6 via the catalog,
   `@HiltAndroidApp` / `@AndroidEntryPoint` / `@HiltViewModel`, delete both manual factories.

Why: the app is small enough that Hilt is overkill on pure engineering grounds, but the project's
stated purpose is a junior Android portfolio, where Hilt is the expected answer and the most common
interview topic. The two-step split keeps the risky part (AGP 9 + KSP + Hilt versions) isolated and
reversible, and if step 2 hits a toolchain problem the codebase is already testable and can fall back
to Option 3 without rework. Koin is not recommended: it adds a dependency without the portfolio
benefit of Hilt and without the zero-dependency benefit of manual DI.

## Proposed Entry for docs/DECISIONS.md

```markdown
## D-005: Dependency injection with Hilt + repository interface in domain

- **Date:** YYYY-MM-DD
- **Status:** Accepted
- **Issue / PR:** #16
- **Decided by:** owner

**Context.** ViewModels create `FootballRepository()` themselves, the repository uses the global
`RetrofitInstance`, two screens use manual `ViewModelProvider.Factory` classes. No unit tests are
possible (#15). The declared Hilt 2.50 plugin is unused and incompatible with AGP 9.

**Options.**
1. Hilt — industry standard, compile-time checks / requires Hilt ≥ 2.59.2 + KSP ≥ 2.3.6, most boilerplate.
2. Koin — simple DSL, no codegen / runtime errors, weaker portfolio signal.
3. Manual `AppContainer` — no dependencies / boilerplate grows, scopes by hand.

**Decision.** Hilt, with `domain/repository/FootballRepository` interface and `FootballRepositoryImpl` in `data`.

**Why.** Portfolio value and compile-time graph validation; the interface + constructor injection
make the app testable independently of the container.

**Consequences.** Two tasks: (1) interface + constructor injection (unblocks #15); (2) Hilt + KSP via
`libs.versions.toml`, `FootballPulseApp`, `@AndroidEntryPoint`/`@HiltViewModel`, manual factories removed.
KSP becomes available for Room if #17 chooses it. Fallback if the Hilt toolchain blocks: Option 3.
```

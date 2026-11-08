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
3. **Coroutines 1.11.0 + Lifecycle 2.11.0** — #41
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

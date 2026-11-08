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

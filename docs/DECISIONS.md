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

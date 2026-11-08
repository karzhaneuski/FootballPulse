# AGENTS.md — FootballPulse

Single source of instructions for all AI agents (Claude Code, Codex, Gemini / Antigravity, etc.).
`CLAUDE.md` imports this file, and `GEMINI.md` delegates to it: make edits **here**.

## 1. Project

Android app: matches, league tables, match and team details for the top-5 European leagues.
Data: REST API `https://api.football-data.org/v4/` (free tier, **10 requests/min**).

## 2. Stack (actual, see `docs/AUDIT.md` §1)

| What | Version / value |
|---|---|
| Gradle Wrapper | 9.3.1 (daemon toolchain JDK 21, `gradle/gradle-daemon-jvm.properties`) |
| AGP | 9.1.0 (Kotlin is built into AGP, no separate Kotlin plugin) |
| Java target | 11 (`compileOptions`) |
| SDK | compile/target 35, min 26 |
| UI | XML Views + `findViewById`, Material 3, ConstraintLayout, RecyclerView |
| Concurrency | Kotlin Coroutines + `StateFlow` |
| Networking | Retrofit 2.9 + Gson, OkHttp logging-interceptor 4.12 |
| Images | Coil 2.6 (without `coil-svg`) |
| Tests | JUnit 4 (no real tests yet) |
| Declared but **unused** | Room 2.6.1, Hilt 2.50 plugin (`apply false`) — their fate is decided in `needs-decision` issues |

Library versions live in `gradle/libs.versions.toml` (partially) and as literal strings in `app/build.gradle.kts`.

## 3. Architecture

Single `:app` module, MVVM, Single Activity.

```
com.artsiom.footballpulse
├── MainActivity            # BottomNavigation, manual fragment navigation (add/hide/show + replace)
├── data/
│   ├── FootballRepository  # the only repository, calls RetrofitInstance.api directly
│   └── remote/             # FootballApiService, RetrofitInstance, *Dto
├── domain/model/           # immutable data classes + Leagues object (no UseCases or interfaces)
└── ui/<screen>/            # Fragment + ViewModel + UiState (+ Adapter): matches, standings,
                            # matchdetails, teamdetails
```

Data flow: `Fragment` ← `StateFlow<XxxUiState>` (`Loading` / `Success` / `Error`) ← `ViewModel` ← `FootballRepository` ← Retrofit.

Known deviations from "clean" architecture (ViewModels create the repository themselves, logic in adapters
and fragments, no DI) are described in `docs/AUDIT.md` §3 and split into issues. **Do not fix them in passing**,
only as part of the corresponding task.

## 4. Commands

```bash
./gradlew assembleDebug        # build debug APK
./gradlew testDebugUnitTest    # unit tests
./gradlew lintDebug            # Android Lint (errors fail the build, warnings do not)
./gradlew assembleDebug testDebugUnitTest lintDebug   # same as in CI
```

Always run the CI command locally before opening a PR. Requires the Android SDK (`ANDROID_HOME` or `sdk.dir`
in `local.properties`) and JDK 21.

### API key

`FOOTBALL_API_KEY` is resolved in this order: `local.properties` → `FOOTBALL_API_KEY` environment variable
(a repository secret in CI) → empty string. Without a key the project still **builds**, but API requests
return 401/403. The key is never committed.

## 5. Code style

- Kotlin official code style (`kotlin.code.style=official`), 4 spaces, no wildcard imports.
- New code follows the existing screen pattern: `XxxFragment` + `XxxViewModel` + `sealed XxxUiState`.
- UI state is exposed only via `StateFlow` in the ViewModel; the fragment subscribes via
  `repeatOnLifecycle(Lifecycle.State.STARTED)`.
- DTOs live in `data/remote/`, every DTO field has `@SerializedName` (required for R8 in release).
- DTOs do not leak outside `data`: `ui` only receives models from `domain/model`.
- In coroutine `catch` blocks never swallow `CancellationException` (rethrow it).
- New user-visible strings go in `res/values/strings.xml`, not hardcoded.
- Dates in new code — `java.time` only (minSdk 26 allows it).
- New dependencies — only via `gradle/libs.versions.toml`.
- Comments should be purposeful and explain "why", not "what".

## 6. Workflow

1. **Issue.** Any work starts with an issue based on the `.github/ISSUE_TEMPLATE/agent-task.md` template
   (context, task, definition of done, affected files, out of scope).
2. **Branch** from a fresh `main`: `agent/<issue-number>-<short-description>`,
   e.g. `agent/12-retry-button-matches`.
3. **Commits** — small, meaningful, Conventional Commits style:
   `fix: ...`, `feat: ...`, `refactor: ...`, `test: ...`, `chore: ...`, `docs: ...`.
   Reference the issue in the message: `fix: keep DTO fields for R8 (#3)`.
4. **PR to `main`.** In the description: `Closes #<number>`, what was done, how it was verified, what remains.
   CI (build + unit tests + lint) must be green.
5. **Handoff.** If the work is unfinished (limit, blocker, fork in the road) — update `docs/HANDOFF.md`
   and link to it in the PR or issue.
6. **Decisions.** Any architectural decision (choice of library or approach, rejecting something)
   is recorded in `docs/DECISIONS.md`.

## 7. Prohibitions

- ❌ Do not push to `main` directly, do not force-push to other people's branches. PRs only.
- ❌ Do not commit secrets: API keys, `local.properties`, keystores, tokens. Not in code, not in logs,
  not in test fixtures.
- ❌ Do not change versions of dependencies, AGP, Gradle, JDK, or SDK without a task that explicitly requires it.
- ❌ Do not add new libraries without a task.
- ❌ Do not disable, delete, or mark tests and lint checks with `@Ignore` just to turn CI green.
- ❌ Do not go beyond the scope of the issue: report problems you notice as a separate issue instead of fixing them in passing.
- ❌ Do not make architectural decisions silently: fork in the road → `needs-decision` label and a question to the owner.
- ❌ Do not record transient environment or tooling issues (proxy errors, missing SDK, network failures) in repository docs; report them only in the PR description or task output.
- ❌ In Codex Cloud, do not create PRs via gh/GitHub API from the container; push the branch and leave PR creation to the Codex UI.
- ❌ Do not run `git checkout` or `git switch` in the main working copy (`~/FootballPulse`).
- ❌ Do not put unrelated folders or worktrees inside the repository or in `/tmp`.

## 8. Routing

Every issue gets a size label and an assignee label.

| Label | When to apply |
|---|---|
| `agent:codex` | All size **S** and **M** implementation tasks. Codex is the main implementer. |
| `agent:antigravity` | Documentation, translations, repository maintenance, and PR reviews performed by Gemini in Antigravity. |
| `needs-decision` | There is an architectural fork (which library/approach to choose, whether to change the structure). The agent **does not start** implementation until the owner makes a decision and it is recorded in `docs/DECISIONS.md`. After the decision the label is removed and `agent:*` is applied. |

Sizes:

| Label | Guideline |
|---|---|
| `size:S` | up to ~1–2 hours, 1–3 files, no new dependencies or one obvious one |
| `size:M` | half a day to a day, 4–10 files or several layers |
| `size:L` | several days, touches most of the app |

## 9. Useful files

- `docs/AUDIT.md` — technical audit (source of tasks).
- `docs/DECISIONS.md` — architectural decision log.
- `docs/HANDOFF.md` — handoff of unfinished work between agents.
- `docs/proposals/` — architectural proposals for `needs-decision` issues.
- `.github/workflows/android-ci.yml` — CI.
- `.agents/workflows/` — reusable workflow definitions for agents.

## 10. Language

All repository artifacts must be written in **English**, regardless of the language of the prompt or
conversation: code, comments, commit messages, branch names, issues, PR titles and descriptions,
review comments, and docs. The only exception is user-facing app strings in `res/values*/strings.xml`,
which follow the app's localization needs.

## 11. Workflows

Standardized prompt shortcuts (workflows) located in `.agents/workflows/` (invoked with `/` in Antigravity):

- `implement-issue <issue-number>`: Read `AGENTS.md` and the given issue via `gh issue view`, create worktree `../FootballPulse-<agent>-<N>`, implement within scope, run `./gradlew assembleDebug testDebugUnitTest lintDebug`, rebase on `origin/main`, open a PR with `gh` (summary, files changed, verification, `Closes #N`), and remove worktree after merge.
- `review-pr <pr-number>`: Create worktree `../FootballPulse-<agent>-review-pr<pr-number>`, check out PR, run `./gradlew assembleDebug testDebugUnitTest lintDebug`, review against `AGENTS.md` and the issue DoD, post findings with `gh pr review --comment` marked `Blocker` / `Should fix` / `Nit`, never change code, and remove worktree.
- `handoff`: Update `docs/HANDOFF.md` (done, where stopped, next step, open questions) before ending a session.
- `decide <issue-number>`: For a `needs-decision` issue, create worktree `../FootballPulse-<agent>-decision-<N>`, write a proposal in `docs/proposals/<N>-<topic>.md` with options, trade-offs, and a recommendation; do not write production code, open a PR, and remove worktree after merge.

## 12. Parallel work

- **Main working copy ownership:** The main working copy (`~/FootballPulse`) belongs to the human and to the implementer; agents never run `git checkout` or `git switch` in the main working copy.
- **Dedicated worktrees:** Every agent task runs in its own git worktree under `../FootballPulse-<agent>-<task>` (never in `/tmp`, which macOS cleans).
- **Workspace cleanliness:** Never put unrelated folders inside the repository.
- **Parallel file isolation:** Tasks running in parallel must not touch the same files.
- **Rebase before PR:** Before opening or merging a PR, rebase on the latest `origin/main`.
- **Worktree cleanup:** Remove the worktree after the PR is merged.

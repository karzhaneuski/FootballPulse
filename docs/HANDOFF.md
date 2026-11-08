# Handoff

Filled in by an agent when work on a task is interrupted: limit reached, blocker,
a fork in the road, or handoff to another agent. The next agent starts by reading this file.
Delete old entries once the task is closed (history stays in git).

## Template

```markdown
## #<issue number>: <title> — <YYYY-MM-DD>

- **Agent:** <claude / codex / antigravity / ...>
- **Branch:** `agent/<number>-<short-description>`
- **PR:** #N (draft) | none

### Done
- ...

### Where I stopped
- Last commit: `<sha>` — <what it contains>
- Current state: does it build? do tests pass? (command output or CI link)

### Next step
1. <specific action, with file and function>
2. ...

### Open questions
- <question> → is an owner answer needed? (if so, add the `needs-decision` label to the issue)
```

---

<!-- Active handoff entries go below this line -->

## #1: Protect DTOs from R8 — 2026-11-08

- **Agent:** Codex
- **Branch:** `agent/1-protect-dtos-from-r8`
- **PR:** none; creation blocked by the environment proxy

### Done
- Implemented issue #1 in commit `8948f3d`: annotated DTO properties, converted score DTOs to data classes, and added Gson/Retrofit R8 rules.
- `./gradlew assembleRelease assembleDebug testDebugUnitTest lintDebug --stacktrace` passed with the existing SDK: unsigned release APK produced, 1 unit test passed, Lint reported 0 errors and 89 warnings.
- Temporary JVM smoke checks passed for nested matches/standings JSON and score round trips against unminified release classes. Live release API/device behavior is unverified.

### Where I stopped
- Implementation is pushed. `gh pr create` failed with `Post "https://api.github.com/graphql": Forbidden`; a separate probe confirmed HTTP CONNECT 403 from the environment proxy.
- PR description is prepared on this machine at `/workspace/.setup/issue-1-pr.md`, including summary, changed files, verification, limitations, and `Closes #1`.
- GitHub CI status is unverified; the required local checks passed.

### Next step
1. Allow access to `api.github.com` in environment settings, then create the PR to `main` using the prepared description; alternatively create it from the pushed branch on GitHub.
2. Verify that PR CI passes. No further code or dependency changes are required for this issue.

### Open questions
- None. The remaining blocker is GitHub API access, not an architectural decision.

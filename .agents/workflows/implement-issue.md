---
description: Implement a GitHub issue following AGENTS.md and open a PR to main
---

# Implement Issue

Follow this workflow to implement an issue end-to-end according to the project's multi-agent rules.

## Guardrails
- **Worktree isolation**: The main working copy (`~/FootballPulse`) belongs to the human and to the implementer. Agents must never run `git checkout` or `git switch` in the main working copy. Every task runs in its own worktree under `../FootballPulse-<agent>-<task>` (never in `/tmp`, which macOS cleans). Never put unrelated folders inside the repository.
- **Parallel isolation**: Tasks running in parallel must not touch the same files.
- **Scope discipline**: Implement strictly what is specified in the issue. Do NOT fix unrelated problems in passing; report them as separate issues instead.
- **Single source of truth**: Follow all guidelines in `AGENTS.md` (stack, architecture, code style, prohibitions, parallel work).
- **Branch protection**: Never push to `main` directly. Work only in a dedicated `agent/<N>-<short-description>` branch.
- **Rebase before PR**: Always rebase on the latest `origin/main` before opening or merging a PR.
- **Verification**: Always verify locally with `./gradlew assembleDebug testDebugUnitTest lintDebug` before opening a PR.

## Steps

### 1. Read Project Rules and Issue Details
- Review `AGENTS.md` for project architecture, code conventions, prohibitions, and parallel work rules.
- Obtain the issue number `$ISSUE_NUMBER` from the invocation argument or context.
- Fetch the issue details:
  ```bash
  gh issue view <issue-number>
  ```
- Inspect the task description, Definition of Done (DoD), affected files, out-of-scope boundaries, and labels. Verify the issue is ready for implementation (not marked `needs-decision`).

### 2. Prepare the Worktree
- Fetch latest changes from `origin`:
  ```bash
  git fetch origin
  ```
- Create a dedicated worktree under `../FootballPulse-<agent>-<task>` branched from `origin/main`:
  ```bash
  git worktree add ../FootballPulse-<agent>-<issue-number>-<short-description> -b agent/<issue-number>-<short-description> origin/main
  cd ../FootballPulse-<agent>-<issue-number>-<short-description>
  ```
- *Note*: Never run `git checkout` or `git switch` in the main working copy (`~/FootballPulse`). Never create worktrees in `/tmp`.

### 3. Implement the Changes
- Make changes strictly within the defined scope inside the worktree.
- Maintain architecture: single `:app` module, MVVM, Single Activity, immutable domain models, DTOs in `data/remote/` with `@SerializedName`.
- Never leak DTOs outside the `data` layer.
- Never swallow `CancellationException` in coroutine catch blocks.
- Put new user-facing strings in `res/values/strings.xml`.
- Use `java.time` for date/time operations.
- Do not add new dependencies without an explicit requirement.

### 4. Verify the Build and Tests
- Run the full CI verification command locally inside the worktree:
  ```bash
  ./gradlew assembleDebug testDebugUnitTest lintDebug
  ```
- Ensure there are 0 build errors and 0 lint errors, and all unit tests pass.

### 5. Commit Changes
- Commit changes using small, meaningful Conventional Commits referencing the issue number:
  ```bash
  git add <files>
  git commit -m "feat/fix: <description> (#<issue-number>)"
  ```

### 6. Push and Open Pull Request
- Rebase on the latest `origin/main` before pushing:
  ```bash
  git fetch origin
  git rebase origin/main
  ```
- Push the branch to remote:
  ```bash
  git push -u origin agent/<issue-number>-<short-description>
  ```
- Open a PR to `main` using GitHub CLI:
  ```bash
  gh pr create --base main --title "<type>: <short summary> (#<issue-number>)" --body "$(cat <<'EOF'
  ## Summary
  <High-level description of what was done>

  ## Files Changed
  <List of key files modified or added>

  ## Verification
  - `./gradlew assembleDebug testDebugUnitTest lintDebug` passed locally (0 errors)
  - <Any manual or unit test verification details>

  Closes #<issue-number>
  EOF
  )"
  ```

### 7. Clean Up Worktree
- After the PR is merged, remove the worktree:
  ```bash
  git worktree remove ../FootballPulse-<agent>-<issue-number>-<short-description>
  ```


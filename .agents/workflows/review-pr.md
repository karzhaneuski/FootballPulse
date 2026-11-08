---
description: Review a pull request against AGENTS.md and the issue Definition of Done
---

# Review Pull Request

Follow this workflow to perform a thorough, non-destructive review of a pull request.

## Guardrails
- **Worktree isolation**: Never run `git checkout` or `gh pr checkout` in the main working copy (`~/FootballPulse`). Reviews must run in a dedicated worktree under `../FootballPulse-<agent>-review-pr<pr-number>` (never in `/tmp`, which macOS cleans). Never put unrelated folders inside the repository.
- **Read-only**: NEVER modify or commit code during a review. All feedback must be submitted through `gh pr review`.
- **Standards enforcement**: Enforce the rules in `AGENTS.md` and the linked issue's Definition of Done (DoD).
- **Structured feedback**: Clearly categorize every finding as **Blocker**, **Should fix**, or **Nit**.

## Steps

### 1. Check Out the Pull Request in a Dedicated Worktree
- Obtain the PR number `$PR_NUMBER` from the invocation argument or context.
- Create a dedicated review worktree under `../FootballPulse-<agent>-review-pr<pr-number>`:
  ```bash
  git fetch origin
  git worktree add ../FootballPulse-<agent>-review-pr<pr-number> -b review-pr-<pr-number> origin/main
  cd ../FootballPulse-<agent>-review-pr<pr-number>
  gh pr checkout <pr-number>
  ```
- *Note*: Never run `gh pr checkout` or switch branches in the main working copy (`~/FootballPulse`).

### 2. Run Gradle Verification Checks
- Run the required CI command locally inside the worktree:
  ```bash
  ./gradlew assembleDebug testDebugUnitTest lintDebug
  ```
- Note any build, test, or lint failures. A build failure or lint error is an automatic **Blocker**.

### 3. Review Against AGENTS.md and Issue DoD
- Identify the linked issue from the PR description (e.g. `Closes #<issue-number>`) and inspect it:
  ```bash
  gh issue view <issue-number>
  ```
- Review the diff using `git diff main...HEAD` or `gh pr diff`:
  - **DoD Compliance**: Did the PR achieve all acceptance criteria? Did it stay within scope?
  - **Architecture & Layering**: Do DTOs stay in `data/remote/`? Does UI only receive domain models? Are ViewModels exposing `StateFlow`?
  - **Prohibitions**: Are there any hardcoded secrets, unauthorized dependency changes, or swallowed `CancellationException`s?
  - **Code Style**: Proper formatting, 4 spaces, no wildcard imports, strings in `res/values/strings.xml`, `java.time` used for dates.

### 4. Post Review Findings
- Submit the review comment using GitHub CLI (do not approve if there are Blockers):
  ```bash
  gh pr review <pr-number> --comment --body "<review-body>"
  ```
- Format the review body with findings tagged by severity:
  - **Blocker**: Issues that prevent merge (e.g., CI failure, architectural violations, security risks, swallowed exceptions, broken requirements).
  - **Should fix**: Important improvements, design flaws, or missing tests that should be addressed before merging.
  - **Nit**: Minor suggestions, style polish, or typo fixes that are non-blocking.

### 5. Clean Up Worktree
- After completing the review, remove the review worktree:
  ```bash
  git worktree remove ../FootballPulse-<agent>-review-pr<pr-number>
  ```


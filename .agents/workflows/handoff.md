---
description: Update docs/HANDOFF.md before ending a work session
---

# Handoff Session

Follow this workflow whenever a work session is paused, interrupted, blocked, or handed off to another agent.

## Guardrails
- **Accurate status**: Never claim work is done or passing if it has not been verified.
- **Actionable next steps**: Write concrete next steps with specific file names, classes, or functions so the next agent can resume immediately.
- **Do not lose WIP**: Ensure any pending work is committed or documented before ending the session.

## Steps

### 1. Assess Current State
- Inspect repository status and recent changes:
  ```bash
  git status
  git diff --stat
  git log -n 5 --oneline
  ```
- Check build and test status:
  ```bash
  ./gradlew assembleDebug testDebugUnitTest lintDebug
  ```
  Note whether the build and tests currently pass or what specific failures exist.

### 2. Read Existing Handoff Notes and Template
- Read `docs/HANDOFF.md` to review the current active handoffs and the required template structure.

### 3. Update docs/HANDOFF.md
- Add or update an entry under `<!-- Active handoff entries go below this line -->` using the standard format:
  ```markdown
  ## #<issue-number>: <title> — <YYYY-MM-DD>

  - **Agent:** <antigravity | claude | codex | ...>
  - **Branch:** `agent/<number>-<short-description>`
  - **PR:** #<number> (draft) | none

  ### Done
  - <Bulleted list of completed tasks>

  ### Where I stopped
  - Last commit: `<sha>` — <commit message summary>
  - Current state: <build / test status and explanation>

  ### Next step
  1. <Specific action, naming file and function/class>
  2. <Subsequent action>

  ### Open questions
  - <Question or blocker> (if owner decision needed, specify that `needs-decision` label should be added)
  ```

### 4. Commit or Stash Changes
- If appropriate, commit the handoff update and WIP on the current agent branch:
  ```bash
  git add docs/HANDOFF.md
  git commit -m "docs: update handoff for #<issue-number>"
  ```


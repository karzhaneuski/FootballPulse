---
description: Write an architectural proposal in docs/proposals/ for a needs-decision issue
---

# Architectural Decision Proposal

Follow this workflow when analyzing a task labeled `needs-decision` to prepare a structured proposal for the project owner.

## Guardrails
- **No production code**: Do NOT implement production code or modify application logic during this workflow. Only documentation proposals are produced.
- **Objective trade-offs**: Present at least two realistic options with objective pros and cons.
- **Alignment**: Ensure options align with existing decisions in `docs/DECISIONS.md` and the stack in `AGENTS.md`.

## Steps

### 1. Review Issue and Existing Decisions
- Obtain the issue number `$ISSUE_NUMBER` from the invocation argument or context.
- Inspect the issue details:
  ```bash
  gh issue view <issue-number>
  ```
- Confirm the issue has the `needs-decision` label and understand the architectural question.
- Read `docs/DECISIONS.md` and `docs/AUDIT.md` for historical decisions and technical background.

### 2. Investigate the Problem
- Inspect the relevant files and architecture in the repository.
- Research viable alternatives, library constraints, performance, maintenance, and migration impact.

### 3. Author the Proposal Document
- Create a proposal file at `docs/proposals/<issue-number>-<topic>.md` (create `docs/proposals/` if it does not exist).
- Use the following structure:
  ```markdown
  # Proposal #<issue-number>: <Title>

  - **Issue:** #<issue-number>
  - **Date:** <YYYY-MM-DD>
  - **Author:** <agent-name>
  - **Status:** Proposed (awaiting owner decision)

  ## Context
  <Explain the problem, constraints, current architecture, and why a decision is needed.>

  ## Options Considered

  ### Option 1: <Name>
  - **Description:** <How this approach works>
  - **Pros:**
    - <Advantage 1>
    - <Advantage 2>
  - **Cons:**
    - <Disadvantage 1>
    - <Disadvantage 2>
  - **Impact on Stack & Dependencies:** <Changes to libs.versions.toml, build.gradle.kts, etc.>

  ### Option 2: <Name>
  - **Description:** <How this approach works>
  - **Pros:**
    - <Advantage 1>
    - <Advantage 2>
  - **Cons:**
    - <Disadvantage 1>
    - <Disadvantage 2>
  - **Impact on Stack & Dependencies:** <Changes to libs.versions.toml, build.gradle.kts, etc.>

  ## Recommendation
  <State the recommended option and explain why it is the best fit for FootballPulse.>

  ## Proposed Entry for docs/DECISIONS.md
  <Provide a draft D-XXX entry following the DECISIONS.md template ready to be recorded once approved.>
  ```

### 4. Link Proposal in Issue
- Add a comment to the GitHub issue linking to the proposal document:
  ```bash
  gh issue comment <issue-number> --body "Prepared architectural proposal: \`docs/proposals/<issue-number>-<topic>.md\`. Awaiting owner decision."
  ```
- Keep the `needs-decision` label until the owner makes an official decision.


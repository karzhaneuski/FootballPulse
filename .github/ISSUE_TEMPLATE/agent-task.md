---
name: Agent task
about: Task for an AI agent (Codex / Antigravity / Claude) or for a human
title: "[area] Short description of what to do"
labels: []
assignees: []
---

<!--
Labels: size (size:S / size:M / size:L) + assignee (agent:codex / agent:antigravity)
or needs-decision if there is a fork in the road. Routing rules are described in AGENTS.md, §8.
-->

## Context
<!-- Why this is needed. Link to the docs/AUDIT.md item if the task comes from there. -->

## Task
<!-- What exactly to do. Be specific and unambiguous. -->

## Definition of done (DoD)
- [ ] ...
- [ ] `./gradlew assembleDebug testDebugUnitTest lintDebug` passes locally and in CI

## Affected files
<!-- Expected list; the agent may deviate but must explain why in the PR. -->
- `...`

## Out of scope
<!-- What NOT to do in this task. -->
- Do not change dependency versions (unless that is the goal of the task).

## How to verify
<!-- Manual verification steps or tests that should appear. -->

## Dependencies
<!-- Blocked by: #N. Blocks: #M. Decision: D-XXX in docs/DECISIONS.md. -->

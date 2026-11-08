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

_No active handoffs._

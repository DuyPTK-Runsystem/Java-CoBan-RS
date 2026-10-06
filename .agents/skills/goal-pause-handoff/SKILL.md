---
name: goal-pause-handoff
description: Prepare a factual checkpoint when a user pauses or temporarily ends an unfinished goal, including active subagents, validation evidence, and a resumable backlog. Does not authorize pausing goals without the user's instruction.
---

# Goal Pause Handoff

Leave an unfinished goal in a stable, reviewable state with enough evidence for a fresh agent to continue.

## Respect the requested stopping boundary

Determine whether the user wants immediate pause or completion of the current bounded task before pause. For immediate pause, stop active work safely. For a final pass, let only that already-authorized pass settle; do not add new tasks, cleanup cycles, full-suite runs, or build runs merely to improve the handoff.

Tell active agents the boundary. Collect settled results and identify any still-running process or unresolved edit. Avoid abrupt interruption during a file write; stop at a safe boundary when feasible. Preserve worktree changes and backup artifacts. Do not resume idle agents solely to repeat status already available.

## Record the checkpoint

Use the project's existing Dev Note or handoff convention; avoid parallel competing reports. Include:

- Goal and approved scope, decisions still applicable, and explicit user pause request.
- Completed work and unfinished work, with source/test ownership and relevant paths.
- Each material gate as PASS, FAIL, or NOT RUN; exact command/filter, counts, report path, and whether evidence predates later edits.
- Scope limits: fixtures versus live runtime, selected tests versus full suite, mocks/H2 versus target database. A successful report-generation command does not prove valid coverage input.
- Residual findings separated into task scope and unrelated baseline. List actionable files/rules or representative failure evidence; do not assume all failures share one cause.
- Missing environment/configuration and access dependencies, without exposing secrets.
- Running services/processes left intentionally available and the minimal next verification steps.

Resolve conflicting counts against current artifacts when inexpensive. Otherwise retain the disagreement explicitly. Do not label partial compile or fixture proof as release completion.

## Pause and report

When a goal-status tool is available, set paused only after the explicit user request and at the agreed boundary. Do not mark an unfinished goal complete. If no status API exists, report the checkpoint without pretending to have changed an unavailable system state.

Give a short user-facing result linking the handoff, stating residual blockers and paused status. On resume, read the checkpoint, verify drift-prone evidence, and continue from the unfinished item rather than redoing completed work.


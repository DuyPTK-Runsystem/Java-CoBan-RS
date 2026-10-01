---
name: subagent-context-budget
description: Manage context capacity while coordinating explicitly authorized subagents, using available telemetry and handoffs. Apply before adding assignments or extending an agent's scope; does not authorize spawning agents.
---

# Subagent Context Budget

Preserve enough working context for agents to complete and validate their owned tasks. Follow the user's threshold; for this workflow the default limit is 70%.

## Establish ownership and capacity

Assign a bounded task with relevant paths, acceptance checks, ownership boundaries, and only the context it needs. Keep development and independent verification separate when requested. Respect available concurrency and model choices; context management does not expand delegation authorization.

Use actual context-utilization telemetry if exposed. Record its source and freshness. If unavailable, report UNKNOWN; token usage for the whole goal, message count, compaction, or elapsed time is not a context percentage. Do not claim a numerical estimate from those proxies.

## Before adding work

- Below or at 70% with current telemetry: add work only if it belongs to the authorized scope and the agent can retain the necessary evidence.
- Above 70%: do not add unrelated or newly expanded tasks. Permit only work necessary to finish the task already owned, including its validation or a concrete defect blocking completion.
- UNKNOWN: do not treat it as zero. Keep assignments narrow, avoid scope expansion, and request a concise checkpoint when needed to continue.
- A user-specified threshold or exception overrides this default.

Do not repeatedly ask for telemetry the environment cannot provide. Do not poll agents for routine status when a completion message or existing report suffices. A completed agent should remain idle unless its expertise or ownership is needed for a concrete authorized follow-up.

## Handoff when more work remains

At a safe boundary, obtain a compact checkpoint: scope, touched paths, decisions, unfinished edits, commands/results, process handles, blockers, and next action. Verify it against artifacts when useful. Use a fresh agent for additional scope only when authorized and capacity is available; otherwise keep the backlog for later.

Do not discard a near-complete task merely to reduce context usage, interrupt unsafe mutations, or assume a new thread automatically has adequate context. Reusing an agent or resuming after compaction still requires reading its checkpoint.

Keep user updates factual: telemetry value or UNKNOWN, scope decisions, and consequences for remaining work.


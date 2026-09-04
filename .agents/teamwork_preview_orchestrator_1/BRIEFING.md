# BRIEFING — 2026-09-04T09:48:40Z

## Mission
Chuyển đổi phân hệ quản lý học sinh sang phân hệ học vụ tích hợp v2 (/v2/students) trong AuthenticatedV2ShellView, kết nối tài khoản v3 và chuỗi nghiệp vụ học vụ v2.

## 🔒 My Identity
- Archetype: orchestrator
- Roles: orchestrator, user_liaison, human_reporter, successor
- Working directory: /home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/teamwork_preview_orchestrator_1
- Original parent: parent
- Original parent conversation ID: 806af660-e0f5-472f-8093-0ca256ee7e9c

## 🔒 My Workflow
- **Pattern**: Project
- **Scope document**: /home/duyptk/Coding/HoiNhapJava/Java-CoBan/PROJECT.md
1. **Decompose**: Survey (3 Explorers / Spec Miners) -> Assess -> Decompose & Delegate into milestones + E2E Testing Track -> Sub-orchestrators for milestones
2. **Dispatch & Execute**: Delegate to sub-orchestrators for milestones; monitor progress and synthesize results
3. **On failure** (in this order):
   - Retry: nudge stuck agent or re-send task
   - Replace: spawn fresh agent with partial progress
   - Skip: proceed without (only if non-critical)
   - Redistribute: split stuck agent's remaining work
   - Redesign: re-partition decomposition
   - Escalate: report to parent (sub-orchestrators only, last resort)
4. **Succession**: Self-succeed at 16 spawns: cancel crons, write handoff.md, spawn successor
- **Work items**:
  1. Survey & Initial Decomposition [DONE]
  2. Milestone 1: Navigation, Routing & Shell V2 Integration [in-progress]
  3. E2E Testing Track: 4-Tier Test Suite & Test Infra [in-progress]
  4. Milestone 2: Student List, Multi-dimensional Search & Safe Lifecycle [pending]
  5. Milestone 3: Student Creation & Account Provisioning V3 [pending]
  6. Milestone 4: Student Detail 4-Tab Workspace [pending]
  7. Milestone 5: Final E2E Test Verification & Adversarial Hardening [pending]
- **Current phase**: 2 (Implementation & E2E Testing Track)
- **Current focus**: Milestone 1 execution + Parallel E2E Testing Track

## 🔒 Key Constraints
- DISPATCH-ONLY orchestrator: NEVER write source code, NEVER run build/test commands, NEVER investigate code directly.
- Delegate all technical work to subagents via invoke_subagent.
- File-editing tools only for metadata/state files (.md) in .agents/.
- Zero tolerance for integrity violations: Forensic Auditor reports INTEGRITY VIOLATION => immediate milestone failure.
- Never reuse a subagent after it has delivered its handoff — always spawn fresh.

## Current Parent
- Conversation ID: 806af660-e0f5-472f-8093-0ca256ee7e9c
- Updated: 2026-09-04T09:36:12Z

## Key Decisions Made
- Completed Phase 0 Survey with 3 parallel agents. Created PROJECT.md with 19 inventoried features and 5 milestones.
- Dispatched Worker for Milestone 1 (Navigation, Routing & Shell V2 Integration).
- Dispatched parallel E2E Testing Track (Test Writer) to build 4-tier test infrastructure and test cases.

## Team Roster
| Agent | Type | Work Item | Status | Conv ID |
|-------|------|-----------|--------|---------|
| spec_miner_be_0 | teamwork_preview_spec_miner | BE Survey & Spec Mining | completed | 888642cb-814d-4dfb-82a0-c9b4a70266a4 |
| spec_miner_fe_0 | teamwork_preview_spec_miner | FE Survey & Spec Mining | completed | caac4838-2463-4cd4-b46a-f5e754391e7a |
| explorer_e2e_0 | teamwork_preview_explorer | E2E Integration Survey | completed | 79d25f52-4299-4d66-a336-a88f0033f806 |
| worker_m1_1 | teamwork_preview_worker | Milestone 1 Implementation | in-progress | 3a6e95f9-8310-4477-ae5f-588b5e412604 |
| test_writer_e2e_1 | teamwork_preview_test_writer | E2E Test Suite & Test Infra | in-progress | c258688a-bac5-4a20-9fe2-efd08a124934 |

## Succession Status
- Succession required: no
- Spawn count: 5 / 16
- Pending subagents: 3a6e95f9-8310-4477-ae5f-588b5e412604, c258688a-bac5-4a20-9fe2-efd08a124934
- Predecessor: none
- Successor: not yet spawned

## Active Timers
- Heartbeat cron: 275dfe1c-b9ce-4cb7-b542-547e4cb2fc91/task-28
- Safety timer: none
- On succession: kill all timers before spawning successor
- On context truncation: run `manage_task(Action="list")` — re-create if missing

## Artifact Index
- /home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/ORIGINAL_REQUEST.md — Original User Request
- /home/duyptk/Coding/HoiNhapJava/Java-CoBan/PROJECT.md — Global project plan and feature inventory

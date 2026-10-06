---
name: timetable-agent-debug
description: Debug the Java-CoBan-RS AI timetable proposal flow from request and logs through snapshot, model output, validation, retries, and persisted response. Use when investigating failed, invalid, conflicting, timed-out, or oversized timetable suggestions.
---

# Timetable Agent Debug

Use this skill to orient quickly in the timetable suggestion flow. Start from the reported symptom and read only the relevant path below; prefer current source and captured evidence over old Dev Notes.

## Read first

1. Check repo state and preserve unrelated worktree changes. Read `.codex/AGENTS.md` and any applicable `AGENTS.override.md` before editing.
2. For the contract, read `document/application-doc/v4/agent-contract/timetable-agent-prompt.md` and `document/application-doc/v4/agent-contract/timetable-proposal.schema.json`.
3. Trace the live implementation through these files as needed:
   - API entry: `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/timetableagent/controller/TimetableAgentController.java`
   - orchestration, snapshot, validation, and persistence files are under `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/timetableagent/`. Key names: `TimetableAgentOrchestrator`, `TimetableSnapshotService`, `TimetableAgentSnapshotDocument`, `TimetableAgentSnapshotEntries`, `TimetableAgentInputLimits`, `TimetableAgentProposalGenerator`, `TimetableAgentProviderCall`, `TimetableProposalValidator`, `TimetableAgentWeeklyDemandValidator`, `TimetableAgentDomainValidation`, `TimetableAgentEntryProjection`, `TimetableAgentProposalFactory`, and `TimetableAgentProposalMapper`.
   - model boundary: `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/timetableagent/ai/TimetableAgentProposalConverter.java`, `SpringAiTimetableModelGateway.java`
   - prompt and schema: `BE/BaiTap-RS/src/main/resources/ai/timetable/proposal-system.txt`, `BE/BaiTap-RS/src/main/resources/ai/timetable/timetable-proposal.schema.json`
   - authoritative overlap rules: `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/timetable/service/TimetableValidationService.java`

Do not read all files above by default. Use `rg` for the error code, log message, field name, and owning service, then follow only direct callers and relevant tests.

## Fast diagnosis by evidence

Classify the failure before proposing a fix:

- **Request rejected / 413:** inspect `TimetableAgentInputLimits`. `max-request-characters` and `max-preferences-characters` apply to request text; `max-context-characters` checks serialized `snapshotJson().length()` before the provider call. A log field named `requestLength` is only the user-request text length, not the full model payload.
- **Contract / JSON error:** compare the exact root and entry fields with the schema and `TimetableAgentProposalConverter` strict field checks. Distinguish malformed JSON, contract mismatch, proposal-size rejection, and semantic validation; they occur at different layers.
- **`NEEDS_INPUT`:** check the model's `unresolvedConstraints` and system prompt. `TimetableProposalValidator` treats model status `NEEDS_INPUT` as terminal and returns before backend candidate validation. Do not describe a model-originated missing-context claim as a backend validation result.
- **`CONFLICTS`:** read each validation issue's `code`, `path`, and `message`. Demand issues are evaluated per assignment and applicable week; overlap issues come from candidate versus retained entries and may be pairwise. Follow the validator that emits the exact code.
- **Timeout:** `TimetableAgentProposalGenerator` creates one deadline for the whole attempt loop; `TimetableAgentProviderCall` waits on each provider call using remaining time and maps timeout to gateway-timeout. Check effective runtime env/config and per-attempt timestamps; source defaults do not prove the running process's values.
- **Too many retries / repeated result:** inspect `budget()`, the shared deadline, repeated-failure/signature break conditions, and feedback growth. Configured model-call count is a maximum, not a guarantee every call will run.
- **Model output/logging:** gateway logs may intentionally omit raw output. Never infer exact model JSON from a digest, summary log, or validation issue codes alone. Prefer persisted proposal data or a controlled, privacy-safe reproduction if authorized.

## Correlating a log

- Correlate by request trace ID and `snapshotId`; keep attempt number, prompt/request length, validation status, issue codes, and timestamps in order.
- Separate model response fields (`modelStatus`, `constraintCodes`) from backend result (`validationStatus`, `issueCodes`). A 201 response may still contain `NEEDS_INPUT` or `CONFLICTS`; inspect the body.
- Verify deployed runtime settings rather than assuming `.env` or `application.properties` was loaded. Do not print keys, bearer tokens, or full private timetable data. If a credential is pasted into logs/chat, advise revocation/rotation without repeating it.
- Snapshot includes only fields serialized by `TimetableAgentSnapshotDocument.SnapshotSource`. Internal fingerprint data or fields on the Java `TimetableAgentSnapshot` record are not automatically sent to the model.
- For context-size questions, count serialized characters and unique context IDs. A compact assignment-to-teacher map costs one pair per unique assignment, not one pair per timetable entry. The configured snapshot character cap remains the final guard.

## Repair approach

1. State the evidence-backed failure layer and the smallest responsible component.
2. Check whether the model has enough information, whether backend validation is authoritative, and whether repair feedback identifies actionable fields/paths before changing prompt or code.
3. Preserve hard constraints, locked entries, date/class scope, existing API contracts, and user data. Do not weaken validators or suppress errors to make a proposal pass.
4. Inspect neighboring tests. Run tests or validation only when requested or authorized; when backend validation is requested, follow `.agents/skills/backend-validation/SKILL.md` and report each gate separately.
5. Do not create a Developer Plan or Dev Note for a read-only debug request unless the user asks. For an authorized code change, follow the repository's current instructions and honor any explicit instruction to omit plan/note artifacts.
6. Report what is confirmed, what remains a hypothesis, the files changed, validation actually run, and whether the live model/runtime flow was exercised.

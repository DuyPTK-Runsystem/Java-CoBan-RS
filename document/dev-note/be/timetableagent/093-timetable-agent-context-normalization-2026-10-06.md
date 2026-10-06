# Dev Note 093 — Timetable agent occupied-context normalization

- Plan: [Developer Plan 093](../../../dev-impl-plan/be/timetable/093-timetable-agent-context-normalization-2026-10-06.md). Authorization: user-approved normalization scope on 2026-10-06.
- Actual scope: published rows that overlap current rows on `(assignmentId, periodId, functionalRoomId)` contribute only uncovered dates to the model snapshot. Retained validation and both teacher-load paths merge overlapping intervals with the same key. Disjoint dates and different assignments, periods, or rooms stay separate. Canonical current rows and IDs remain intact for locked entries, diff, and persistence; no managed entity or database row is mutated.
- Files: added `TimetableAgentOccupiedContext.java`, `TimetableAgentDateInterval.java`, and `TimetableAgentOccupiedContextTest.java`; updated `TimetableAgentSnapshotEntries.java`, `TimetableAgentEntryProjection.java`, `TimetableAgentSnapshotLoads.java`, and `TimetableAgentDomainValidation.java`. Adjacent Plan 092 trace logging in `TimetableAgentProposalGenerator.java` remains a separate task.
- Important decision: the date subtraction walks the already merged occupied intervals once with a cursor and emits only uncovered fragments. Current snapshot rows retain their canonical IDs and representation; the captured Lan 3 data had 376 unique current occupied keys and 354 overlapping published copies.

## Validation Result

- `test`: PASS — 11/11 focused tests across `TimetableAgentOccupiedContextTest` (3), `TimetableProposalValidatorTest` (4), and `TimetableAgentWeeklyPatternTest` (4). Full suite NOT RUN on the final revision per the user's instruction.
- `checkstyle`: PASS — `checkstyleMain` succeeded with 980 repository warnings across 122 files; no new warning remains in the changed main files.
- `PMD`: FAIL — `pmdMain` reports 8 known baseline findings in `TimetableAgentDomainValidation.java`; no finding remains in the new normalization classes.
- `build`: FAIL — `build -x test` completed compile/JAR/assemble and `checkstyleTest`, then stopped at the same 8 PMD findings. `checkstyleTest` reported 351 repository warnings.

Commands ran from `BE/BaiTap-RS` with `GRADLE_USER_HOME="$PWD/.gradle-user-home"`, `--offline --no-daemon --max-workers=1`; Gradle commands required sandbox escalation for its local file-lock socket.

```text
test --tests '*TimetableAgentOccupiedContextTest' --tests '*TimetableProposalValidatorTest' --tests '*TimetableAgentWeeklyPatternTest'
checkstyleMain
pmdMain
build -x test
```

JaCoCo report generation ran with the focused tests. Provider, database, browser, and live runtime were NOT RUN. Four bounded code/test/debug rounds addressed test compilation and new Checkstyle/PMD findings; no rules, suppressions, or build configuration were changed.

- Deviations and remaining limits: the full test suite was intentionally omitted on the final revision. This note does not claim runtime/provider behavior or full-suite proof. PMD and `build -x test` remain blocked by the existing `TimetableAgentDomainValidation` findings.

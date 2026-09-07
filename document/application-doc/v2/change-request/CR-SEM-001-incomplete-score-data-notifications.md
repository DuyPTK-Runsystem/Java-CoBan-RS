# CR-SEM-001: Incomplete Score Data Notifications

## 1. Metadata

- Status: `Approved` (2026-08-25)
- Application-document version: `v2`
- Related module: Academic Structure / Semester
- Related requirements: `FR-SEM-005`, `FR-SEM-006`, `FR-SEM-007`, `FR-SEM-008`,
  `BR-SEM-004`, `BR-SEM-005`, `BR-SEM-006`, `BR-SEM-007`, `BR-SEM-008`, `BR-SEM-009`
- Related Developer Plan: `document/dev-impl-plan/be/academic/040-semester-completeness-notification-cr-sem-001-2026-08-25.md` (and historical Plan 027)
- Scope owner: Academic Structure and future Assessment/Scoring notification flow

### Amendment 040.1 (2026-09-03)

Notification delivery is email-only. The system records delivery success only when the mail sender returns successfully; it does not claim recipient receipt. Legacy `IN_APP` rows are not automatically changed or deleted by migration. If they exist during rollout, the migration fails safely and requires a separately approved data-remediation plan.

## 2. Reason for change

The semester lifecycle is standardized as:

```text
DRAFT -> ACTIVE -> LOCKED -> CLOSED
```

The current baseline requires an incomplete-score report when a semester is automatically
locked, but does not define what incomplete data means, when the report is generated, how
duplicate notifications are prevented, or how failures are retried. This CR expands
`BR-SEM-006` without changing the current rule that incomplete data does not prevent locking.

This CR also records the notification contract needed by a later implementation plan. It does
not authorize implementation of email delivery, SMTP configuration, scheduler code, or score
queries in Plan 027.

## 3. Decision and lifecycle impact

### 3.1. Semester lifecycle

- `DRAFT`: semester configuration is editable and not yet in operation.
- `ACTIVE`: semester is in operation and applicable academic data may be processed.
- `LOCKED`: direct teacher score mutation is blocked; authorized users may still view data
  and perform explicitly allowed correction/reopen workflows.
- `CLOSED`: semester is read-only and represents the final lifecycle state.

The lifecycle is forward-only during the normal flow. Reopening a `LOCKED` semester requires
the existing authorization, reason, actor and timestamp audit required by `BR-SEM-009`.
Reopening a `CLOSED` semester is not introduced by this CR and requires a separate approved
change request.

### 3.2. Locking and incomplete data

The system may move a semester to `LOCKED` when either condition in `BR-SEM-004` is met:

1. an authorized academic-office user confirms the lock; or
2. the automatic-lock time is reached, including the 45-day rule after semester end.

Incomplete score data is reported after the lock decision. The report is informational and
must not roll back or block the lifecycle transition, consistent with `BR-SEM-007`.

## 4. Expanded business rules

The following rules extend `BR-SEM-006`:

- `BR-SEM-006-01`: at each configured checkpoint, the system evaluates score completeness
  for the target semester using the authoritative Assessment/Scoring query.
- `BR-SEM-006-02`: a result identifies the semester, checkpoint, evaluation time, affected
  scope, completeness status, and a summary of missing or incomplete data.
- `BR-SEM-006-03`: an empty result is valid; the system must record that the checkpoint was
  evaluated and must not send a false incomplete-data notification.
- `BR-SEM-006-04`: each `{semester, checkpoint}` evaluation is idempotent. Re-running a
  checkpoint must not create duplicate reports or duplicate notifications.
- `BR-SEM-006-05`: notification delivery failure must be retained with failure context and
  be eligible for a bounded retry/outbox workflow defined by the implementation plan.
- `BR-SEM-006-06`: a failure to evaluate completeness or deliver a notification must not
  change the semester lifecycle state or unlock a locked semester.
- `BR-SEM-006-07`: generated reports and delivery attempts must carry audit metadata,
  including semester, checkpoint, execution time, status and correlation/request identifier
  when available.
- `BR-SEM-006-08`: the report must not expose student data beyond the recipient's authorized
  scope. Recipient selection and access filtering must follow the authorization contract of
  the Assessment/Scoring module.

## 5. Checkpoint contract

The implementation in Plan 027 must support 11 deterministic checkpoints around the automatic-lock time
`t`. `t` is the effective automatic-lock checkpoint for the semester, calculated from the
semester dates and the approved 45-day rule.

The schedule for Plan 027 is:

```text
t-45d, t-30d, t-14d, t-7d, t-3d, t-1d,
t,     t+1d,  t+3d,  t+7d,  t+14d
```

The checkpoint timezone is `Asia/Ho_Chi_Minh`. At each checkpoint, Plan 027 only evaluates
completeness and returns an output decision; it does not send a notification.

The output must contain one of these decisions:

- `NEEDS_NOTIFICATION`: completeness data is incomplete and a notification would be needed.
- `NO_NOTIFICATION`: completeness data is complete, empty, or no notification is required.

The output also identifies the semester, checkpoint and evaluation time, together with the
summary needed by a later notification workflow. Delivery, persistence, idempotency and retry
are outside Plan 027.

## 6. Notification and persistence contract

The future implementation plan must define:

- a completeness query or service owned by Assessment/Scoring;
- a persistent evaluation/notification record keyed by `{semester, checkpoint}`;
- states for evaluation and delivery, including success and failure;
- recipient resolution and authorization filtering;
- notification content/template versioning;
- bounded retry, backoff, dead-letter or manual recovery behavior;
- correlation/request identifiers and audit payload;
- retention and privacy handling for report details.

Plan 027 does not implement email delivery, SMTP/provider configuration, templates,
notification persistence, idempotency or retry. Those concerns must be implemented only under
a separately approved plan or implementation note.

## 7. Compatibility and non-goals

- No change to legacy `/api/v1/**` APIs.
- No score-entry API, score calculation, transcript or assessment schema is introduced here.
- No change to `BR-SEM-007`: incomplete data does not prevent locking.
- No new semester status beyond `DRAFT`, `ACTIVE`, `LOCKED` and `CLOSED`.
- No automatic `CLOSED` transition is defined by this CR.
- No assumption that a teacher role or homeroom assignment grants score access.

## 8. Acceptance criteria for a future implementation

- The four-state lifecycle is persisted and validated consistently across API, service and
  migration layers.
- Manual and automatic locking produce the same auditable lifecycle result.
- Every configured checkpoint returns one notification decision for a semester evaluation.
- `NEEDS_NOTIFICATION` and `NO_NOTIFICATION` are distinguishable outputs.
- Evaluation failure does not change the semester lifecycle state or unlock a locked semester.
- The output does not expose data outside the evaluation scope.
- Tests cover boundary dates, duplicate execution, retry failure, empty results and lifecycle
  failure isolation.

## 9. Open decisions before implementation

- Confirm the 11-checkpoint schedule listed above.
- Confirm the source-of-truth query and owner in Assessment/Scoring.
- Define notification delivery, persistence, recipient roles, privacy, retry and recovery in a
  separate implementation plan.

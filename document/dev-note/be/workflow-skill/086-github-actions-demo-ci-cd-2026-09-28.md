# Plan 086 — GitHub Actions demo CI/CD

## Plan and approval

- Related Developer Plan: [Plan 086](../../../dev-impl-plan/summary/086-github-actions-first-demo-seed-2026-09-28.md).
- Approval: user approved Plan 086 and selected GitHub Actions.
- This note records only the CI/CD workflow, read-only DB verification scripts, and their operator runbook. Backend code/migration/test changes are documented separately by the backend implementation owner.

## Actual scope completed

- Added one GitHub Actions workflow for PR/push quality gates and release-branch demo operations. FE gates and focused BE tests (DemoSeedCompletionTest, DemoDataSeederIntegrationTest, DemoNotificationSeedRunnerTest, DemoSeedActivationTest), bootJar, checkstyleMain, and pmdMain are required; full BE tests and pmdTest run as separate steps in a non-release-gating observation job.
- Added explicit manual operations for first bootstrap, ordinary deploy, rollback, and protected `private-cutover`. Release images use the full commit SHA; cutover requires a valid immutable `cutover_image` already active in the parallel app.
- Added a single protected GitHub Environment approval job before release operations. Subsequent workflow jobs use repository variables/secrets.
- Added read-only MySQL preflight that checks the completion marker and canonical Plan 081 fixture keys across academic years, identities, assignment, room mapping, timetable, historical academic data, placement, notifications, and scorebook.
- Added postflight marker/target/image verification. Bootstrap then updates ACA to a new revision with the same image and explicit APP_SEED_DEMO_ENABLED=false; the workflow checks Single mode, one active revision, and readiness.
- Added a cleanup job that attempts to force the bootstrap image back to false when backend startup or postflight fails.
- Added production FE build/deploy through Vercel CLI after backend postflight succeeds.
- Preserved the user's existing deploy-handoff.md; no cloud setting or its contents were changed.

## Files changed

- Workflow: `.github/workflows/demo-ci-cd.yml`.
- Private network/Log Analytics/ACA Job infrastructure and exact Job-scoped GitHub role: `infra/plan086-vnet.bicep`, `infra/plan086-github-job-role.json`, `infra/modules/plan086-acr-pull-role.bicep`, `infra/modules/plan086-keyvault-secret-reader-role.bicep`.
- Read-only DB gates and network helpers: `scripts/ci/demo-seed-preflight.sh`, `scripts/ci/demo-seed-postflight.sh`, `scripts/ci/run-demo-network-job.sh`, `scripts/ci/verify-demo-azure-network.sh`, `scripts/ci/verify-demo-private-network.sh`, `scripts/ci/verify-demo-mysql-readonly.sh`, `scripts/ci/deploy-plan086-network.sh`.
- Operator instructions and GitHub/Azure/Vercel setup: document/deploy-runbook/github-actions-demo-operations.md.
- This Dev Note and the global/backend Dev Note summaries.

## Decisions

- DB verification runs inside the VNet-attached manual ACA Job. GitHub-hosted runners use OIDC only to start the exact Job execution and poll it; they do not connect to MySQL. The Job identity has ACR pull and Key Vault Secrets User only on the exact DB secret scope. Job `start/action` permits template override, so treat the protected OIDC identity as able to exercise the Job identity/secret; use a dedicated SELECT-only DB principal.
- Bootstrap refuses a marker that already exists and refuses an absent marker if any canonical Plan 081 key is present. Such a DB requires manual inspection; the workflow has no partial-data override.
- Normal deploy and rollback always set the seed flag to false. Rollback selects a previously published 40-character SHA image and does not reverse Flyway migrations.
- ACA must already use Single revision mode. Health gating uses /actuator/health/readiness because the handoff records overall health as DOWN.
- User-facing deploy-handoff data remains unverified; runbook requires readback from GitHub/Azure/Vercel before first cloud execution.

## Validation evidence

- Independent QA: workflow parsed with PyYAML — PASS.
- Independent QA: bash -n for both checked-in scripts and all 20 inline workflow run blocks — PASS.
- Independent static schema review confirmed marker and fixture query aliases/columns against repository entity/migration definitions — PASS.
- SQL was not executed against MySQL. No tests, GitHub workflow run, Azure, Vercel, browser, or live smoke check was run — NOT RUN.
- The current baseline report from the parent/QA handoff is full BE test FAIL (Placement save/saveAndFlush failures and OOM) plus about 400 pmdTest findings. This implementation did not rerun either check. The workflow's baseline observation job reports both as distinct GitHub steps and is not in the deployment dependency graph; its failure remains visible as a failed job.
- actionlint and shellcheck were unavailable in the QA environment.

## Deviations and remaining prerequisites

- No self-hosted runner VM or NAT Gateway is in scope. A custom-VNet workload-profile ACA environment and manual ACA Job execute DB checks privately. The parallel ACA app must be provisioned manually from captured current runtime settings.
- Before first use, configure the protected demo Environment/reviewer and release-branch restriction; repository variables/secrets listed in the runbook; the branch-specific Azure OIDC federated credential and least-privilege Azure roles; ACA ACR pull access and Single mode; private DB read access/Key Vault access; and Vercel project IDs/token.
- The workflow has not been run with the actual repository branch, cloud resource names, private route, marker, or Vercel project. The DB queries need a read-only run against the intended target before bootstrap.
- Perform the first seed only after configuration readback and manual DB preflight; record workflow run, image SHA, ACA revision, marker row, readiness, Vercel URL, and representative v2/v3 live smoke.


## Read-only cloud readiness audit (2026-09-28)

- The `secondary` remote branch `training/duyptk/student-management-deploy` exists at `708f136b1558498099b395aeed043d9471a1cf01`. `.github/workflows/demo-ci-cd.yml` is not present on that remote branch, and the repository Actions API returned zero runs. No GitHub UI tab was open, and opening an additional browser tab was unavailable in this CUA session; environment, repository secrets/variables, runner registration, and branch protection remain unverified.
- Azure Portal lists the ACA standard/demo apps, ACR `javacobanrsdemo26`, Key Vault `kv-javacobanrsdemo26`, and MySQL Flexible Server `java-coban-rs-demo-2026`. ACA `acae-java-coban-rs-standard-2026` is an Express environment with no custom VNet and public network access enabled. Its attached app identity has no federated identity credentials; this does not establish whether a separate GitHub OIDC principal exists.
- MySQL Networking showed public access enabled, “Allow public access from any Azure service within Azure” enabled, a firewall rule for `0.0.0.0–255.255.255.255`, and no private endpoints. This is incompatible with the runbook's assumption of a private DB route. Actual application traffic path was not traced.
- Vercel project `duyptk-runsystem/java-co-ban-rs` exists and a Production deployment for commit `708f136...` was Ready. Project Production branch tracking is `training/duyptk/student-management`, while the workflow release branch/deployment is `training/duyptk/student-management-deploy`. The workflow uses `vercel build --prod` and `vercel deploy --prebuilt --prod`, which target Vercel Production; the branch mismatch therefore remains a production release-control issue even though CLI deployment can promote to Production. Only the masked presence of `VITE_API_BASE_URL` was observed; no value was revealed. Vercel CLI token/project linkage for GitHub Actions remains unverified.
- Safe network direction: keep current rules unchanged until a parallel private path is proven. Establish a workload-profile ACA environment with custom VNet and manual ACA verification Job; add MySQL Private Link/private DNS reachable from that VNet; deploy and verify DB/readiness/preflight on the parallel app; cut traffic only after verification; then disable public access and remove the allow-all rule. No cloud setting was changed. Microsoft references: [MySQL security overview](https://learn.microsoft.com/en-us/azure/mysql/flexible-server/security-overview), [MySQL Private Link](https://learn.microsoft.com/en-us/azure/mysql/flexible-server/concepts-networking-private-link), [ACA custom virtual networks](https://learn.microsoft.com/en-us/azure/container-apps/custom-virtual-networks?tabs=workload-profiles-env).
- `gh`, `az`, and `vercel` CLIs were unavailable. No GitHub workflow run, cloud mutation, DB query, deployment, or live smoke test was performed (**NOT RUN**).
- The Azure Container Apps UI accessibility snapshot exposed a secret value in tool output without an explicit reveal action. The value is intentionally omitted from this note and all reports. Rotate it through the approved process before any deployment. No secrets page was revisited.


## VNet and ACA Job amendment status (2026-09-28)

- User selected VNet. Current subscription inventory showed no VNets; external IPAM overlap remains unknown. Proposed VNet/subnets, WLP environment, MySQL Private Endpoint/private DNS, verifier Job, and parallel app are recorded in Plan 086 and the operator runbook. No Azure resources, app configuration, DNS, DB routes, credentials, firewall settings, or traffic have been changed.
- Independent QA static review: final workflow YAML and changed shell helpers passed syntax/flow checks; marker/target/image semantics and explicit protected cutover gates passed. QA did not execute SQL against Azure DB or deploy cloud resources.
- Final Bicep compile with official Azure CLI container `mcr.microsoft.com/azure-cli:2.78.0`: **PASS (exit 0, no Bicep diagnostics)** after cross-RG role assignments moved into target-scope modules and Log Analytics workspace creation was added. No ARM deployment validation or `what-if` was run.
- Subscription quota and regional SKU availability remain **UNVERIFIED**: the authenticated Azure quota blade returned an error and local `az` is unavailable. East Asia is confirmed as the existing resource region; WLP support is documented, but subscription capacity is not proven. Read-only inventory found no current VNet or Log Analytics workspace. IaC adds `log-java-coban-demo-2026` (PerGB2018, 30-day retention); ingestion/retention are variable usage. External CIDR/IPAM overlap is unknown. Cloud creation remains **NOT RUN** pending quota/SKU, external CIDR/IPAM, DBA verifier principal/secret, owner review, and root review.
- `private-cutover` is explicit `workflow_dispatch`; routine deploy/bootstrap/rollback do not close MySQL public access. Cutover requires an existing immutable 40-hex `cutover_image` in the configured ACR, sole active app revision matching that image, Plan 081 marker key/version/target matching the DB, and marker provenance that is a valid SHA image from the same ACR repository (marker provenance may be the historical bootstrap image, not the current cutover image). Before public-off it checks app readiness/private TLS route; afterward it postchecks. The verifier DB principal is enforced as USAGE+SELECT only; the Job identity reads only the exact DB secret. `jobs/start/action` still allows template override, so it remains restricted to a protected GitHub environment principal.
- Public retail model: MySQL Private Endpoint USD 0.01/hour global (~USD 7.30 per 730h), private DNS first zone USD 0.50/month and USD 0.40/million queries; ACA consumption Job max 30m at 0.25 vCPU/0.5 GiB ~USD 0.0135 per execution using East Asia retail rates. App runtime, PE data, logs, taxes/discounts remain variable. User removed the former $10/month cap as a gate; values are not a hard limit.


### Final IaC/read-only prerequisite update (2026-09-28)

- VNet workflow owner added a Log Analytics workspace to IaC so there is no secret-key lookup prerequisite. The workspace key is consumed internally by ACA environment configuration and is not an output or workflow secret.
- The verifier DB identity and dedicated Key Vault password secret have not been verified. Before running the Job, DBA must create a separate principal with only `USAGE` and `SELECT ON <demo_schema>.*`, then store its password in a dedicated Key Vault secret using an approved silent/secure flow. Do not inspect, print, or pass the password through command arguments, GitHub, or notes. Keep app writer credentials separate.
- Current read-only subscription inventory confirms East Asia resource group `rg-java-coban-demo` has the existing ACA/ACR/Key Vault/MySQL resources but no VNet or Log Analytics workspace. Quota/SKU could not be read because the portal quota view errored; local `az` is absent. No resource, identity, secret, firewall, route, DB, or app was mutated.


## Verifier identity correction and live preflight (2026-09-29)

- Related plan: Plan 086, approved. The user directly requested a separate `java_coban_verify` identity with only `USAGE` and `SELECT ON java_coban.*` and authorized implementation.
- Changed external configuration: GitHub repository Actions variable `DEMO_MYSQL_USER` from `java_coban_app` to `java_coban_verify`; GitHub UI readback showed the new value. No repository script or infrastructure file was changed. The application account was not modified.
- Current live ACA Job name is `aca-job-db-verify-2026`. The verifier Job now references the new version of Key Vault `mysql-password`. GitHub variable readback **PASS**; `verify-demo-mysql-readonly.sh` and `verify-demo-private-network.sh` were unchanged.
- The attempted method of trying several unrelated Key Vault secrets as administrator credentials was rejected by automatic approval review as overly broad credential discovery. No such probe ran. The user then explicitly identified `dbase-password` in `kv-javacobanrsdemo26` as the `javacobanadmin` credential source.
- Using that exact secret in memory, admin login **PASS**. Created `java_coban_verify`@`%` with `REQUIRE SSL`, granted only `SELECT ON java_coban.*`, and left `java_coban_app` unchanged. `SHOW GRANTS` returned exactly `USAGE ON *.*` and `SELECT ON java_coban.*`; login using the verifier password **PASS**.
- Updated the Key Vault `mysql-password` secret to a new random password without printing it. Secret version metadata changed to `4af81a29181f4e69a49cf5e11ab94511`. Re-read that secret in memory, logged in as the verifier, and checked `SHOW GRANTS FOR CURRENT_USER()` **PASS**, with effective grants classified `SELECT_ONLY`.
- Re-ran all jobs of `Demo CI and deployment` run [36543260647](https://github.com/DuyPTK-Runsystem/Java-CoBan-RS/actions/runs/36543260647), attempt 2, at 2026-09-29 15:58 ICT. It initially waited behind [run #10](https://github.com/DuyPTK-Runsystem/Java-CoBan-RS/actions/runs/36545215938) due repository workflow concurrency, then reached database preflight.
- A direct ACA Job probe immediately after the Key Vault update failed MySQL login from the Job while direct Key Vault password readback/login passed, indicating the Job had not picked up the new secret version. Updated only the verifier Job `mysql-password` Key Vault reference to the new versioned URI, preserving the same system-assigned identity. The next direct Job probe `aca-job-db-verify-2026-9u3ux4i` **PASS**; logs show `mysql_identity=SELECT_ONLY` and `private_network=PASS` with TLS MySQL PASS. This was ACA probe evidence before workflow attempt 2 started. An attempted cancellation of unrelated run #10 to release the concurrency lock was rejected by automatic approval review; no cancellation was performed by this task.
- Run #9 attempt 2 reached [database-preflight job 109344143568](https://github.com/DuyPTK-Runsystem/Java-CoBan-RS/actions/runs/36543260647/job/109344143568). Its ACA execution `aca-job-db-verify-2026-ev2mgo7` logged `mysql_identity=SELECT_ONLY` and `private_network=PASS`, then failed because normal `deploy` requires the Plan 081 completion marker. The Azure CLI preview-extension warnings were not the cause.
- Direct read-only MySQL inspection found no `app_demo_seed_completion` table; Flyway history ends at V27, and canonical Plan 081 academic years, students, and teachers each count zero. A separate ACA preflight execution `aca-job-db-verify-2026-m833giy` with `OPERATION=bootstrap` succeeded and logged `seed_state=clean-for-plan-081-keys` plus `mysql_identity=SELECT_ONLY` and `private_network=PASS`. This was a read-only classification; no bootstrap seed or deployment was run in this correction task.
- The intended protected `workflow_dispatch` bootstrap could not be started: GitHub Actions shows no **Run workflow** button. Git remote HEAD resolves to `master` (`c05dca2...`), and the workflow file is absent from `secondary/master` but present on `training/duyptk/student-management-deploy`. GitHub requires the workflow file on the default branch for `workflow_dispatch`. No default-branch change, alternate direct seeding, or bootstrap deployment was performed.


## Empty-schema bootstrap preflight (2026-09-30)

- Related Developer Plan: Plan 086, previously approved. User requested a fresh first deploy after dropping and recreating the demo schema; existing preflight queried application tables before Flyway could create them.
- Changed `scripts/ci/demo-seed-preflight.sh`: for `bootstrap` only, an absent completion marker plus zero objects in the configured schema returns `seed_state=empty-schema`. Any nonempty schema still follows the existing canonical fixture-key checks. Normal `deploy` and `rollback` still require a matching completion marker.
- Validation: `bash -n scripts/ci/demo-seed-preflight.sh` **PASS**; a local mocked MySQL run accepted the empty-schema path and rejected an existing schema with fixture keys **PASS**; `git diff --check` **PASS**. Live empty-schema ACA Job and full GitHub workflow **NOT RUN**. No Azure/MySQL mutation occurred for this code change.
- Deviation: the original preflight assumed Flyway tables already existed. The new branch handles only a truly empty schema, preserving fail-closed behavior for partial schemas.
- Remaining: publish the patch on the release branch; draft PR #7 proposes a dispatch-only default-branch entry and is not merged. After review, run the protected bootstrap and verify marker/seed flag. Live DB reset and bootstrap are separate operations.


## Bootstrap completion-marker wait (2026-09-30)

- Related Developer Plan: [Plan 086](../../../dev-impl-plan/summary/086-github-actions-first-demo-seed-2026-09-28.md), approved. This follow-up implements the approved requirement to wait for all seed `ApplicationRunner`s to finish and to preserve failure cleanup.
- Changed `scripts/ci/demo-seed-postflight.sh`: bootstrap postflight polls for `DEMO_FIXTURE_PLAN_081` for up to 900 seconds at 15-second intervals. Normal deploy and private-cutover keep their existing immediate marker check. On timeout, the verifier logs elapsed time, the missing marker result, and aggregate `academic_year`/`student`/`teacher` counts; the existing network-job wrapper retrieves the failed ACA execution logs.
- Changed `.github/workflows/demo-ci-cd.yml`: set the timeout and poll interval explicitly for the ACA postflight verifier step. Changed `scripts/ci/run-demo-network-job.sh` to forward both values into the ACA Job container. Existing failed-postflight cleanup still forces the seed flag off.
- Validation: `bash -n` for both changed scripts **PASS**; workflow YAML parse with PyYAML **PASS**; mocked delayed-marker success, timeout with aggregate-count evidence, non-bootstrap fail-fast, and ACA Job environment forwarding **PASS**; `git diff --check` **PASS**. Live ACA/MySQL and GitHub Actions run **NOT RUN**.
- Deviations: none.
- Remaining risk: the 900-second allowance is not verified against a live full seed run; if runner completion consistently takes longer, the workflow will fail closed and retain timeout diagnostics.


## ACA replica defaults after run #14 (2026-09-30)

- Related Developer Plan: [Plan 086](../../../dev-impl-plan/summary/086-github-actions-first-demo-seed-2026-09-28.md), approved. Run #14 push deploy reached backend update and failed with `--min-replicas: invalid int value: ''`; live ACA scale readback was min/max `1/1`.
- Changed `.github/workflows/demo-ci-cd.yml`: normal deploy defaults missing `ACA_MIN_REPLICAS` and `ACA_MAX_REPLICAS` repository variables to `1`, and validates nonnegative integer bounds before calling `az containerapp update`. Bootstrap continues to use explicit `1/1`. No DB or ACA resource was changed by this fix.
- Validation: workflow YAML parse, extracted deploy shell syntax and replica-bound cases, and `git diff --check` **PASS**. Live GitHub workflow **NOT RUN** for this patch.
- Deviation: none. After run #14 failed, a read-only MySQL query found the Plan 081 completion marker for image `51e75915` and 160 students. Run #14 DB preflight had succeeded. The earlier partial-seed observation is no longer current; a normal deploy can proceed if its preflight continues to pass. Live deploy after this patch remains unverified.


## Wrapped Actuator health response in CI gates (2026-09-30)

- Related Developer Plan: [Plan 086](../../../dev-impl-plan/summary/086-github-actions-first-demo-seed-2026-09-28.md), approved.
- Run #15 showed `/actuator/health` returns the standard REST envelope with status under `data.status`; the workflow had asserted root `.status`, so a healthy response failed the jq gate. Updated all three jq health assertions in `.github/workflows/demo-ci-cd.yml` to require `.data.status == "UP"` and emit an explicit jq error otherwise. Kept readiness checks that only require a successful HTTP response unchanged.
- Validation: workflow YAML parse and `bash -n` over all 29 inline run blocks **PASS**; jq mock for wrapped `UP` and clear failure on `DOWN` **PASS**; confirmed all three jq health assertions use the wrapped status **PASS**; `git diff --check` **PASS**. Live workflow/API rerun **NOT RUN**.
- Deviations: none.
- Remaining: run #15 was not rerun after this workflow edit, so remote confirmation is pending.


## Vercel CLI working directory after run #16 (2026-09-30)

- Related Developer Plan: [Plan 086](../../../dev-impl-plan/summary/086-github-actions-first-demo-seed-2026-09-28.md), approved. Run #16 passed backend deployment and DB postflight, then Vercel CLI `vite build` failed with `Could not resolve entry module "index.html"`. The project Root Directory is `FE`, and the workflow ran the CLI from `FE`; Vercel documents that the Root Directory setting also applies to CLI commands.
- Changed `.github/workflows/demo-ci-cd.yml`: run only the `vercel pull`, `vercel build`, and `vercel deploy --prebuilt` step from repository root. The separate FE production build remains in `FE`. This avoids applying `FE` twice while preserving the configured Vercel project root.
- Validation: workflow YAML parse, run-block shell syntax, repo `FE/index.html` readback, and `git diff --check` **PASS**. Live Vercel CLI build/deploy **NOT RUN** for this patch; next workflow run will verify it.
- Deviations: none. Remaining: approve the protected demo environment for the new run, confirm Vercel deployment, then run FE/API smoke.

# GitHub Actions demo CI/CD operations

## Scope and current evidence

- Workflow: `.github/workflows/demo-ci-cd.yml`.
- Frontend gates and focused backend release gates run on pull requests and pushes. A separate full backend test/PMD observation job reports failures without being a dependency of deployment. Pushes to the configured `DEPLOY_BRANCH` deploy after quality checks. `workflow_dispatch` supports `deploy`, `bootstrap`, `rollback`, and the separately protected `private-cutover` operation from that same branch. Routine deploy/bootstrap/rollback never changes MySQL public networking.
- Backend image tags are immutable Git commit SHAs in ACR. ACA must use Single revision mode. Bootstrap uses one replica and `APP_SEED_DEMO_ENABLED=true`; normal deploy and rollback set it explicitly to `false`.
- MySQL marker and fixture preflight runs inside the VNet-attached ACA manual Job `aca-job-java-coban-db-verify-2026`. GitHub-hosted runners use OIDC only to start the exact job execution and poll its terminal state; they do not connect to MySQL. The workflow does not add a public MySQL firewall rule or store the DB password in GitHub.
- Readiness is checked at `/actuator/health/readiness`; `/actuator/health` is not used because the handoff records its overall status as `DOWN` while readiness is `UP`.
- The checked-in workflow and runbook have not been run against GitHub, Azure, MySQL, or Vercel. Existing Azure/Vercel values in `deploy-handoff.md` remain historical, unverified inputs until read back from those services.
- Read-only portal audit on 2026-09-28 confirmed the selected ACA environment `acae-java-coban-rs-standard-2026` is Express, has no custom VNet, and has public network access enabled. MySQL `java-coban-rs-demo-2026` has public access enabled, Azure-services access enabled, a `0.0.0.0–255.255.255.255` firewall rule, and no private endpoint. The intended private DB route does not currently exist based on these settings; actual app egress was not traced. **Do not run bootstrap/deploy or close these rules until a private route is built and verified.**
- The visible Vercel project Production branch tracking is `training/duyptk/student-management`; the workflow release branch and observed Production deployment branch are `training/duyptk/student-management-deploy`. The workflow's `vercel deploy --prebuilt --prod` explicitly promotes to Production regardless of Git-based automatic branch tracking. Choose and approve one release branch and align GitHub `DEPLOY_BRANCH`, the Azure OIDC federated subject, and Vercel Production branch tracking before first run.
- The workflow is not yet present on the remote branch; the GitHub Actions API showed zero runs. GitHub Environment, secrets/variables, branch protection, Azure deployment OIDC principal/role assignments, ACA Job system identity grants, and Vercel CLI token linkage remain unverified. Cloud rollout is **BLOCKED** pending those checks.

## Required GitHub configuration

Create a protected GitHub Environment named demo and restrict it to the release branch. Require a reviewer. The workflow references it once in deployment-approval, so approval gates the subsequent deployment jobs without prompting once per job.

Configure these repository-level Actions variables:

| Name | Value |
| --- | --- |
| DEPLOY_BRANCH | The exact approved release branch name. |
| ACR_NAME | ACR resource name without .azurecr.io. |
| ACR_REPOSITORY | Backend repository path in ACR, for example baitap-rs. |
| AZURE_RESOURCE_GROUP | Resource group containing ACA. |
| ACA_NAME | Existing backend Container App name. |
| ACA_MIN_REPLICAS / ACA_MAX_REPLICAS | Normal operation replica limits. |
| DEMO_SEED_TARGET_ID | Stable identifier for this exact MySQL target; must match the marker row. |
| DEMO_MYSQL_HOST / DEMO_MYSQL_DATABASE / DEMO_MYSQL_USER | Private DB connection coordinates and a read-only preflight user. |
| DEMO_KEY_VAULT_NAME | Key Vault holding the existing DB credential. |
| DEMO_MYSQL_PASSWORD_SECRET_NAME | Key Vault secret name for the DB credential. |
| VITE_API_BASE_URL | The BE origin used for the CI FE build, without /api. |
| VERCEL_ORG_ID / VERCEL_PROJECT_ID | IDs for the Vercel FE project. Set its root directory to FE. |

Configure these repository-level Actions secrets:

- AZURE_CLIENT_ID, AZURE_TENANT_ID, AZURE_SUBSCRIPTION_ID: OIDC workload identity settings. Add a federated credential for this repository's release branch (repo:OWNER/REPO:ref:refs/heads/BRANCH); do not add a client secret.
- VERCEL_TOKEN: project-scoped token sufficient to pull production settings and deploy this Vercel project.

The workflow uses the Vercel CLI to pull production project settings, build, and deploy the prebuilt output. Keep VITE_API_BASE_URL in the Vercel production environment aligned with the ACA origin. The workflow injects the live ACA ingress origin for its production FE build.

## Azure identities and permissions

- GitHub-hosted `ubuntu-latest` uses federated OIDC to call the ACA Job start and status APIs only. It must not query MySQL directly. The ACA Job runs in the verified VNet-attached WLP environment and has the MySQL client/TLS verification script and read-only-account verifier in its immutable ACR image.
- The workflow OIDC identity has deployment permissions for backend ACR build/push, Key Vault reference metadata, and the named ACA app. Assign custom role `infra/plan086-github-job-role.json` at the exact verifier Job resource scope; it grants `Microsoft.App/jobs/read`, `Microsoft.App/jobs/start/action`, and `Microsoft.App/jobs/executions/read`. The start action accepts a template override and can therefore use the Job’s known secret references/system identity. Treat this permission as secret and identity use; assign it only to the protected GitHub release environment principal. Do not grant it to general workflow contributors. The Job managed identity has `Key Vault Secrets User` at the exact DB secret scope, not the whole vault; keep the Job DB account dedicated and read-only.
- Verifier Job `aca-job-java-coban-db-verify-2026` uses a system-assigned managed identity with `AcrPull` at the ACR scope and `Key Vault Secrets User` at the exact DB secret scope (not the whole vault). Its Key Vault secret reference is mounted as `mysql-password`; do not copy the password into GitHub or workflow output. The parallel ACA app must use its own managed identity for ACR pull and secret references, and its ingress must serve readiness.
- The DB user used by preflight should have `SELECT` on the marker and canonical fixture tables only. Keep application write credentials in Azure Key Vault/ACA secret references. The current server exposes a full IPv4 allow-all firewall rule; retain current settings only until the ACA Job and parallel app have a proven private route. Build a custom-VNet workload-profile ACA environment, MySQL Private Link/private DNS, and verifier Job; deploy and verify TLS/DB/readiness/preflight from the Job and app; cut traffic; then disable public access and remove the allow-all rule. Do not switch the live app/network in place without a planned maintenance/cutover procedure.
- Configure GitHub concurrency and environment protection as checked-in workflow settings; the workflow serializes all events for this repository and does not cancel a running deployment.

## Plan 086 VNet staging amendment

### Verified inventory and target layout

- Azure Network Foundation inventory for the active subscription showed no VNets. The current app `aca-java-coban-rs-standard` runs in Express environment `acae-java-coban-rs-standard-2026` in East Asia. Express has no VNet-routed outbound path. Current MySQL `java-coban-rs-demo-2026` is public-access mode with public access and the allow-all rule still enabled; no Private Endpoint exists.
- Proposed new resources (not created): VNet `vnet-java-coban-demo-2026` (`10.42.0.0/16`); dedicated ACA workload-profile subnet `snet-aca-infra` (`10.42.0.0/23`, delegated `Microsoft.App/environments`); Private Endpoint subnet `snet-private-endpoints` (`10.42.2.0/27`). The selected subscription has no existing VNet ranges; verify again before create and recalculate if an external network will be peered. No runner VM/subnet or NAT Gateway is required for the current design.
- New workload-profiles environment `acae-java-coban-rs-vnet-2026` keeps external ingress for the Vercel FE; parallel app name `aca-java-coban-rs-vnet-2026` uses seed flag false and is provisioned manually from the captured current app runtime settings. Current Express app remains online until cutover acceptance. Add MySQL Private Endpoint `pe-mysql-demo-2026` to `privatelink.mysql.database.azure.com`, link that zone to the VNet, and configure/verify the server DNS record. Keep the standard MySQL FQDN in application configuration. The ACA manual Job `aca-job-java-coban-db-verify-2026` runs with 0.25 vCPU/0.5 GiB, parallelism/completion count 1, 1,800-second timeout, and retry 0. It reads the Key Vault password via system identity and validates TLS/MySQL/preflight from the private path. No VM, self-hosted runner, or NAT is required. Verify WLP regional/SKU availability, quota, permissions, and external/CIDR overlap before creation. Current subscription inventory contains no Log Analytics workspace, so IaC creates `log-java-coban-demo-2026` (PerGB2018, 30-day retention); quota/SKU is still unverified because the portal quota blade errored and local `az` is unavailable. DBA setup is also required before Job execution: create a separate verifier principal with only `USAGE` and `SELECT ON <demo_schema>.*`, and add its password under a dedicated Key Vault secret via an approved silent/secure process. Do not read or print the password, pass it in command arguments, or place it in GitHub; keep the app writer identity separate.

### Job start privilege and database secret scope

- Microsoft documents `Microsoft.App/jobs/start/action` as permission to use job secrets whose names the caller knows and managed identities made available to that job, even when `listSecrets` is not granted. The start payload also overrides the Job template, including its command/image. Therefore the protected GitHub OIDC principal must be treated as able to exercise the Job identity and DB credential; assign the custom role only at the named Job scope to the protected `demo` environment and trusted release branch.
- The Job managed identity must receive `Key Vault Secrets User` only on the exact DB secret resource (`.../vaults/<vault>/secrets/<secret>`), not the entire vault. The GitHub identity itself must not receive Key Vault data-plane read permission. The verifier DB account should be dedicated and read-only for Plan 081 marker/preflight queries; `scripts/ci/verify-demo-mysql-readonly.sh` enforces only `USAGE` and `SELECT` on the configured schema. If secret-scope RBAC is not supported by the active vault/ACA integration, stop and use a dedicated vault rather than broadening access to a shared vault.

### Cost model (retail estimate, variable usage)

- Microsoft Retail Prices API East Asia rates queried 2026-09-28: Standard Private Endpoint is a global meter at USD 0.01/hour (about USD 7.30 for 730 hours); Private Endpoint processed traffic is USD 0.01/GB ingress and USD 0.01/GB egress. Azure Private DNS pricing lists USD 0.50/month for the first private zone and USD 0.40 per million private queries. These are public retail estimates, not Azure for Students net charges. Sources: [Retail Prices API](https://prices.azure.com/api/retail/prices?api-version=2023-01-01-preview), [Retail API documentation](https://learn.microsoft.com/en-us/rest/api/cost-management/retail-prices/azure-retail-prices).
- ACA Consumption profile has no fixed management fee unless a Dedicated workload profile, ACA environment private endpoint, or planned maintenance feature is enabled. Do not enable those features for this design. East Asia ACA active compute meters: USD 0.0864/vCPU-hour and USD 0.0108/GiB-hour. The verifier Job max run is 30 minutes at 0.25 vCPU/0.5 GiB, approximately USD 0.0135/execution before any subscription free grants. The parallel app is min 1 during bootstrap; its runtime/configuration and traffic determine variable ongoing usage. Existing Log Analytics ingestion and retention are additional and depend on volume.
- User removed the USD 10/month cap as a rollout gate. These figures are usage estimates only; quota/SKU verification, subscription-specific pricing, and cost monitoring remain prudent before provisioning. Do not describe the model as a hard spend limit.

### Gates, cutover, rollback

1. QA/root review the topology; verify region/SKU/quota/cost and any CIDR overlap. If secure reuse of the current app credential cannot be done without exposing or changing the value, stop for owner setup. Do not read/export secret values.
2. After static QA of IaC/workflow and verified subscription quota/SKU availability, create VNet/subnets, MySQL Private Endpoint/DNS, workload-profile ACA environment, and verifier Job while public MySQL access/firewall remain unchanged. Provision the parallel ACA app manually from captured current runtime settings. Current Express app remains in service.
3. Start the exact manual ACA Job execution via protected GitHub OIDC; prove the standard MySQL FQDN resolves to the approved PE IP and a TLS-verified MySQL query and read-only marker/preflight reach the intended DB. In parallel, verify app readiness with seed flag false and app-to-DB access. GitHub-hosted runners need only authorized ARM/OIDC job-start and polling access. Do not run bootstrap seed as part of network QA.
4. Root approves a separate cutover after QA evidence. Point FE at the new app and smoke test while public MySQL access remains enabled. Keep old app available through acceptance.
5. After QA evidence and frontend traffic acceptance, explicitly dispatch `private-cutover` on the protected release branch with `cutover_image` set to the existing immutable ACR image reference containing the 40-character commit SHA, then approve the `demo` environment. Before public-off, it verifies this is the sole active ACA revision and its image matches `cutover_image`; the private ACA Job verifies marker key/version/target ID and marker `deployment_ref` is a valid immutable 40-character image tag from the same configured ACR repository (it may be the historical bootstrap image); then app readiness and private DNS/TLS are checked. Only then it disables MySQL public access/removes the allow-all rule and runs post-cutover private DNS/TLS/Job/app readiness checks. It does not build or deploy an app image or seed. Routine deploy/bootstrap/rollback cannot trigger this operation.
6. After public access is disabled, the old Express app cannot connect to MySQL and is not a valid rollback target. Rollback uses a previously tested, schema-compatible image in the new workload-profiles ACA app. Re-enabling public DB access is a separate incident decision, never automatic.

No Azure resource, route, DNS, app, credential, firewall, or traffic change has been made for this amendment.

## Release gates and backend baseline report

The release dependency is the aggregate Required release quality gates job. It requires FE lint/tests/build and BE tests DemoSeedCompletionTest, DemoDataSeederIntegrationTest, DemoNotificationSeedRunnerTest, and DemoSeedActivationTest, followed by bootJar, checkstyleMain, and pmdMain. The full BE test suite and pmdTest run as separate steps in Backend full-suite baseline observation (non-release-gating), so pmdTest still executes when tests fail; release-image does not depend on it. A failure there remains a visible failed GitHub job and makes the overall workflow run red, while the release dependency may proceed if its own gates pass. Do not make the observation job a required branch-protection check while the documented baseline is unresolved.

The current baseline report from the independent QA handoff is full test FAIL (Placement save/saveAndFlush failures and OOM) and pmdTest about 400 findings. This task did not rerun those checks. The diagnostic job will report its actual result on each workflow run; never summarize it as PASS merely because deployment gates pass.

## First seed bootstrap

1. Confirm repository-level Actions variables/secrets, the protected demo approval environment, and Azure OIDC/ACA Job identity readiness. Read back the selected ACA, ACR, Key Vault, DB target, and Vercel project from the services. Do not infer them from `deploy-handoff.md` alone.
2. Run `Demo CI and deployment` manually on `DEPLOY_BRANCH` with operation `bootstrap`.
3. The read-only DB preflight requires no DEMO_FIXTURE_PLAN_081 marker and no canonical Plan 081 keys: academic years 2026-2027 and 2025-2026; identities academic.office, students STU2600001–STU2600160, and teachers GV001–GV020; functional-room codes/mappings; scoped class/subject assignments; timetable audit/revision actions SEED_PLAN_081_FULL_V2 and SEED_PLAN_081_G7; historical enrollments/transcripts for STU2600041–STU2600080; placement scopes PLACE-081-*; Plan 081 notification idempotency keys; and scorebook 2026-2027 / 6A1 / TOAN / HK1. Any found key without a matching completion marker stops the run for manual review. Do not override this gate or rerun after partial writes.
4. The workflow builds the current commit image, changes ACA to one replica with the seed flag true and explicit target/image metadata, and waits for readiness. Backend startup runs Flyway before the seed runners. The completion runner writes `app_demo_seed_completion` only after all runners succeed.
5. Private DB postflight requires exactly the marker key `DEMO_FIXTURE_PLAN_081`, fixture version `PLAN_081_V1`, configured target ID, and bootstrap image reference. Only then it changes ACA to a new revision with the same image and explicit seed flag false. It verifies Single revision mode, one active revision, the explicit false setting, and readiness.
6. Once backend postflight succeeds, CI deploys FE to Vercel. Check the Vercel URL, API origin, CORS, sign-in, and a representative v2 and v3 read flow. Store workflow run, image SHA, ACA revision, marker readback, and smoke result in the deployment record.

If preflight finds fixture keys without a marker, stop and inspect the private DB before further action. If the bootstrap runner or marker check fails after the ACA revision was changed to true, a cleanup job attempts to deploy the same image with seed false. Verify the cleanup job and ACA active revision. If cleanup itself fails, manually update the app to `APP_SEED_DEMO_ENABLED=false` and inspect Flyway/runner logs plus the marker and fixture keys before any retry. A missing marker is not proof that no partial writes occurred.

## Subsequent deploy and rollback

- A push to the configured release branch runs the CI gates, then verifies the existing marker/version/target before publishing and deploying a new SHA image. The flag is explicitly false on the deployed revision. It does not close public DB access.
- Manual `deploy` uses the same marker preflight and false flag.
- Manual `rollback` requires an existing 40-character commit SHA image under the configured ACR repository. It verifies the same DB marker and always deploys with the seed flag false. It does not reverse Flyway migrations; select an image compatible with the current schema. If compatibility is unknown, stop and prepare a forward fix instead. After MySQL public access is disabled, rollback stays within the new workload-profiles ACA app; the old Express app has no private DB route. Re-enabling public DB access is not an automatic workflow action.
- Manual `private-cutover` is separate from release deploys. It requires protected `demo` approval, verifies current app readiness and private ACA Job/app DB connectivity, then closes MySQL public access and removes broad firewall rules; it finally repeats private-only checks. Review the exact workflow diff and evidence before approving this operation.
- Do not use bootstrap to retry a target with an existing marker. An existing matching marker means use normal deploy/rollback. A marker with another target or fixture version requires manual investigation.

## What the workflow does not prove

A successful workflow proves build gates, ACA readiness, private DB marker match, and Vercel deployment command completion. It does not prove interactive sign-in, end-to-end CORS, or every v2/v3 screen. Record those as a separate live smoke check. No cloud run has been performed as part of authoring this workflow.


## IaC preview and current blockers (read-only, 2026-09-28)

The frozen IaC is intended to create these resources in existing East Asia resource group `rg-java-coban-demo`: Log Analytics workspace `log-java-coban-demo-2026`; VNet `vnet-java-coban-demo-2026` (`10.42.0.0/16`) with ACA delegated subnet `10.42.0.0/23` and PE subnet `10.42.2.0/27`; private DNS zone plus VNet link; MySQL PE `pe-mysql-demo-2026`; ACA WLP Consumption environment `acae-java-coban-rs-vnet-2026`; manual verifier Job `aca-job-java-coban-db-verify-2026`; and narrowly scoped ACR/Key Vault role assignments. It does not create the parallel application, GitHub federated identity/role assignment, MySQL verifier account/secret, or Vercel configuration. Existing Express app and MySQL public/firewall settings remain untouched.

Read-only portal inventory confirmed the existing ACR `javacobanrsdemo26`, Key Vault `kv-javacobanrsdemo26`, MySQL Flexible Server `java-coban-rs-demo-2026`, and ACA apps in `rg-java-coban-demo`; no VNet or Log Analytics workspace was present. East Asia is the current resource region. WLP support is documented for East Asia, but subscription quota/SKU availability could not be proven: the quota blade returned an error and local `az` is unavailable. External IPAM overlap is also unverified despite no current subscription VNet overlap. Do not deploy until quota/SKU, external CIDR/IPAM and DBA verifier-principal/secret prerequisites are resolved.

The verifier database account must be provisioned separately with only `USAGE` and `SELECT ON <demo_schema>.*`; use a dedicated Key Vault secret. DBA SQL template (substitute the schema and inject the password interactively through an approved secure client; never save or run the placeholder literally):

```sql
CREATE USER 'plan086_verifier'@'%' IDENTIFIED BY '<securely-injected-password>' REQUIRE SSL;
GRANT SELECT ON `<demo_schema>`.* TO 'plan086_verifier'@'%';
SHOW GRANTS FOR 'plan086_verifier'@'%';
```

Verify the effective grants contain only USAGE and the intended schema-level SELECT. Use the approved Key Vault secure-entry procedure to create a dedicated secret (the workflow default reference name is `mysql-password`); do not place the password in GitHub, shell arguments, logs, notes, or source control. No credential value has been read or stored in this runbook. The final Bicep build compiled locally with no Bicep diagnostics; no ARM deployment validation/what-if, quota check, cloud mutation, DB test, workflow run, or app cutover was performed.

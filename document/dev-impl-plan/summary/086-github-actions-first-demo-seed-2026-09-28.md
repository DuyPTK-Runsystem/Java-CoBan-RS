# Plan 086 — GitHub Actions deploy và seed demo đúng một lần

## Trạng thái và phạm vi

- **Status:** Approved; local implementation and static QA are complete. Final Bicep compile passed with no Bicep diagnostics. No Azure resources have been created. Cloud rollout: **NOT RUN** pending subscription quota/SKU confirmation, external CIDR/IPAM check, verifier DB principal/secret setup, and root review.
- **Application document:** v2 và v3; dùng fixture của Plan 081 trên môi trường demo.
- **Đích:** GitHub Actions → Azure Container Registry → Azure Container Apps (BE), Vercel (FE), Azure Database for MySQL Flexible Server.
- **Mục tiêu:** lần bootstrap seed thành công đầu tiên chạy với `APP_SEED_DEMO_ENABLED=true`; mọi revision phục vụ ứng dụng sau đó và mọi lần deploy thường kỳ chạy với `false`.
- Không tự chạy seed trên production/DB khác, không xóa hoặc reset dữ liệu, không đổi contract nghiệp vụ v2/v3.

## Hiện trạng cần giải quyết

1. Backend Azure đang được ghi trong `deploy-handoff.md` với `APP_SEED_DEMO_ENABLED=false`; tài liệu này là bản bàn giao chưa được xác minh lại với Azure trong lượt lập plan. Seed chưa được người dùng xác nhận đã chạy.
2. `application.properties` hiện dùng `${APP_SEED_DEMO_ENABLED:true}`. Dockerfile đặt `false`, nhưng JVM chạy ngoài image hoặc cấu hình bị bỏ sót vẫn có thể bật seed. Cần mặc định `false` ở application và ghi rõ flag ở mọi đường deploy.
3. Seed là các `ApplicationRunner` chạy sau Flyway. `DemoDataSeeder` tạo dữ liệu nền; placement, notification, scorebook chạy riêng. Phải kiểm tra đủ toàn bộ fixture trước khi coi bootstrap thành công.
4. Repo chưa có `.github/workflows/`. `.gitlab-ci.yml` cũ chỉ chạy theo schedule với `docker run`, không deploy đến Azure hiện tại. FE hiện build/deploy bằng Vercel.
5. Handoff ghi `/actuator/health` trả `DOWN` trong khi `liveness` và `readiness` trả `UP`; cần xác minh hiện trạng và sửa health gate trước khi dùng nó làm điều kiện kết thúc deploy.

## Luồng đề xuất

### A. CI trên pull request và nhánh deploy

- Thêm GitHub Actions CI cho BE và FE: chạy build, test và các quality gate đã cấu hình; báo riêng gate pass, fail hoặc chưa chạy. Không dùng build thành công để suy ra seed/DB đã đúng.
- Build BE image từ `BE/BaiTap-RS`, gắn tag bất biến theo commit SHA; push ACR chỉ sau khi CI đạt điều kiện phát hành. FE giữ Vercel build từ thư mục `FE` và kiểm tra `VITE_API_BASE_URL` theo đích BE.
- Dùng GitHub Environment được bảo vệ cho demo deploy, `concurrency` chung giữa bootstrap và deploy thường kỳ; xác thực Azure qua OIDC, phân quyền tối thiểu. Secret DB/JWT ở Azure secret reference, không in vào log/workflow.

### B. Bootstrap seed lần đầu, kích hoạt thủ công một lần

1. `workflow_dispatch` riêng, chỉ trên nhánh deploy đã chốt và environment demo. Preflight đọc **đúng** Azure app, image, revision mode, replica count, MySQL target, Flyway history và trạng thái seed hiện có. Dừng nếu DB có dữ liệu không khớp fixture hoặc marker hoàn tất; trường hợp dữ liệu một phần phải điều tra trước khi chạy lại.
2. Dùng một image đã qua CI. Giới hạn backend còn một replica, khóa deploy đồng thời, bảo đảm không còn revision `true` đang hoạt động. Đặt `APP_SEED_DEMO_ENABLED=true` tường minh cho revision bootstrap; không dùng giá trị mặc định hoặc biến từ file `.env` local.
3. Chờ Flyway và toàn bộ `ApplicationRunner` kết thúc. Ghi tín hiệu hoàn tất bền vững **trong chính MySQL đích**, sau khi kiểm tra các nhóm fixture v2/v3 trọng yếu và thứ tự runner; marker có phiên bản fixture, DB target và commit/image tương ứng. Không dùng số lần workflow chạy hoặc trạng thái health đơn lẻ làm bằng chứng seed thành công.
4. Chỉ khi kiểm tra DB/marker và API smoke đạt, cập nhật Azure app sang `APP_SEED_DEMO_ENABLED=false`, chờ revision mới sẵn sàng; xác nhận revision `true` đã ngừng, không nhận traffic và không còn replica. Nếu seed lỗi, workflow fail, không ghi marker; cố gắng đưa app về `false` và báo tình trạng revision/DB để xử lý có kiểm soát.
5. Với app hiện đã deploy nhưng chưa seed: đây là **lần bootstrap seed đầu tiên trên DB đó**, dù không phải deployment đầu tiên. Không tự tạo DB mới hoặc coi revision đã tồn tại là bằng chứng đã seed.

### C. Các lần deploy sau

- Workflow deploy thông thường chỉ chạy khi marker seed hoàn tất trên đúng DB; cập nhật image và đặt `APP_SEED_DEMO_ENABLED=false` tường minh. Kiểm tra flag trên revision mới trước và sau cutover. Nếu DB bị thay hoặc marker mất, dừng deploy và đưa về quy trình bootstrap, không tự bật seed trong deploy thường kỳ.
- Dùng Azure Container Apps single revision mode hoặc cơ chế tương đương bảo đảm revision `true` không sống cùng revision mới; kiểm tra thực tế khi chuyển revision. Rollback cũng bắt buộc `false`, không kích hoạt lại seed.
- Sau BE smoke, kiểm tra Vercel URL thực tế, API base, CORS và một luồng đăng nhập/đọc dữ liệu demo v2, v3. FE redeploy khi `VITE_API_BASE_URL` thay đổi.

## Phạm vi đã triển khai local sau approval

- Added `.github/workflows/demo-ci-cd.yml` for CI, manual one-time bootstrap, normal deploy, and rollback; normal deploy and rollback set the seed flag to `false`.
- Changed `application.properties` seed fallback to `false`; added ordered seed completion marker and migration `V28__add_demo_seed_completion_marker.sql`.
- Added read-only DB preflight/postflight scripts and `document/deploy-runbook/github-actions-demo-operations.md`.
- Added Dev Notes for CI/CD and backend marker implementation. The existing `deploy-handoff.md` remains preserved.
- Independent QA static review passed YAML parsing, `bash -n` on workflow run blocks and scripts, and static SQL/schema mapping review. Full backend tests and `pmdTest` baseline remain failing as recorded in the Dev Note; focused implementation tests were not rerun in this ops task.
- No workflow was pushed or run against GitHub. No cloud deployment, DB query, or live smoke test was run. The read-only readiness audit on 2026-09-28 found the current ACA environment is Express with no custom VNet, MySQL public access enabled with an allow-all firewall rule and no private endpoint, and the Vercel Production branch differs from the workflow release branch. Resolve the network and project-branch prerequisites before cloud rollout.
- `.gitlab-ci.yml` was not changed; review its scheduled job after the GitHub workflow has a verified first run.

## Validation và tiêu chí nghiệm thu

- Test backend chứng minh flag thiếu hoặc `false` không chạy demo runners; `true` chạy fixture trên DB sạch; chạy lại không tăng dữ liệu; lỗi giữa chừng không tạo marker thành công.
- Kiểm tra migration Flyway với MySQL demo, bao gồm checksum và schema; chạy focused test seed, test/build/Checkstyle/PMD theo gate thực tế. CI có BE/FE build và test phù hợp; ghi rõ blocker baseline thay vì báo PASS toàn bộ.
- Bootstrap thực tế: xác nhận `2026-2027`, account/teacher/student, enrollment, assignment, scorebook, timetable, placement và notification bằng khóa nghiệp vụ/marker của Plan 081. Không lấy tổng row count trên DB đã có dữ liệu làm tiêu chí duy nhất.
- Sau bootstrap: đúng một active revision `false`, không có replica `true`; deploy lại cùng image và image mới không thay đổi fixture; FE → BE → MySQL smoke đạt. Lưu workflow run, image SHA, revision ID và kết quả truy vấn đã che thông tin nhạy cảm.
- Chẩn đoán và sửa health indicator đang khiến overall `/actuator/health` xuống `DOWN`, hoặc ghi rõ gate tạm thời dựa trên readiness cùng kiểm tra DB/API độc lập cho đến khi sửa xong.

## Điều kiện cần xác nhận khi triển khai

- Nhánh GitHub nào là nguồn phát hành và quyền OIDC/Azure hiện có.
- Môi trường Azure/MySQL hiện tại có dữ liệu seed một phần hay chưa; chỉ triển khai bootstrap sau khi preflight đọc trạng thái thực tế.
- Cách CI truy cập MySQL để đọc bằng chứng seed: execute một ACA Job trong VNet. GitHub-hosted runner chỉ gọi Azure APIs bằng OIDC, không kết nối MySQL. Không mở MySQL công khai chỉ để phục vụ workflow.


## Amendment — parallel private VNet path through an ACA manual Job (2026-09-28)

### Current inventory and decision

- Azure Network Foundation inventory for the active Azure for Students subscription showed no existing VNets. Existing app `aca-java-coban-rs-standard` runs in Express environment `acae-java-coban-rs-standard-2026` (East Asia); Express has no VNet-routed outbound. MySQL remains public-access mode. Preserve current app and MySQL access/firewall while the private path is created and proven.
- User selected VNet for Plan 086. MySQL Private Link supports Flexible Servers created in public-access mode. Do not convert/recreate the database in this amendment.
- The GitHub-hosted workflow uses OIDC to start a manual ACA Job and poll its exact execution; the GitHub runner does not connect to MySQL. The Job and parallel ACA app run within the private WLP environment. No self-hosted runner VM or NAT Gateway is in scope. MySQL public access is disabled only by explicit `private-cutover` workflow_dispatch after protected approval; routine deploy/bootstrap/rollback cannot disable it.

### Proposed topology

| Resource | Proposed name | Configuration |
| --- | --- | --- |
| VNet | `vnet-java-coban-demo-2026` | East Asia, `10.42.0.0/16`; no existing Azure VNet overlap seen. No peering/VPN in scope; external IPAM remains unverified. |
| ACA infra subnet | `snet-aca-infra` | `10.42.0.0/23`, delegated to `Microsoft.App/environments`. |
| Private Endpoint subnet | `snet-private-endpoints` | `10.42.2.0/27`, separate from ACA subnet. |
| ACA environment | `acae-java-coban-rs-vnet-2026` | WLP v2, Consumption profile only, external ingress. Do not enable Dedicated profiles, ACA environment Private Endpoint, or planned maintenance for this design. |
| Parallel ACA app | `aca-java-coban-rs-vnet-2026` | Provision manually from captured current runtime settings. Seed flag false for ordinary deploys. Existing Express app keeps serving until cutover acceptance. |
| MySQL PE | `pe-mysql-demo-2026` | Attach existing `java-coban-rs-demo-2026` to PE subnet; public DB access/firewall remain unchanged during validation. |
| Private DNS | `privatelink.mysql.database.azure.com` | Link zone to VNet; standard MySQL hostname must resolve to approved PE IP inside Job/app. |
| Log Analytics workspace | `log-java-coban-demo-2026` | Created by IaC, PerGB2018, 30-day retention; ACA uses workspace credentials internally. Ingestion/retention are variable cost. |
| ACA verifier Job | `aca-job-java-coban-db-verify-2026` | Manual Consumption Job; 0.25 vCPU/0.5 GiB; one completion; 1,800-second timeout; retry 0; immutable `plan086-db-verify:${GITHUB_SHA}` image. Executes TLS-verified DB query and marker/preflight inside VNet with dedicated USAGE+SELECT-only DB account; `scripts/ci/verify-demo-mysql-readonly.sh` checks its grants. |
| GitHub OIDC custom role | `infra/plan086-github-job-role.json` | Job read/start/execution read at exact Job scope. `jobs/start/action` allows a template override and can use known Job secret references/system identity; assign only to protected release environment principal. |
| Job managed identity | System-assigned | `AcrPull` at ACR and `Key Vault Secrets User` at the exact DB secret resource scope (not the whole vault); password is a Key Vault reference named `mysql-password`. Use a dedicated read-only database account for marker/fixture SELECTs. |

### Retail cost model (not a hard cap)

- Microsoft Retail Prices API East Asia data queried 2026-09-28: Standard Private Endpoint is a global meter at USD 0.01/hour (~USD 7.30 per 730-hour month); processed data is USD 0.01/GB ingress and USD 0.01/GB egress. Azure Private DNS lists USD 0.50/month for the first private zone and USD 0.40/million private queries. Retail estimates may not reflect Azure for Students discounts or tax. Sources: [Azure Retail Prices API](https://prices.azure.com/api/retail/prices?api-version=2023-01-01-preview), [API documentation](https://learn.microsoft.com/en-us/rest/api/cost-management/retail-prices/azure-retail-prices).
- ACA Consumption profile has no fixed management fee unless a Dedicated profile, ACA environment Private Endpoint, or planned maintenance is enabled; do not enable those features. East Asia active rates: USD 0.0864/vCPU-hour and USD 0.0108/GiB-hour. A maximum 30-minute verifier execution at 0.25 vCPU/0.5 GiB is about USD 0.0135/run before subscription free grants. The parallel app is min 1 during bootstrap; its actual resource sizing/runtime/traffic and existing Log Analytics ingestion drive additional variable usage.
- The user removed the prior USD 10/month cap as a rollout gate. This usage model is not a hard spending limit; subscription-specific rates and runtime/traffic/log volume change the bill.

### Pre-create gates and staged rollout

- Before provisioning, verify CIDRs against current Azure VNets and external IPAM; check East Asia WLP/Job SKU availability and subscription quota; confirm ACR/Key Vault identity permissions, private DNS permissions, and QA of the frozen IaC/workflow. Portal inventory found no current VNets; regional quota/SKU availability remains unverified. The portal quota blade returned an error and local `az` is unavailable, so quota was not proven.
- The target subscription currently has no Log Analytics workspace; the final IaC creates `log-java-coban-demo-2026`. A dedicated MySQL verifier user and its password secret have not been verified. DBA must provision a separate principal with only `USAGE` and `SELECT ON <demo_schema>.*`, then place its password in a dedicated Key Vault secret using an approved silent/secure flow. Do not read or copy credential values into GitHub, command arguments, logs, or notes. The app writer credential remains separate.
- After independent QA and root review, create VNet/subnets, DNS, MySQL PE, WLP environment, and verifier Job. Keep MySQL public access/firewall unchanged. Provision the parallel ACA app manually from captured current runtime settings. The old Express app keeps serving traffic.
- Before bootstrap, QA starts the exact Job execution through protected GitHub OIDC. Prove the standard MySQL hostname resolves to the approved PE IP, TLS and a MySQL query succeed, marker/preflight reads the intended DB, and the parallel app readiness succeeds with seed flag false. Confirm app/Job managed identities can pull ACR and resolve the Key Vault secret. GitHub-hosted runners never connect directly to MySQL. Do not seed during network QA.
- Root separately approves traffic cutover after reviewing evidence. Verify FE login/read flows against the parallel app while MySQL public access remains enabled. Keep old app available until acceptance.
- Only after acceptance and separate root approval, dispatch the explicit `private-cutover` operation from the protected release branch and approve `demo`. It requires `cutover_image` to identify an existing immutable 40-character SHA image in ACR; it verifies the sole active ACA revision uses that image and a private Job confirms marker key/version/target ID, plus marker `deployment_ref` as a valid 40-character immutable image tag from the same configured ACR repository (it may be the historical bootstrap image). It then checks app readiness and private Job TLS/network before disabling MySQL public access and removing the allow-all firewall rule; private DNS/TLS/Job/app readiness are checked again. It skips normal image deploy and does not seed. Routine deploy/bootstrap/rollback cannot disable public networking. After closure, rollback uses a known-good schema-compatible SHA in the new WLP app; old Express cannot reach the DB. Reopening MySQL public access is a separate incident decision and never automatic.

### Current status

- Independent QA static review passed workflow YAML/shell/SQL schema checks and final marker/image/cutover/network gates. Official Azure CLI container Bicep compile exited 0 with no Bicep diagnostics. No resource, route, DNS, app, identity, secret, firewall, or traffic change has been made. Cloud setup remains **NOT RUN** pending quota/SKU, external CIDR/IPAM, DBA verifier account/secret, and root review.

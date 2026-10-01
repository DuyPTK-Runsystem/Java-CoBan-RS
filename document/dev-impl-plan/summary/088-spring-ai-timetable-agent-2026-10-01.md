# Developer Plan 088 (CR-V4-001) — Spring AI timetable agent

## 1. Status, mục tiêu và nguồn

- Ngày: 2026-10-01; **DRAFT — chờ phê duyệt trước code**.
- Nền tài liệu: v3; cải tiến được ghi nhận là [CR-V4-001](../../application-doc/v4/change-request/CR-V4-001-spring-ai-timetable-agent.md).
- Authorization hiện tại: soạn kế hoạch/CR; chưa phê duyệt implementation.
- Amendment 2026-10-01: [harness/tools/snapshot/cache](../../application-doc/v4/agent-contract/harness-tools-and-prompt-cache.md), người dùng yêu cầu cập nhật tài liệu; D08–D10 giữ đề xuất/TBD.
- Provider/model: chưa chốt theo lựa chọn người dùng; giữ cấu hình linh hoạt.
- Ngôn ngữ prompt được người dùng xác nhận 2026-10-01: system prompts bằng tiếng Anh, natural-language output cho người dùng bằng tiếng Việt; JSON keys/enums/codes/tool names giữ nguyên contract. Prompt version draft `timetable-agent-2`.
- Output: proposal TKB cấu trúc, preview đã kiểm tra, tool call lưu bản nháp do application thực thi.
- [Prompt và ví dụ](../../application-doc/v4/agent-contract/timetable-agent-prompt.md), [schema proposal](../../application-doc/v4/agent-contract/timetable-proposal.schema.json), [schema tool arguments](../../application-doc/v4/agent-contract/save-timetable-draft.schema.json), [wireframe](../../wireframes/fe/timetable/V4-001-spring-ai-agent/README.md).

## 2. Baseline đã đọc ở checkout hiện tại

| Khu vực | Bằng chứng và ảnh hưởng |
|---|---|
| `BE/BaiTap-RS/build.gradle.kts` | Spring Boot 4.0.7, Java 21; chưa có dependency Spring AI |
| `timetable/controller/TimetableController.java` | v3/v2 aliases; ADMIN/ACADEMIC_OFFICE được sửa/validate/publish; TEACHER xem theo quyền hiện tại |
| `ReqUpdateTimetableEntriesDTO`, `ReqEntryItemDTO` | `expectedVersion`, `upserts`, `deletedEntryIds`; item dùng `entryId`, `assignmentId`, `periodId`, `functionalRoomId`, `validFrom`, `validTo` |
| `TimetablePeriod` | ID slot xác định calendar + ngày trong tuần + buổi + tiết; không để model tự tạo slot |
| `TimetableService.updateEntries` | Transaction, trạng thái DRAFT/VALIDATED và version check; lưu draft không tự kiểm tra mọi conflict |
| `TimetableValidationService.validateRevision` | Có ghi blocking/warning/status/fingerprint; không dùng trực tiếp cho preview không ghi TKB |
| `TimetableValidationService.checkAllConflicts` | Có thể tái sử dụng phần kiểm tra candidate; phải bổ sung kiểm tra tải, completeness và phạm vi nguồn lịch cho preview |
| `TimetableRevision` | Có JPA `@Version`; phải chứng minh entry mutation thực sự tăng revision version trong integration test |
| `ClassSubject` | Không có số tiết yêu cầu; không được tự suy ra nhu cầu môn từ phân công |
| `FE/src/services/timetableApi.ts`, views/components timetable | Typed v3 client, grid/editor/conflict panel có thể tái sử dụng qua mapping đúng DTO |

Các reference v3: [ApplicationContext](../../application-doc/v3/ApplicationContext.md), [baseline](../../application-doc/v3/RequirementBaseline.md), [module](../../application-doc/v3/modules/02-TimetableAndTeachingLoad.md), [data boundary](../../application-doc/v3/data-model/README.md), [FE boundary](../../application-doc/v3/frontend-api/README.md), [master plan](MASTER_PLAN_V3-2026-09-09.md). FE override có routing v2 cũ; chỉ dẫn người dùng v3 + CR v4 là nguồn ưu tiên cho task này.

## 3. Scope

In-scope: agent trên draft có sẵn; snapshot dữ liệu scope; bảng nhu cầu số tiết; constraints/soft preferences; proposal JSON; validation không ghi TKB; bounded repair; preview/diff; approval gắn hash; native tool call; executor lưu draft, audit/idempotency; provider adapter/feature flag; contract tests và FE states.

Out-of-scope: tự publish, tạo mới TKB/auto-revise trong agent, sửa policy/calendar/assignment/busy approvals, refactor rộng v3, RAG/vector DB/MCP, solver mới, cloud deploy hoặc chọn model thay người dùng. MVP không hỗ trợ stream partial JSON vào executor.

## 4. Kiến trúc và orchestration

Baseline là **hybrid** lập phương án → kiểm tra–sửa → duyệt → thực thi. Phần 4.1–4.3 là baseline snapshot-only; D08 đề xuất tools bổ sung ở phần 4.4, chưa coi là chọn ReAct đầy đủ.

```text
Vue agent panel
  → TimetableAgentController (auth/capability/validated request)
  → TimetableAgentOrchestrator
      → SnapshotService: application đọc dữ liệu được phép
      → ModelGateway / Spring AI ChatClient: đề xuất JSON
      → ProposalValidator: schema + deterministic business checks
      → ProposalStore: lưu proposal/metadata, chưa sửa TKB
  → Preview + diff + warnings + xác nhận người dùng
  → ApprovalService: actor/proposalVersion/hash/snapshot/revision binding
  → ModelGateway: nhận approved proposal reference; trả native tool call
  → ActionDispatcher: allowlist/schema/approval/scope checks
  → CommandExecutor: transaction + revalidation + idempotency
      → TimetableService.updateEntries (đúng domain contract)
      → audit + action receipt
  → Response từ application; model không quyết định trạng thái lưu
```

### 4.1 Snapshot và validation

Application dựng snapshot immutable: revision/version/semester/date range; assignment active; slot calendar 2 × 4; room eligibility; lịch bận approved; locked entries và entries ngoài scope; nhu cầu số tiết đã xác nhận; policy ID/version/giới hạn đã tính; fingerprint các dependency liên quan.

Chỉ gửi minimal identifiers và label cần thiết, không gửi credentials, hồ sơ học sinh, lý do lịch bận hoặc thuộc tính riêng tư dẫn tới miễn giảm. Policy value lấy từ evaluator, không đưa 19/4/3 thành luật cố định trong prompt.

Candidate phải được ghép với phần lịch giữ nguyên trước khi check: conflict lớp/giáo viên/phòng, assignment/date/calendar/closed dates/approved busy slots, exact weekly demand theo khoảng hiệu lực, locked entries, tải giáo viên và policy. Kiểm tra nguồn lịch hiệu lực liên quan trong cùng scope, không chỉ so các hàng model gửi với nhau. Không áp dụng warning như hard constraint nếu domain hiện tại không chặn.

Tách hoặc bổ sung evaluator không ghi dữ liệu; chia sẻ rule với validator v3, không copy logic thành engine khác. `validateRevision()` hiện có side effect nên không dùng để 'dry run'. Khi snapshot thiếu policy/slot/demand: trả `NEEDS_INPUT` hoặc lỗi domain trước model; không tạo dữ liệu giả. Schema-valid JSON chưa có nghĩa là business-valid.

### 4.2 Bounded agent loop

Proposal call chỉ có schema output, không cấp tool mutation. Model trả `PROPOSED`, `NEEDS_INPUT`, `NO_SOLUTION_FOUND`. Backend quyết định proposal `READY_FOR_REVIEW` khi schema/rule checks pass. Model status không cấp quyền lưu.

Lỗi sửa được trả theo `{code,path,message,allowedValues}` đã scope/redact. Đề xuất tối đa 2 lượt sửa sau lần đầu, cấu hình theo D05; schema retry và business retry cùng tính vào ngân sách tổng. Dừng khi timeout, cancellation, hết budget, refusal, không tiến triển hoặc output bị truncate. Đếm mọi model call thực tế, kể cả framework retry; không để retry lồng nhau vượt budget.

Không giữ transaction DB trong khi gọi provider. Proposal có `expiresAt`; refresh snapshot và chạy lại khi stale, không auto-rebase proposal đã duyệt. Không chứng minh tối ưu/vô nghiệm bằng kết luận LLM.

### 4.3 Tool call và lưu

Hai call model riêng: structured proposal và native function/tool call sau approval. Không buộc provider vừa dùng response JSON Schema vừa trả tool calls trong cùng response. Tool schema có duy nhất `saveTimetableDraft`; tắt tự thực thi tool. Nếu model không gửi đúng tool call, trả action error và không lưu.

Arguments: `proposalId`, `proposalVersion`, `targetRevisionId`, `expectedVersion`. Parameters tham chiếu immutable proposal, không gửi lại toàn bộ entries do model có thể thay đổi sau approval. Server đối chiếu toàn bộ arguments với approved proposal. Không nhận role, actor, approval token, idempotency key hay SQL từ model.

Trước dispatch: đúng một tool call, tên/arguments/schema đúng, tool call ID hợp lệ trong lượt hiện tại, proposal chưa hết hạn, actor có quyền và approval còn hiệu lực. Không thực thi batch tool calls không rõ thứ tự; không dùng generic reflection theo tên model gửi.

Executor nhận target từ proposal server, map desired scoped schedule thành `ReqUpdateTimetableEntriesDTO`. Server tự tính `upserts/deletedEntryIds`; tất cả entry IDs phải thuộc target revision và scope, locked/out-of-scope entries giữ nguyên. Không gọi `deleteAllById` với IDs do model/FE gửi chưa kiểm scope.

Transaction lưu: reserve/load idempotent action theo unique `(actorId, idempotencyKey)` + request hash; lock target revision và các nguồn ràng buộc theo quy ước đồng bộ; kiểm tra expectedVersion/fingerprint; reload candidate và rule checks; save entries; đảm bảo revision version tăng; flush; audit và receipt; commit. Thất bại rollback mọi thay đổi của hành động. Cùng key/cùng hash trả receipt cũ; cùng key/khác hash trả 409.

Lock riêng proposal/revision không đủ cho assignment/calendar/busy/policy thay đổi đồng thời. Contract checkpoint phải chốt cơ chế phối hợp writers hiện có (dependency versions + locks theo thứ tự hoặc isolation phù hợp) và test TOCTOU. Nếu không bảo đảm, slice lưu chưa được promote; không tuyên bố fingerprint trước transaction tự giải quyết concurrency.

Application trả receipt authoritative cho FE và `ToolResponseMessage` đúng tool call ID nếu cần lượt giải thích tiếp. Kết quả model về sau chỉ là text; mất kết nối sau commit phải phục hồi bằng receipt/idempotency, không phụ thuộc model để biết đã lưu. Khi retry execute cùng key, kiểm tra action record trước mọi model call: trả receipt đã commit hoặc trạng thái đang chạy; không yêu cầu model phát lại tool call cho hành động đã lưu.

### 4.4 Amendment harness, read tools và cache (D08–D10)

Đọc [amendment](../../application-doc/v4/agent-contract/harness-tools-and-prompt-cache.md) cùng plan. Nếu chọn D08, thêm read/validate tool phase trước final structured proposal; application sở hữu mọi tool executor. Tools đề xuất: getSchedulingContext, getTeachingAssignments, getTeacherAvailability, getExistingTimetable, validateTimetableProposal. Reads cùng snapshotId, có schema/scope/pagination/size/call limits; tổng budgets gồm read iterations và framework retries.

D09 chốt immutable snapshot/versioned source trước tool/resume; metadata/fingerprint không đủ tái dựng. DB proposal/approval/action vẫn logical draft, không thêm bảng snapshot/cache mặc định.

D10 stable serialized prefix theo phase; usage normalizer và metrics đo cache/latency/net cost trước timer. Missing usage ghi UNKNOWN; cache TTL không thay snapshot/approval expiry; không keep-warm background calls. 300s cần xác minh theo provider/model.

File scope có điều kiện: timetableagent/ai/TimetableReadToolRegistry.java, service/TimetableReadToolDispatcher.java, DTO arguments/results và snapshot retrieval; instrumentation/usage normalizer trong ModelGateway. Schema tools cụ thể chốt sau D08/provider; chưa đổi schema proposal/save hoặc source production.

Tests bổ sung: read scope/pagination/truncation, immutable data khi nguồn đổi, expiry/restart, zero timetable writes, budget/no-progress, final validator độc lập; cache prefix stability/usage normalization/UNKNOWN/miss correctness, benchmark provider riêng. FE flow preview/approve/save giữ nguyên; tool results không phải receipt lưu.

## 5. Spring AI integration đề xuất

- Target dòng Spring AI **2.0.x stable**, đề xuất pin 2.0.1 qua BOM tại implementation checkpoint; verify artifact resolution/runtime trước khi coi là tương thích thực tế.
- Spring Boot hiện là 4.0.7; tài liệu chính thức xác nhận Spring AI 2.0.x hỗ trợ Boot 4.0.x/4.1.x. Không cần đề xuất nâng Boot để áp dụng feature này.
- `TimetableModelGateway` giữ adapter riêng cho proposal và tool calls, cấu hình provider/model, timeout/token/retry và flag `app.ai.timetable.enabled` (đề xuất default false).
- Proposal: `ChatClient.call().entity(...)` + provider-native structured output khi hỗ trợ + schema validation. Fallback prompt-based converter chỉ dùng sau capability test; parse/validation fail thì không lưu.
- Action: Spring AI 2.x opt-out qua `AdvisorParams.toolCallingAdvisorAutoRegister(false)`; đọc `ChatResponse` tool calls và dispatch sau guards. Không dùng API 1.x `internalToolExecutionEnabled(false)` trong implementation 2.x.
- Không đăng ký save tool như default tool của mọi ChatClient; chỉ cấp cho lượt action được server cho phép. Không dùng `ToolCallingManager.executeToolCalls` trước custom guards.
- Provider adapter phải chứng minh native tool calling và structured output, nullable IDs/date/schema surface; không giả định mọi model của provider có cùng capability.

Nguồn truy cập 2026-10-01: [compatibility/BOM](https://docs.spring.io/spring-ai/reference/getting-started.html), [tool calling](https://docs.spring.io/spring-ai/reference/api/tools.html), [native output](https://docs.spring.io/spring-ai/reference/api/structured-output/native.html), [converters](https://docs.spring.io/spring-ai/reference/api/structured-output/converters.html). Đây là evidence tài liệu; dependency/model runtime NOT RUN.

## 6. API và persistence đề xuất — chưa approved

Namespace mới `/api/v4/timetable-agent`; không thêm alias vào v2/v3.

| Endpoint | Contract chính |
|---|---|
| `POST /proposals` | `{targetRevisionId,expectedVersion,classIds,validFrom,validTo,demands:[{assignmentId,periodsPerWeek}],lockedEntryIds,preferences,userRequest}` → proposal + backend validation + capabilities |
| `GET /proposals/{id}` | Trả proposalVersion/hash/expiry/snapshot summary, entries, diff, issues, status; ownership/scope check |
| `POST /proposals/{id}/approve` | `{proposalVersion,proposalHash}`; server ghi approval exact payload, không nhận quyền từ model |
| `POST /proposals/{id}/execute` | `{proposalVersion}` + `Idempotency-Key`; server gọi action model và executor, trả authoritative receipt |
| `GET /actions/{id}` | Receipt theo quyền để phục hồi sau mất kết nối |
| `GET /actions/by-key?key=...` | Lookup theo actor từ session + key client đã giữ trước execute; dùng khi mất response và chưa nhận actionId |

HTTP theo envelope hiện có: 400 malformed/schema, 401 unauthenticated, 403 denied, 404 missing/not visible theo policy hiện có, 409 stale/idempotency mismatch, 422 domain violation, 502 invalid/provider error, 503 disabled/unavailable, 504 timeout. Response có machine `code` và correlation ID; không rò provider secret/raw internal errors.

Model proposal schema chỉ mô tả nội dung proposal. HTTP response enrich thêm fields do server cấp; model không tự tạo `proposalId`, hash, approval, capabilities, revision version hoặc trạng thái SAVED.

Logical storage đề xuất: `TimetableAgentProposal` (scope/snapshot fingerprints, immutable payload/hash/version, state/expiry, actor, model/prompt/schema versions); `TimetableAgentApproval` (actor/hash/version/time); `TimetableAgentAction` (key/hash/status/receipt/audit). Receipt tham chiếu revision/new version, count và thời điểm server. Không lưu chain-of-thought; raw transcript không mặc định persist.

Migration mới xác định số V kế tiếp khi implementation, không đoán số từ danh sách cũ. Foreign keys, uniqueness, retention và JSON/text columns chốt ở contract slice trước code. Proposal logs không thay audit domain TKB.

## 7. Phạm vi file dự kiến

Đường dẫn Java dưới `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/`:

| File/khu vực | Tạo/sửa và trách nhiệm |
|---|---|
| `timetableagent/controller/TimetableAgentController.java` | Mới: proposals/approve/execute/receipt endpoints, validation/auth |
| `timetableagent/service/TimetableAgentOrchestrator.java` | Mới: snapshot → model → validate → store, budgets |
| `timetableagent/service/TimetableSnapshotService.java` | Mới: scope/minimal context/dependency fingerprint |
| `timetableagent/service/TimetableProposalValidator.java` | Mới: schema, references, complete candidate and demand checks, không ghi TKB |
| `timetableagent/service/TimetableAgentApprovalService.java` | Mới: immutable payload binding và expiry |
| `timetableagent/service/TimetableActionDispatcher.java` | Mới: native tool call guards/allowlist |
| `timetableagent/service/TimetableAgentCommandExecutor.java` | Mới: atomic/idempotent map và lưu domain |
| `timetableagent/ai/TimetableModelGateway.java`, `SpringAiTimetableModelGateway.java` | Mới: provider abstraction, proposal/action calls |
| `timetableagent/config/TimetableAgentConfiguration.java` | Mới: feature flag, provider options and budgets |
| `timetableagent/domain/DTOs/{requests,response}/`, `domain/entity/`, `repository/` | Mới: contracts/proposal/approval/action persistence |
| `timetable/service/TimetableValidationService.java` và evaluator liên quan | Sửa giới hạn: chia sẻ pure candidate validation, giữ semantics v3 |
| `timetable/service/TimetableService.java`, repositories và dependency writers cần thiết | Sửa khi chứng minh cần: scope-safe apply/version bump và concurrency guard; không refactor ngoài slice |
| `BE/BaiTap-RS/build.gradle.kts`, `src/main/resources/application.properties` | Sửa sau approval: BOM/provider starter/config không có secret |
| `BE/BaiTap-RS/src/main/resources/ai/timetable/` | Mới: prompt/schema versioned và template escaping |
| `BE/BaiTap-RS/src/main/resources/db/migration/` | Mới: proposal/approval/action tables, số migration chốt lúc triển khai |
| `FE/src/services/timetableAgentApi.ts`, `FE/src/types/timetableAgent.ts` | Mới: typed v4 contracts/error mapping |
| `FE/src/components/timetable/TimetableAgentPanel.vue`, `TimetableAgentReview.vue` | Mới: constraints/status/preview/diff/confirm/recovery |
| `FE/src/views/timetable/TimetableWorkspaceView.vue` | Sửa: entry point agent theo capability backend |
| Stories và test cạnh component/service; BE tests module `timetableagent` | Mới: fixtures/states/unit/integration |

Chốt tên/file tách evaluator tại contract checkpoint. Đọc skill/controller/service/entity/Lombok/FE tương ứng trước implementation; plan này chưa triển khai source.

## 8. FE và wireframe

Review [HTML wireframe](../../wireframes/fe/timetable/V4-001-spring-ai-agent/index.html) và [state checklist](../../wireframes/fe/timetable/V4-001-spring-ai-agent/README.md). HTML static dùng dữ liệu minh hoạ, không gọi backend/model.

Giữ cùng grid 2 buổi × 4 tiết; UI tiếng Việt: nhập nhu cầu/ưu tiên, preview với diff và warning, duyệt riêng rồi lưu, kết quả draft/reload. Không đưa tên tool/schema/token vào flow người dùng. Không render HTML từ lời giải thích model. Mọi chỉnh input hủy trạng thái duyệt; lưu disabled nếu blocking/stale/expired/không quyền. UI success dựa receipt; mất mạng hiển thị kiểm tra trạng thái.

Stories: idle, generating, needs-input, invalid-schema, conflicts, no-solution-found, ready/warnings, approved, executing, stale, expired, denied, provider-timeout, saved và committed-response-lost. FE dùng approved fixtures trước khi backend xong; production promotion sau review wireframe/Storybook và contract approval.

## 9. Unit/integration test plan

| Class/method | Fixtures/mock | Assertion và trường hợp |
|---|---|---|
| SnapshotService.build | Fake catalog/clock, calendar/assignment/policy readers | Chỉ đúng scope; không leak reasons/private attributes; missing policy/demand/slot; empty/out-of-range/duplicate inputs; locked set |
| ModelGateway.propose | Fake ChatModel: valid JSON, refusal, unknown field, malformed, truncated, missing required, wrong date/enum/null | Parse strict và budget; mọi failure không gọi executor; provider-native unsupported được xử lý rõ |
| ProposalValidator.validate | Candidate + fixed entries, fake domain evaluator | Duplicate slots, external conflict, approved vs pending busy, invalid room/assignment/date, demand thiếu/thừa, policy warning/blocking; zero timetable repository writes |
| Orchestrator.propose/repair | Fake gateway, fake clock/store | Success stores proposal only; max retry/timeout/cancel/no-progress; tổng model calls đúng budget; chưa có approval không có save tool |
| ApprovalService.approve | Proposal v1/v2 hashes, actors và clock | Exact binding; changed/expired/stale/unauthorized/not-found rejection; approval không ghi TKB |
| ActionDispatcher.dispatch | Native tool calls giả | Unknown tool, SQL/function injection, multiple tool calls, extra arg, target/version sai, forged actor/approval; zero mutation khi fail |
| CommandExecutor.execute | Repository test doubles cho orchestration; real DB cho atomicity | Đúng entries + outside-scope/locked preserved; foreign entry ID không sửa/xoá; no approval/422/409 => zero writes; receipt server |
| Controller/API | Auth/capability fixtures, fake gateway | 400/401/403/404/409/422/502/503/504 và envelope; teacher không dùng agent |
| Regression v3 | Existing TimetableService/Validation/Publish tests | Manual draft vẫn cho sửa theo semantics cũ, publish vẫn chặn conflict; validate lifecycle không đổi |

Integration dùng DB cô lập/resettable: rollback sau một upsert lỗi; revision version thực sự tăng khi chỉ đổi entries; hai save cùng expectedVersion chỉ một thành công; same key/same hash one write; same key/different hash 409; response mất sau commit recover receipt; policy/calendar/assignment/busy thay đổi đồng thời không vượt revalidation. Không chứng minh MySQL locks bằng mock hoặc H2 đơn thuần.

FE tests: map DTO đúng IDs/dates, user input không tự cấp quyền, preview diff/locked entries, approval invalidation, warnings, double click, stale/recovery và UI receipt. Browser live: tạo proposal bằng provider đã chốt, kiểm tra không đổi TKB, duyệt/lưu/reload, revision persistence và manual publish regression.

Lệnh BE tại `BE/BaiTap-RS`, chạy tuần tự: `./gradlew.bat test --tests '*timetableagent*'`, suites regression timetable, `./gradlew.bat test`, `./gradlew.bat jacocoTestReport`, `./gradlew.bat checkstyleMain checkstyleTest`, `./gradlew.bat pmdMain pmdTest`, `./gradlew.bat build`. Đọc report JaCoCo module thay đổi và nhánh rejection/retry/atomicity; không tự đặt threshold mới.

FE tại `FE`: `npm run lint`, `npm run test`, `npm run test:coverage`, `npm run build`, `npm run build-storybook`. Browser/provider/MySQL gates ghi riêng; quality suite không thay live evidence.

## 10. Delivery, rủi ro và approval gates

1. Duyệt CR/plan/wireframe và chốt D02–D04; D05/D07 chốt trước pilot/production tương ứng.
2. Contract slice: DTO/schema/error/capability, fixture, pure validation boundary, concurrency protocol và dependency/provider capability spike.
3. BE/FE cùng slice proposal: snapshot/strict JSON/validator và panel/Storybook qua fixtures.
4. BE/FE cùng slice approval/action: binding/tool dispatcher/atomic save và review/recovery UI.
5. Integration thật: auth/conflict/race/idempotency/persistence; Storybook review rồi browser/provider pilot.
6. Dev Notes và Validation Result theo gate. Bật feature ở môi trường được duyệt sau khi gates đạt; không deploy trong task lập plan.

Parallel workstreams không tự yêu cầu spawn nhiều agent trong phiên lập kế hoạch này.

Rủi ro: LLM không tìm lịch/chi phí context lớn → limit và benchmark theo scale D05; JSON/tool capability khác nhau → adapter test, fail closed; hallucination/injection → schema + IDs + guards; stale/concurrency → approval hash/version và transaction protocol; regression evaluator → share rule + v3 suites; sensitive data → computed limits/minimal context; model unavailable → manual v3 vẫn sử dụng được.

Acceptance: đủ FR/BR của CR, artifacts được duyệt, không ghi TKB khi gợi ý/sai schema/tool/quyền/approval, save atomically/exactly approved payload, clear authoritative receipt/reload, quality + browser/live evidence theo scope. Mọi API/dependency/migration ở đây vẫn draft.

## 11. Validation của lần lập kế hoạch

Docs/link/schema-example checks và `git diff --check` được ghi ở [Dev Note](../../dev-note/summary/088-spring-ai-timetable-agent-plan-2026-10-01.md). BE/FE production tests/build, model/provider runtime, MySQL concurrency và visual browser review: **NOT RUN — chưa implementation**.

# Harness, tools, snapshot và prompt cache — amendment draft

- Ngày: 2026-10-01; [CR-V4-001](../change-request/CR-V4-001-spring-ai-timetable-agent.md), [Plan 088](../../../dev-impl-plan/summary/088-spring-ai-timetable-agent-2026-10-01.md).
- Người dùng yêu cầu cập nhật tài liệu sau trao đổi; không phê duyệt code/dependency/migration. Các lựa chọn mới chưa xác nhận giữ trạng thái đề xuất/TBD.

## 1. Phân loại và harness

Ngôn ngữ được người dùng xác nhận 2026-10-01: system prompts bằng tiếng Anh và nội dung tự nhiên hướng tới người dùng bằng tiếng Việt; machine contract giữ nguyên. [Prompt guide](timetable-agent-prompt.md) dùng version draft timetable-agent-2; mọi read-tool prompt tương lai tuân thủ cùng quy tắc. Đổi nội dung/ngôn ngữ prompt làm thay prefix/version dùng khi đo cache.

Baseline là hybrid: lập phương án TKB → application kiểm tra/model sửa → người dùng duyệt → model yêu cầu hành động → application lưu. Chưa phải ReAct đầy đủ vì chưa có model tự gọi tools đọc dữ liệu; chưa Plan-and-Execute đầy đủ vì proposal TKB không phải execution plan nhiều bước do model tự điều phối.

Harness gồm snapshot reader, prompt/schema registry, model gateway, orchestrator/budget, validator, proposal store, approval service, action dispatcher, transaction executor và audit/receipt. Application sở hữu quyền, state transitions và business invariants.

## 2. Nội dung snapshot

| Nhóm | Nội dung |
|---|---|
| Scope | Semester, target draft/revision, lớp được chọn, validFrom/validTo |
| Assignments | ID, lớp/môn/giáo viên và khoảng hiệu lực |
| Demand | Số tiết mỗi tuần/assignment đã xác nhận, không tự suy từ phân công |
| Slots/calendar | periodId, ngày/buổi/tiết/giờ, calendar, ngày nghỉ và tiết bị chặn |
| Rooms | Phòng chức năng khả dụng và quan hệ môn/phòng hợp lệ |
| Availability | Lịch bận approved còn hiệu lực, không gửi lý do |
| Existing schedule | Lịch trong scope và nguồn lịch liên quan để tránh xung đột |
| Preserved entries | Locked entries và phần ngoài scope |
| Load policy | Policy/version và giới hạn do application tính, không gửi thuộc tính cá nhân miễn giảm |
| Preferences | Ưu tiên mềm, tách hard constraints |

Server giữ snapshotId, createdAt/expiresAt, revision version và dependency fingerprint. Model chỉ nhận metadata cần cho contract. Không đưa timestamp đổi mỗi request vào đầu prompt cố định.

Metadata/fingerprint phát hiện thay đổi nhưng không đủ tái dựng dữ liệu cũ. Cách lưu snapshot còn TBD: immutable payload/materialized snapshot hoặc nguồn versioned có thể truy hồi. Nếu chỉ giữ snapshot trong bộ nhớ thì không resume sau restart và phải tạo snapshot mới; không đọc DB hiện tại rồi gắn snapshotId cũ. TTL snapshot, TTL proposal/approval và TTL provider prompt cache là các vòng đời riêng.

DB baseline vẫn đề xuất proposal/approval/action, không tạo bộ bảng TKB riêng cho AI. Chưa chốt snapshot toàn bộ hay metadata/versioned references; không mặc định thêm bảng snapshot/cache. Nhu cầu số tiết lưu trong proposal theo scope draft; cấu hình demand dùng chung là scope riêng nếu được yêu cầu.

## 3. D08 — đề xuất snapshot ban đầu + tool đọc bổ sung

Chưa chốt thay baseline snapshot-only. Model yêu cầu thông tin còn thiếu trong scope; application thực thi tool và trả structured ToolResponse. Không cấp repository/SQL/HTTP tổng quát.

| Tool đề xuất | Parameters khái niệm | Result |
|---|---|---|
| getSchedulingContext | snapshotId | Scope/calendar/slots, constraints và demand đã xác nhận |
| getTeachingAssignments | snapshotId, classIds, cursor, limit | Assignments trong scope; demand chỉ trả nếu đã xác nhận |
| getTeacherAvailability | snapshotId, teacherIds, validFrom, validTo, cursor, limit | Slots khả dụng/bị chặn, không trả lý do lịch bận |
| getExistingTimetable | snapshotId, classIds, validFrom, validTo, cursor, limit | Lịch hiện có/liên quan và flags giữ nguyên |
| validateTimetableProposal | snapshotId, candidate đúng proposal schema | Issues/completeness/load results; không ghi TKB |

Tên/parameters là contract đề xuất, chưa phải JSON Schema/API approved. Validator tool nhận candidate trước khi có proposalId. Harness vẫn tự chạy authoritative validation khi model hoàn tất, không tin model nói đã kiểm tra.

Guards: schema strict, actor từ session, scope/expiry của snapshot, IDs/date range, page/output size và call budget. Result envelope đề xuất `{snapshotId,data,issues,nextCursor,truncated}`; một page/truncated result không phải toàn bộ catalog. Tool reads phải cùng immutable dataset. Error/refusal không ghi TKB; allowlist và thứ tự definitions ổn định theo phase. Nếu chưa có nguồn snapshot/versioned, chưa mở tools cùng snapshot trước khi chốt D09.

## 4. Loop nếu chọn D08

```text
Application tạo snapshot + allowlist read/validate
  → Model yêu cầu native tool call
  → Harness kiểm schema/quyền/snapshot/scope/budget
  → Application đọc/validate, trả structured ToolResponse đúng call ID
  → Model tiếp tục trong ngân sách hoặc kết thúc thu thập
  → Bước final proposal riêng trả JSON theo schema
  → Application validation → preview → người dùng duyệt
  → Action call chỉ có saveTimetableDraft
  → Executor revalidate/transaction → authoritative receipt
```

Read/validate phase và final structured proposal phase tách riêng, không giả định provider hỗ trợ JSON Schema và tool calls đồng thời. Save tool không xuất hiện ở read phase. Hết tool/model/token/time budget, lặp không tiến triển, missing data/cancellation thì dừng. Giới hạn 2 lượt sửa sau lần đầu không cấp vô hạn read-tool iterations: D05 phải chốt ngân sách tổng và giới hạn riêng từng phase.

## 5. Output sai structure

Parse lỗi/prose ngoài JSON/missing field/wrong type/extra field → OUTPUT_SCHEMA_INVALID; sai ID/date/scope/conflict → lỗi nghiệp vụ. Trả errors có path/message và allowedValues phù hợp để sửa trong budget. Không đoán trường thiếu, coercion sai kiểu hoặc tự sửa arguments để lưu. Tool sai tên/params bị reject trước executor. Hết retry/timeout giữ nguyên TKB. Native structured output giảm lỗi định dạng, không thay validator.

## 6. D10 — prompt cache: đo trước khi quyết định timer

Logical layout: system rules + schema/tool definitions + fixed examples → snapshot ổn định trong session → user request/proposal/errors/tool results. Provider adapter quyết định thứ tự wire/cache breakpoints; không hứa thứ tự chung cho mọi provider. Serialize ổn định, không chèn IDs/timestamps đổi mỗi call vào static prefix. Không cấp tool save sớm chỉ để giữ cache.

Đo theo provider/model/promptVersion/schemaVersion/toolsetVersion và phase (read, proposal, repair, action): cached-read input tokens, uncached input tokens, cache-write tokens nếu có, tổng logical input tokens, latency và actual cost. Không ghi raw prompt/private data vào metric labels.

- Token reuse ratio = tổng cached-read input tokens / tổng logical input tokens trong cửa sổ đo.
- Request hit rate = số call có cached-read tokens > 0 / số call có cache usage quan sát được; báo riêng eligible hit rate nếu xác định được eligibility.
- Missing usage = UNKNOWN, không gán 0/100%. Chuẩn hoá semantics theo provider để không double-count cached-read/write trong input tokens đã bao gồm chúng.
- Cold/warm benchmark cùng workload hợp lệ; đo chi phí ròng gồm cache-write premium và latency. Không chọn chỉ vì hit rate cao; threshold/sample size TBD.

300s là thông tin người dùng nêu, không chốt TTL toàn hệ thống. Tài liệu Claude xác nhận cache mặc định 5 phút, refresh khi sử dụng và có lựa chọn 1 giờ; đây là ví dụ TTL phụ thuộc provider/cấu hình, không chọn Claude cho dự án. [Nguồn chính thức đọc 2026-10-01](https://platform.claude.com/docs/en/build-with-claude/prompt-caching).

Nếu provider/model được chọn có TTL 300s, timestamp request/cache usage chỉ ước tính expiry, không chứng minh hit. Không chạy timer gọi model định kỳ để giữ cache. Review lâu/cache miss vẫn tiếp tục bình thường; cache không quyết định quyền, snapshot validity hoặc save correctness. Metrics qua gateway/observability sau checkpoint, không tạo bảng cache mới.

## 7. Decisions và test bổ sung

| Mục | Trạng thái |
|---|---|
| Hybrid và harness/application ownership | Làm rõ baseline |
| D08 Snapshot + read/validate tools | Đề xuất cần chọn; baseline snapshot-only vẫn được ghi rõ |
| D09 Snapshot persistence/reconstruction | TBD trước read-tool/resume implementation; fingerprint không đủ |
| D10 Prompt cache policy | Đề xuất stable prefix + metrics trước timer; provider TTL/breakpoints/threshold TBD |

Nếu chọn D08, tests thêm: cross-scope/date/forged snapshot rejection; pagination/truncation; dataset consistency khi DB nguồn đổi; expiry/restart; budgets/no-progress; zero TKB writes cho read/validate; final validator independent of model claims. Cache tests: stable serialization, đổi toolset không bỏ guards để giữ hit, usage normalization/no double-count, UNKNOWN metrics, miss/TTL không đổi semantics. Provider benchmark riêng: NOT RUN. Không mở migration/source code trong task cập nhật tài liệu.

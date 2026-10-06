# CR-V4-001 — Agent gợi ý và lưu thời khoá biểu bằng Spring AI

## Trạng thái và nguồn

- Ngày: 2026-10-01; status: **DRAFT / IMPLEMENTATION APPROVAL PENDING**.
- Loại: CR cải tiến v4, kế thừa v3; không sửa ngầm contract v2/v3.
- Người dùng yêu cầu: gợi ý TKB theo ràng buộc, gửi lời gọi hàm + parameters để lưu; model không thao tác dữ liệu; output có cấu trúc để application xử lý.
- Người dùng xác nhận nền v3 và phân loại v4; provider/model chưa chốt, giữ cấu hình linh hoạt.
- Nền: [Requirement v3](../../v3/RequirementBaseline.md), [TKB và tải giáo viên](../../v3/modules/02-TimetableAndTeachingLoad.md).
- Chi tiết: [Developer Plan](../../../dev-impl-plan/summary/088-spring-ai-timetable-agent-2026-10-01.md), [prompt](../agent-contract/timetable-agent-prompt.md), [wireframe](../../../wireframes/fe/timetable/V4-001-spring-ai-agent/README.md).

## Vấn đề và kết quả

Amendment 2026-10-01: [harness/tools/snapshot/prompt cache](../agent-contract/harness-tools-and-prompt-cache.md). Người dùng yêu cầu cập nhật tài liệu; các phương án mới vẫn draft, không suy ra approval triển khai.

TKB hiện có editor, validation, revision và publish. CR bổ sung trợ lý đề xuất phương án dựa trên dữ liệu/ràng buộc được application cung cấp, preview thay đổi và yêu cầu application lưu bản nháp sau khi người dùng duyệt.

## Requirement v4

| ID            | Requirement                                                                | Acceptance                                                                             |
| ------------- | -------------------------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| FR-V4-TTA-001 | Người có quyền chọn draft, lớp, khoảng áp dụng, nhu cầu số tiết và ưu tiên | Application kiểm tra scope và dữ liệu trước khi gọi model                              |
| FR-V4-TTA-002 | Agent gợi ý lịch theo hard constraints và soft preferences                 | Có proposal JSON, giải thích ngắn; backend kiểm tra từng ràng buộc                     |
| FR-V4-TTA-003 | Output theo schema có version                                              | JSON không hợp lệ/unknown fields/ID giả bị từ chối; không parse prose để ghi DB        |
| FR-V4-TTA-004 | Application trả lỗi cấu trúc cho agent chỉnh phương án                     | Số lượt, timeout và token có giới hạn; hết giới hạn không lưu TKB                      |
| FR-V4-TTA-005 | Preview grid, diff, conflict, thiếu dữ liệu và cảnh báo tải                | Kết quả kiểm tra của backend quyết định nút duyệt; mock không coi là runtime proof     |
| FR-V4-TTA-006 | Model gửi tool call tên hàm + arguments yêu cầu lưu                        | Chỉ `saveTimetableDraft`; application kiểm tra và thực thi                             |
| FR-V4-TTA-007 | Lưu đúng proposal đã duyệt và trả kết quả thực thi                         | Atomic, audit, optimistic locking, idempotency; chỉ kết quả server xác nhận thành công |
| FR-V4-TTA-008 | Provider/model cấu hình linh hoạt                                          | Chọn adapter qua cấu hình, feature flag; test capability trước khi bật                 |

## Business rule và ranh giới

- `BR-V4-TTA-001`: model không có DB connection, repository, SQL, HTTP tool tổng quát hoặc quyền tự ghi dữ liệu. Application sở hữu snapshot reader, validator, command executor và transaction.
- `BR-V4-TTA-002`: hard constraints giữ nguyên nền v3: lớp/giáo viên/phòng chức năng không trùng, calendar/assignment/date hợp lệ, lịch bận đã duyệt; số tiết yêu cầu phải được người dùng xác nhận. Không hard-code chính sách 19/4/3 vào prompt.
- `BR-V4-TTA-003`: policy active và mức BLOCKING/WARNING lấy từ backend, không để model đổi. Không gửi lý do lịch bận hoặc thông tin cá nhân về miễn giảm; chỉ gửi slot bị chặn và giới hạn đã tính.
- `BR-V4-TTA-004`: đề xuất MVP chỉ áp dụng vào draft có sẵn; published/archived phải qua luồng revision v3 trước. Không cấp tool publish, delete timetable hoặc approve busy slots.
- `BR-V4-TTA-005`: xác nhận gắn actor + proposal version/hash + snapshot + target revision. Sửa proposal/ràng buộc hoặc thay dữ liệu liên quan phải kiểm tra lại và duyệt lại.
- `BR-V4-TTA-006`: command của model là yêu cầu chưa tin cậy. Quyền, idempotency key và bằng chứng duyệt lấy từ server, không từ model.
- `BR-V4-TTA-007`: không đổi cách v3 cho phép draft lỗi. Đường agent lưu chỉ proposal qua kiểm tra mới nhất, không có blocking issue và không thiếu nhu cầu số tiết.
- `BR-V4-TTA-008`: dữ liệu snapshot và yêu cầu tự do không được ghi đè system rules/schema/tool allowlist. Output prose/giải thích chỉ hiển thị như text.
- `BR-V4-TTA-009`: `NO_SOLUTION_FOUND` chỉ nghĩa là chưa tìm được phương án trong giới hạn, không chứng minh vô nghiệm/tối ưu toàn cục.
- `BR-V4-TTA-010`: warning về ưu tiên/tải vẫn hiển thị khi duyệt; lịch đã khoá và tiết ngoài scope phải giữ nguyên. Candidate được kiểm tra cùng phần lịch cố định và các nguồn lịch liên quan đang có hiệu lực.

## Role matrix đề xuất

| Vai trò                | Tạo/gợi ý/xem/duyệt/lưu proposal | Xem lịch cá nhân         | Publish                       |
| ---------------------- | -------------------------------- | ------------------------ | ----------------------------- |
| ADMIN, ACADEMIC_OFFICE | Có, trong scope backend cho phép | Theo capability hiện có  | Luồng v3 hiện có, ngoài agent |
| TEACHER                | Không trong MVP                  | Giữ nguyên v3            | Không                         |
| STUDENT/khác           | Không                            | Giữ nguyên quyền hiện có | Không                         |

## Quyết định draft và còn mở

| Mục                   | Đề xuất / trạng thái                                                                                                       |
| --------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| D01 Provider/model    | Người dùng chọn giữ linh hoạt; tên provider/model và credentials TBD                                                       |
| D02 Mức tự động       | Đề xuất preview → người dùng duyệt → model yêu cầu lưu → application thực thi                                              |
| D03 Phạm vi lưu       | Đề xuất cập nhật draft có sẵn, thay thế tiết trong scope đã chọn; ngoài scope và locked entries được giữ                   |
| D04 Nhu cầu số tiết   | Đề xuất nhập bảng số tiết/assignment theo tuần và xác nhận; contract `ClassSubject` hiện không có định mức số tiết         |
| D05 Quy mô và chi phí | Giới hạn lớp/entries/context, timeout/token và retry cấu hình; giá trị cụ thể TBD trước live pilot                         |
| D06 Tối ưu            | MVP LLM + deterministic validation; solver là CR/amendment riêng nếu benchmark không đáp ứng                               |
| D07 Lưu dữ liệu AI    | Đề xuất lưu metadata, proposal, hành động và hash; thời hạn lưu và chính sách gửi dữ liệu ra provider TBD trước production |

## Bổ sung sau trao đổi — 2026-10-01

- Ngôn ngữ được người dùng xác nhận: system prompts bằng tiếng Anh; explanation, thông báo thiếu dữ liệu và kết quả hướng tới người dùng bằng tiếng Việt. Không dịch JSON keys, enums, machine codes hoặc tool identifiers; không thêm prose ngoài structured contract.

- Làm rõ mô hình hybrid; baseline chưa ReAct đầy đủ hoặc Plan-and-Execute đầy đủ.
- D08 (đề xuất): snapshot ban đầu + tool đọc/validate do application thực thi, cùng snapshotId, giới hạn quyền/scope/schema/pagination/output/calls; save tool chỉ sau approval.
- D09 (TBD): lưu immutable snapshot hoặc nguồn versioned để đọc/resume đúng dữ liệu; metadata/fingerprint không đủ tái dựng. Chưa thay đổi DB/migration.
- D10 (đề xuất): stable prompt prefix và đo token reuse/request hit rate/latency/net cost trước khi quyết định timer. Không gọi model định kỳ để giữ cache; TTL 300s không phải mặc định chung.
- Output sai structure bị reject, sửa trong budget; không đoán/coercion fields để lưu. Chi tiết contract/error/decisions/tests trong amendment.

## Điều kiện nghiệm thu

Có bằng chứng độc lập cho structured contract, zero timetable writes khi gợi ý, schema/rule rejection, quyền, approval binding, stale/concurrent data, rollback, retry idempotency, FE Storybook và browser lưu/reload với backend thật. Không coi model thông báo thành công là bằng chứng lưu.

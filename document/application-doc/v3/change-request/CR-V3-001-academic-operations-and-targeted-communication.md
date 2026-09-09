# CR-V3-001: Academic Operations, Rule-based Placement and Targeted Communication

## 1. Metadata

- Application-document version: `v3`
- Status: `DRAFT - chờ phê duyệt`
- Ngày: `2026-09-09`
- Related master plan: `document/dev-impl-plan/summary/MASTER_PLAN_V3-2026-09-09.md`
- Starting Developer Plan: `Plan 073`
- Affected domains: Enrollment, Teaching, Timetable, Notification, Query, Scoring,
  Lesson Log, Audit

## 2. Lý do thay đổi

v2 đã có nền tảng xếp lớp, phân công, lịch học, điểm và thông báo theo một số flow,
nhưng chưa có một contract thống nhất cho vận hành theo tiêu chí, lập thời khóa biểu,
audience notification, truy vấn nâng cao, import một cột điểm và sổ đầu bài.

## 3. Quyết định phạm vi

CR này thêm sáu capability:

1. Xếp lớp theo tiêu chí cấu hình: điểm số, giới tính và các tiêu chí được phê duyệt.
2. Thời khóa biểu lớp/giáo viên với conflict detection và teacher-load policy.
3. Thông báo `INDIVIDUAL`, `CLASS`, `SCHOOL` có audience và trạng thái đọc.
4. Search/filter cho danh sách và bảng điểm, xử lý ở backend khi có pagination.
5. Import file vào đúng một cột điểm, với preview/validation/audit.
6. Sổ đầu bài ghi nhận đánh giá của từng tiết học.

## 4. Quy tắc và ranh giới

- Xếp lớp là workflow có preview và confirm; không tự động thay enrollment đã phát sinh
  điểm danh/điểm nếu chưa có policy chuyển lớp được phê duyệt.
- Thuật toán xếp lớp phải deterministic với cùng input + rule version và phải giải thích
  được kết quả. Nếu nhiều nghiệm tương đương, dùng tie-breaker đã cấu hình.
- Timetable publish bị chặn bởi conflict; draft có thể chứa item cần xử lý nhưng phải
  hiển thị rõ. Kiểm tra tải giáo viên dựa trên policy version, không hard-code.
- Notification không mở rộng quyền xem; audience phải được snapshot hoặc resolution
  strategy chốt trong Plan 076 và phải idempotent.
- Search/filter không thay đổi semantics điểm. `0` là điểm hợp lệ; missing là trạng thái
  trình bày theo contract điểm hiện hành.
- Import không tạo assessment column mới và không cho phép file quyết định cột đích.
- Lesson log không thay thế attendance, scorebook hoặc transcript.

## 5. Contract cần thiết kế

Plan 073 phải chốt vocabulary, role matrix, error model, audit/idempotency và API shape.
Các plan sau mới được thêm endpoint/schema cụ thể sau khi contract checkpoint được duyệt.
Không tự tạo endpoint chỉ từ tên capability.

## 6. Phụ thuộc và TBD

- `TBD-001` (`PARTIALLY RESOLVED`): nội dung nghiệp vụ là 19 tiết/tuần chuẩn; GVCN giảm
  4 tiết; GV nữ nuôi con dưới 12 tháng giảm thêm 3 tiết; các mức giảm được cộng dồn.
  Còn thiếu văn bản/source, effective date và policy version để policy được coi là active.
- `TBD-002`: bộ tiêu chí, trọng số, tie-breaker và policy sĩ số cho xếp lớp.
- `TBD-003`: quyền publish timetable, gửi notification, import score và sửa lesson log.
- `TBD-004`: retention/privacy và kênh gửi thông báo (in-app/email/push nếu có).
- `TBD-005` (`PARTIALLY RESOLVED`): import dùng `.xlsx`, preview bắt buộc trước khi xác
  nhận, mỗi lần upload chỉ xử lý một `assessmentColumnId`. Có thể update ô đã có điểm sau
  khi user review giá trị cũ/mới và thông tin học sinh. Quy ước `0–10` là điểm, `11` là
  `ABSENT`, `12` là `EXEMPTED`; chi tiết template, identity mapping, limits, blank-cell,
  `CANCELLED` và partial-commit policy để Plan 078 chốt.

Các TBD chưa được chốt đầy đủ vẫn không được suy đoán. Khi có thông tin bổ sung, phải tạo
amendment hoặc cập nhật phần open decisions trước khi code phần bị ảnh hưởng.

## 7. Acceptance criteria cấp CR

- Mỗi capability có requirement ID, API/data ownership, role matrix và FE review state.
- Plan 073 cung cấp contract checkpoint dùng chung cho BE và FE.
- Từ Plan 073, mỗi plan có workstream BE và FE song song, cùng test fixture/contract,
  không dùng mô hình “BE hoàn tất toàn bộ rồi FE mới bắt đầu”.
- Không có conflict timetable được publish; không có import ghi nhầm cột; không có
  notification vượt audience; mọi thay đổi placement/lesson log có audit.
- Các quy định chưa được cung cấp được giữ `TBD`, không suy đoán thành con số nghiệp vụ.

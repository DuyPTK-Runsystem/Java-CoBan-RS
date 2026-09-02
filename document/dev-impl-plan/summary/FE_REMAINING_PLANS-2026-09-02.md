# FE Roadmap — Các plan còn lại

Ngày cập nhật: `2026-09-02`  
Application version: `v2`

## Mục đích

Note này ghi lại các hạng mục FE còn lại theo roadmap hiện tại. Trạng thái
được đối chiếu với FE Dev Plan Summary, FE Dev Note Summary và các plan/note
đã có trong repository.

## Danh sách hạng mục

| Ưu tiên | Hạng mục | Phạm vi chính | Trạng thái hiện tại | Plan liên quan |
|---:|---|---|---|---|
| 1 | Attendance History & Summary UI | Lịch sử điểm danh học sinh; thống kê theo lớp/thời gian | **Đã hoàn thành trong Plan 056**; còn manual walkthrough với live backend và office capability khi contract sẵn sàng | [Plan 056](../fe/attendance/056-attendance-workspace-ui-2026-08-28.md), [Dev Note 056](../../dev-note/fe/attendance/056-attendance-workspace-ui-2026-08-28.md) |
| 2 | Scorebook Configuration UI | Tạo sổ điểm, assessment column, hệ số/trọng số, cấu hình môn kỹ năng | **Còn làm FE**; backend Scorebook Foundation đã có. Plan 057 đã có phần assessment-column nhưng chưa bao phủ toàn bộ configuration workflow | [BE Plan 036](../be/scorebook/036-scorebook-foundation-2026-08-24.md), [FE Plan 057](../fe/scorebook/057-scorebook-assessment-column-ui-2026-09-01.md) |
| 3 | Student Score Entry UI | Nhập/sửa điểm đơn lẻ, nhập hàng loạt, trạng thái điểm | **Đang triển khai một phần trong Plan 057**; score grid, single entry và bulk entry đã có, cần chốt completion theo backend quality gate | [BE Plan 037](../be/scorebook/037-student-score-entry-2026-08-24.md), [FE Plan 057](../fe/scorebook/057-scorebook-assessment-column-ui-2026-09-01.md) |
| 4 | Score Change Request UI | Giáo viên tạo request; giáo vụ duyệt/từ chối; xem lịch sử | **Còn làm FE**; backend Plan 038 đã triển khai, còn e2e persistence test pending theo Dev Note | [BE Plan 038](../be/scorebook/038-score-change-request-2026-08-25.md) |
| 5 | Semester Lock & Completeness UI | Báo cáo thiếu dữ liệu, khóa/mở học kỳ, trạng thái dữ liệu | **Còn làm FE**; backend Plan 039/040 đã có. Cần UI cho completeness, lock/reopen, quyền và trạng thái lỗi | [BE Plan 039](../be/academic/039-semester-lock-2026-08-25.md), [BE Plan 040](../be/academic/040-semester-completeness-notification-cr-sem-001-2026-08-25.md) |
| 6 | Transcript Viewer UI | Bảng điểm theo học kỳ/năm, điểm môn, `IN_PROGRESS`/`FINISH`, thời điểm tính gần nhất | **Còn làm FE**; backend transcript schema/aggregation/query đã có Dev Note, cần xác nhận lại contract query trước khi lập plan FE | [BE Plan 041](../be/scorebook/041-transcript-result-schema-foundation-2026-08-25.md), [BE Plan 044](../be/scorebook/044-transcript-aggregation-2026-08-26.md), [BE Plan 046](../be/scorebook/046-transcript-query-api-2026-08-26.md) |
| 7 | Retake Result UI | Danh sách thi lại, nhập điểm thi lại, hiển thị điểm trước/sau thi lại | **Còn làm FE**; backend Plan 045 đã có foundation, API và calculation integration | [BE Plan 045](../be/scorebook/045-retake-foundation-and-calculation-integration-2026-08-26.md) |
| 8 | Calculation Task & Audit UI | Theo dõi task lỗi, retry, audit log, trạng thái tính toán | **Còn làm FE**; backend operational APIs đã có trong Plan 047, cần xác định role/capability và màn hình vận hành | [BE Plan 047](../be/scorebook/047-be-operational-closure-2026-08-26.md) |
| 9 | FE E2E & Release Hardening | E2E flow, responsive, accessibility, permission regression, build/deploy validation | **Chưa có FE plan chi tiết**; thực hiện sau khi các flow chính hoàn tất, gồm cả browser walkthrough với live backend | Chưa gán plan |

## Trạng thái cần lưu ý

- `Attendance History & Summary UI` không còn là plan FE mới độc lập; đã được
  gộp và triển khai trong Plan 056.
- Plan 057 đã triển khai phần lớn Scorebook/Score Entry/Assessment Column và FE
  visual QA đã PASS, nhưng backend `pmdMain`, `pmdTest` và `build` còn bị chặn
  bởi các violation ngoài amendment scope. Chưa nên đánh dấu Plan 057 là
  `COMPLETED` trước khi quality gate được xử lý hoặc có quyết định phạm vi rõ
  ràng.
- Plan 053 còn Phase 6 bị block bởi backend contract; cần xử lý nếu release
  hardening yêu cầu đầy đủ module academic.
- Không suy diễn rằng backend có API là FE đã hoàn thành. Mỗi mục từ 2 đến 8
  vẫn cần một FE plan chi tiết, approval qua agent message, implementation,
  Storybook review khi phù hợp và validation thực tế.

## Thứ tự đề xuất

1. Chốt quality gate và trạng thái cuối của Plan 057.
2. Scorebook Configuration UI và hoàn thiện Student Score Entry UI.
3. Score Change Request UI.
4. Semester Lock & Completeness UI.
5. Transcript Viewer UI và Retake Result UI.
6. Calculation Task & Audit UI.
7. FE E2E & Release Hardening cho toàn bộ các flow.

## Definition of Done chung cho mỗi FE plan

- Có Developer Plan và approval rõ ràng qua agent message.
- Có typed API boundary, role/capability handling, loading/empty/error states
  và responsive behavior.
- Có unit/component tests; có Storybook deterministic states để review khi
  module có UI tương tác.
- Chạy validation theo script thực tế trong `FE/package.json`: lint, test,
  coverage, build và Storybook build khi áp dụng.
- Thực hiện browser walkthrough khi yêu cầu visual QA hoặc release hardening;
  nếu không có browser thì ghi rõ `NOT RUN`, không ghi `PASS`.

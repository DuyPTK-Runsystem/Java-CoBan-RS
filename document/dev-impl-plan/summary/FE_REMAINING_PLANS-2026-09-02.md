# FE Roadmap — Các plan còn lại

Ngày cập nhật: `2026-09-04`
Application version: `v2`

## Mục đích

Note này ghi lại các hạng mục FE còn lại theo roadmap hiện tại. Trạng thái
được đối chiếu với FE Dev Plan Summary, FE Dev Note Summary và các plan/note
đã có trong repository.

## Danh sách hạng mục chưa hoàn thành

| Ưu tiên | Hạng mục                              | Phạm vi chính                                                                       | Trạng thái hiện tại                                           | Plan liên quan                                                       |
| ------: | ------------------------------------- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------- | -------------------------------------------------------------------- |
|       1 | Plan 064 — FE E2E & Release Hardening | E2E flow, responsive, accessibility, permission regression, build/deploy validation | **Đã có plan, chưa implementation; đang Draft, chờ approval** | [Plan 064](../fe/release/064-fe-e2e-release-hardening-2026-09-03.md) |

## Các hạng mục đã hoàn thành, không còn là remaining plan

| Hạng mục                                        | Trạng thái mới nhất                         | Ghi chú                                                                                                                                                            |
| ----------------------------------------------- | ------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Academic Catalog Statistics & Capacity Warnings | **Đã hoàn thành trong Plan 053.2**          | Gỡ bỏ blocker Plan 053 Phase 6; BE read contract thống kê năm học và cảnh báo lệch sĩ số 20%; FE hiển thị sĩ số thực tế, banner cảnh báo và bộ chọn năm học khối   | [Plan 053.2](../fe/academic/053.2-academic-catalog-statistics-and-capacity-warning-2026-09-04.md)                                                           |
| Attendance History & Summary UI                 | **Đã hoàn thành trong Plan 056/056.1**      | Đã có history/summary, role-based navigation, automatic session lookup và xử lý `401`/`403`. Còn live-backend/browser walkthrough bổ sung nếu cần release evidence | [Plan 056](../fe/attendance/056-attendance-workspace-ui-2026-08-28.md), [Plan 056.1](../fe/attendance/056.1-attendance-authorization-and-403-2026-09-02.md) |
| Scorebook Configuration UI                      | **Đã triển khai phần chính trong Plan 057** | Đã có lifecycle, assessment columns, skill weight và lookup; chỉ còn đồng bộ trạng thái/documentation cuối cùng nếu cần                                            | [FE Plan 057](../fe/scorebook/057-scorebook-assessment-column-ui-2026-09-01.md)                                                                             |
| Student Score Entry UI                          | **Đã triển khai trong Plan 057**            | Đã có score grid phân trang, single entry, bulk entry, status/value/note validation và optimistic conflict handling                                                | [FE Plan 057](../fe/scorebook/057-scorebook-assessment-column-ui-2026-09-01.md)                                                                             |
| JWT role claim prerequisite                     | **Đã hoàn thành trong Plan 058**            | JWT có claim `role` dạng danh sách; FE dùng cho capability/navigation, backend vẫn authoritative về authorization                                                  | [BE Plan 058](../be/user-auth/058-jwt-role-claim-2026-09-02.md)                                                                                             |
| Score Change Request UI                         | **Đã hoàn thành trong Plan 059**            | Đã có UI tạo/theo dõi request cho giáo viên và duyệt/từ chối/hủy theo capability; browser QA chưa chạy                                                             | [FE Plan 059](../fe/scorebook/059-score-change-request-ui-2026-09-03.md)                                                                                    |
| Semester Lock & Completeness UI                 | **Đã hoàn thành trong Plan 060**            | Đã có report, missing-data details, notification và lock/reopen; live backend/SMTP chưa chạy                                                                       | [FE Plan 060](../fe/academic/060-semester-lock-completeness-ui-2026-09-03.md)                                                                               |
| Published Score Entry and Reopen                | **Đã hoàn thành trong Plan 065**            | Đã hỗ trợ nhập điểm trực tiếp khi `PUBLISHED` và vòng đời reopen                                                                                                   | [BE Plan 065](../../be/scorebook/065-published-score-entry-and-reopen-2026-09-03.md)                                                                        |
| Transcript Viewer UI                            | **Đã hoàn thành trong Plan 061**            | Đã hoàn thành bảng điểm học kỳ ma trận động 2 tầng header, bảng cả năm, footer summary, routing v2 và validation PASS                                              | [FE Plan 061](../fe/scorebook/061-transcript-viewer-ui-2026-09-03.md)                                                                                       |
| Retake Result UI                                | **Đã hoàn thành trong Plan 062**            | Đã có màn hình tra cứu, tạo record PLANNED, nhập/sửa điểm thi lại, hủy record, before/after score, calculation status, Storybook stories và validation PASS        | [FE Plan 062](../fe/scorebook/062-retake-result-ui-2026-09-03.md)                                                                                           |
| Class Transcript Viewer UI                      | **Đã hoàn thành trong Plan 062.1**          | Đã có 4 chế độ bảng điểm lớp (1A, 1B, 2A, 2B), phân quyền GVCN/Admin, drill-down học sinh, BE query APIs và FE tests PASS                                          | [Plan 062.1](../fe/scorebook/062.1-class-transcript-viewer-ui-2026-09-04.md)                                                                                |
| Calculation Task & Audit UI                     | **Đã hoàn thành trong Plan 063**            | Đã có monitoring task tính toán, retry xác nhận với 409 recovery, score audit trail read-only, student transcript status card, Storybook và validation PASS        | [FE Plan 063](../fe/scorebook/063-calculation-task-audit-ui-2026-09-03.md)                                                                                  |
| Student Attendance History by ID & Transcript Integration | **Đã hoàn thành trong Plan 066**            | Đã tích hợp API tra cứu chuyên cần học sinh theo studentId (`/api/v2/attendance/students/{studentId}/history`) vào `TranscriptViewerView.vue` khi xem bảng điểm chi tiết (staff drill-down), sửa lỗi 403 cho Giáo vụ và hiển thị đầy đủ số buổi vắng | [Plan 066](../../be/attendance/066-student-attendance-history-by-id-and-transcript-integration-2026-09-04.md)                                              |

## Trạng thái cần lưu ý

- `Attendance History & Summary UI` không còn là plan FE mới độc lập; đã được
  gộp và triển khai trong Plan 056, sau đó được điều chỉnh thêm trong Plan 056.1.
- Plan 057 đã triển khai phần chính của Scorebook Configuration và Student Score
  Entry. Backend PMD/build blocker trước đây đã được xử lý bằng refactor được
  ghi nhận ngày `2026-09-03`; PMD hiện PASS với 0 violation và build đã được
  rerun PASS theo summary mới nhất.
- Plan 053 Phase 6 đã được gỡ block và hoàn thành trọn vẹn qua Plan 053.2 (Dev Note 053.2), mang lại hợp đồng thống kê năm học, cảnh báo sĩ số 20% và hiển thị số liệu thực tế trên UI lớp/khối.
- Plan 061 (Transcript Viewer UI) đã hoàn thành trọn vẹn, bao gồm bảng ma trận động 2 tầng header, tích hợp chuyên cần học kỳ, ánh xạ columnNo có gap và bộ test suite 188 tests PASS.
- Backend Plan 038 còn e2e persistence test pending; đây là backend follow-up,
  không nên trộn vào scope FE nếu chưa được approve.
- Plan 062 (Retake Result UI) đã hoàn thành và tích hợp đầy đủ kiểm thử, Storybook, route và validation PASS.
- Plan 062.1 (Class Transcript & Homeroom Student Transcript Viewer UI) đã hoàn thành trọn vẹn với 4 chế độ bảng điểm lớp, API bảo mật theo GVCN/Admin và drill-down sang bảng điểm cá nhân; toàn bộ kiểm thử BE/FE và quality gates đều PASS.
- Plan 063 (Calculation Task & Audit UI) đã hoàn thành trọn vẹn với monitoring danh sách task, modal chi tiết JSON payload/error, retry confirmation xử lý 409 conflict, tra cứu audit trail bất biến và thẻ trạng thái đồng bộ bảng điểm; toàn bộ kiểm thử FE (310 tests) và build/storybook đều PASS.
- Plan 066 (Student Attendance History by Student ID and Transcript Integration) đã hoàn thành cung cấp API tra cứu chuyên cần theo studentId cho cán bộ/giáo viên và tích hợp vào màn hình bảng điểm cá nhân chi tiết khi drill-down; toàn bộ kiểm thử BE/FE và quality gates đều PASS.

## Thứ tự đề xuất

1. Phê duyệt và thực hiện Plan 064 — FE E2E & Release Hardening (hạng mục FE duy nhất còn lại trên roadmap).

## Definition of Done chung cho mỗi FE plan

- Đầy đủ luồng giao diện theo spec, xử lý các trạng thái loading, error, empty và responsive behavior.
- Có unit/component tests; có Storybook deterministic states để review khi
  module có UI tương tác.
- Có E2E hoặc browser walkthrough cho các role `ADMIN`, `ACADEMIC_OFFICE`,
  `TEACHER`, `STUDENT` khi flow yêu cầu permission regression.
- Kiểm tra các mã `401`, `403`, `404`, `409`; persistence sau mutation; phân
  trang; optimistic locking và trạng thái `IN_PROGRESS`/`FINISH` nếu áp dụng.
- Chạy validation theo script thực tế trong `FE/package.json`: lint, test,
  coverage, build và Storybook build khi áp dụng.
- Thực hiện browser walkthrough khi yêu cầu visual QA hoặc release hardening;
  nếu không có browser thì ghi rõ `NOT RUN`, không ghi `PASS`.

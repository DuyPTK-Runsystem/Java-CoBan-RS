# FE Roadmap — Các plan còn lại

Ngày cập nhật: `2026-09-04`
Application version: `v2`

## Mục đích

Note này ghi lại các hạng mục FE còn lại theo roadmap hiện tại. Trạng thái
được đối chiếu với FE Dev Plan Summary, FE Dev Note Summary và các plan/note
đã có trong repository.

## Danh sách hạng mục chưa hoàn thành

| Ưu tiên | Hạng mục                               | Phạm vi chính                                                                       | Trạng thái hiện tại                                           | Plan liên quan                                                                    |
| ------: | -------------------------------------- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------- | --------------------------------------------------------------------------------- |
|       1 | Plan 053 — Academic Catalog Phase 6    | Báo cáo thống kê sĩ số/điểm trung bình/cảnh báo                                     | **Bị block** bởi backend chưa có read contract phù hợp        | [FE Plan 053](../fe/academic/053-grade-class-subject-management-ui-2026-08-27.md) |
|       2 | Plan 063 — Calculation Task & Audit UI | Theo dõi task lỗi, retry, audit log, trạng thái tính toán                           | **Đã có plan, chưa implementation; đang Draft, chờ approval** | [Plan 063](../fe/scorebook/063-calculation-task-audit-ui-2026-09-03.md)           |
|       3 | Plan 064 — FE E2E & Release Hardening  | E2E flow, responsive, accessibility, permission regression, build/deploy validation | **Đã có plan, chưa implementation; đang Draft, chờ approval** | [Plan 064](../fe/release/064-fe-e2e-release-hardening-2026-09-03.md)              |

## Các hạng mục đã hoàn thành, không còn là remaining plan

| Hạng mục                         | Trạng thái mới nhất                         | Ghi chú                                                                                                                                                            |
| -------------------------------- | ------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Attendance History & Summary UI  | **Đã hoàn thành trong Plan 056/056.1**      | Đã có history/summary, role-based navigation, automatic session lookup và xử lý `401`/`403`. Còn live-backend/browser walkthrough bổ sung nếu cần release evidence | [Plan 056](../fe/attendance/056-attendance-workspace-ui-2026-08-28.md), [Plan 056.1](../fe/attendance/056.1-attendance-authorization-and-403-2026-09-02.md) |
| Scorebook Configuration UI       | **Đã triển khai phần chính trong Plan 057** | Đã có lifecycle, assessment columns, skill weight và lookup; chỉ còn đồng bộ trạng thái/documentation cuối cùng nếu cần                                            | [FE Plan 057](../fe/scorebook/057-scorebook-assessment-column-ui-2026-09-01.md)                                                                             |
| Student Score Entry UI           | **Đã triển khai trong Plan 057**            | Đã có score grid phân trang, single entry, bulk entry, status/value/note validation và optimistic conflict handling                                                | [FE Plan 057](../fe/scorebook/057-scorebook-assessment-column-ui-2026-09-01.md)                                                                             |
| JWT role claim prerequisite      | **Đã hoàn thành trong Plan 058**            | JWT có claim `role` dạng danh sách; FE dùng cho capability/navigation, backend vẫn authoritative về authorization                                                  | [BE Plan 058](../be/user-auth/058-jwt-role-claim-2026-09-02.md)                                                                                             |
| Score Change Request UI          | **Đã hoàn thành trong Plan 059**            | Đã có UI tạo/theo dõi request cho giáo viên và duyệt/từ chối/hủy theo capability; browser QA chưa chạy                                                             | [FE Plan 059](../fe/scorebook/059-score-change-request-ui-2026-09-03.md)                                                                                    |
| Semester Lock & Completeness UI  | **Đã hoàn thành trong Plan 060**            | Đã có report, missing-data details, notification và lock/reopen; live backend/SMTP chưa chạy                                                                       | [FE Plan 060](../fe/academic/060-semester-lock-completeness-ui-2026-09-03.md)                                                                               |
| Published Score Entry and Reopen | **Đã hoàn thành trong Plan 065**            | Đã hỗ trợ nhập điểm trực tiếp khi `PUBLISHED` và vòng đời reopen                                                                                                   | [BE Plan 065](../../be/scorebook/065-published-score-entry-and-reopen-2026-09-03.md)                                                                        |
| Transcript Viewer UI             | **Đã hoàn thành trong Plan 061**            | Đã hoàn thành bảng điểm học kỳ ma trận động 2 tầng header, bảng cả năm, footer summary, routing v2 và validation PASS                                              | [FE Plan 061](../fe/scorebook/061-transcript-viewer-ui-2026-09-03.md)                                                                                       |
| Retake Result UI                 | **Đã hoàn thành trong Plan 062**            | Đã có màn hình tra cứu, tạo record PLANNED, nhập/sửa điểm thi lại, hủy record, before/after score, calculation status, Storybook stories và validation PASS        | [FE Plan 062](../fe/scorebook/062-retake-result-ui-2026-09-03.md)                                                                                           |

## Trạng thái cần lưu ý

- `Attendance History & Summary UI` không còn là plan FE mới độc lập; đã được
  gộp và triển khai trong Plan 056, sau đó được điều chỉnh thêm trong Plan 056.1.
- Plan 057 đã triển khai phần chính của Scorebook Configuration và Student Score
  Entry. Backend PMD/build blocker trước đây đã được xử lý bằng refactor được
  ghi nhận ngày `2026-09-03`; PMD hiện PASS với 0 violation và build đã được
  rerun PASS theo summary mới nhất.
- Plan 053 còn Phase 6 bị block bởi backend contract; cần xử lý nếu release
  hardening yêu cầu đầy đủ module academic.
- Plan 061 (Transcript Viewer UI) đã hoàn thành trọn vẹn, bao gồm bảng ma trận động 2 tầng header, tích hợp chuyên cần học kỳ, ánh xạ columnNo có gap và bộ test suite 188 tests PASS.
- Backend Plan 038 còn e2e persistence test pending; đây là backend follow-up,
  không nên trộn vào scope FE nếu chưa được approve.
- Plan 062 (Retake Result UI) đã hoàn thành và tích hợp đầy đủ kiểm thử, Storybook, route và validation PASS.

## Thứ tự đề xuất

1. Phê duyệt và triển khai Plan 063 — Calculation Task & Audit UI.
2. Xử lý blocker Plan 053 Phase 6 nếu cần đầy đủ academic catalog.
3. Phê duyệt và thực hiện Plan 064 — FE E2E & Release Hardening.

## Definition of Done chung cho mỗi FE plan
  và responsive behavior.
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

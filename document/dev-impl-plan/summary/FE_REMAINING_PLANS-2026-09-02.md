# FE Roadmap — Các plan còn lại

Ngày cập nhật: `2026-09-03`
Application version: `v2`

## Mục đích

Note này ghi lại các hạng mục FE còn lại theo roadmap hiện tại. Trạng thái
được đối chiếu với FE Dev Plan Summary, FE Dev Note Summary và các plan/note
đã có trong repository.

## Danh sách hạng mục

| Ưu tiên | Hạng mục | Phạm vi chính | Trạng thái hiện tại | Plan liên quan |
|---:|---|---|---|---|
| 1 | Score Change Request UI | Giáo viên tạo request; giáo vụ duyệt/từ chối; xem lịch sử | **Còn làm FE**; backend Plan 038 đã triển khai. Backend vẫn còn e2e persistence test pending theo Dev Note | [BE Plan 038](../be/scorebook/038-score-change-request-2026-08-25.md) |
| 2 | Semester Lock & Completeness UI | Báo cáo thiếu dữ liệu, checkpoint, khóa/mở học kỳ, notification và trạng thái dữ liệu | **Còn làm FE**; backend Plan 039/040 đã có. UI cần report, decision/checkpoint, notification list/dispatch/retry, lock/reopen, quyền và trạng thái lỗi | [BE Plan 039](../be/academic/039-semester-lock-2026-08-25.md), [BE Plan 040](../be/academic/040-semester-completeness-notification-cr-sem-001-2026-08-25.md) |
| 3 | Transcript Viewer UI | Bảng điểm theo học kỳ/năm, điểm môn, `IN_PROGRESS`/`FINISH`, thời điểm tính gần nhất | **Còn làm FE**; cần đồng bộ lại trạng thái và contract query của Plan 046 trước khi lập FE plan | [BE Plan 041](../be/scorebook/041-transcript-result-schema-foundation-2026-08-25.md), [BE Plan 044](../be/scorebook/044-transcript-aggregation-2026-08-26.md), [BE Plan 046](../be/scorebook/046-transcript-query-api-2026-08-26.md) |
| 4 | Retake Result UI | Danh sách thi lại, nhập điểm thi lại, hiển thị điểm trước/sau thi lại | **Còn làm FE**; backend Plan 045 đã có foundation, API và calculation integration | [BE Plan 045](../be/scorebook/045-retake-foundation-and-calculation-integration-2026-08-26.md) |
| 5 | Calculation Task & Audit UI | Theo dõi task lỗi, retry, audit log, trạng thái tính toán | **Còn làm FE**; cần danh sách/lọc task `FAILED`, lỗi gần nhất, retry đơn/hàng loạt, transcript status, audit log, pagination và role/capability | [BE Plan 047](../be/scorebook/047-be-operational-closure-2026-08-26.md) |
| 6 | FE E2E & Release Hardening | E2E flow, responsive, accessibility, permission regression, build/deploy validation | **Chưa có FE plan chi tiết**; thực hiện sau các flow chính, gồm browser walkthrough với live backend và smoke test deploy | Chưa gán plan |

## Các hạng mục đã hoàn thành, không còn là remaining plan

| Hạng mục | Trạng thái mới nhất | Ghi chú |
|---|---|---|
| Attendance History & Summary UI | **Đã hoàn thành trong Plan 056/056.1** | Đã có history/summary, role-based navigation, automatic session lookup và xử lý `401`/`403`. Còn live-backend/browser walkthrough bổ sung nếu cần release evidence | [Plan 056](../fe/attendance/056-attendance-workspace-ui-2026-08-28.md), [Plan 056.1](../fe/attendance/056.1-attendance-authorization-and-403-2026-09-02.md) |
| Scorebook Configuration UI | **Đã triển khai phần chính trong Plan 057** | Đã có lifecycle, assessment columns, skill weight và lookup; chỉ còn đồng bộ trạng thái/documentation cuối cùng nếu cần | [FE Plan 057](../fe/scorebook/057-scorebook-assessment-column-ui-2026-09-01.md) |
| Student Score Entry UI | **Đã triển khai trong Plan 057** | Đã có score grid phân trang, single entry, bulk entry, status/value/note validation và optimistic conflict handling | [FE Plan 057](../fe/scorebook/057-scorebook-assessment-column-ui-2026-09-01.md) |
| JWT role claim prerequisite | **Đã hoàn thành trong Plan 058** | JWT có claim `role` dạng danh sách; FE dùng cho capability/navigation, backend vẫn authoritative về authorization | [BE Plan 058](../be/user-auth/058-jwt-role-claim-2026-09-02.md) |

## Trạng thái cần lưu ý

- `Attendance History & Summary UI` không còn là plan FE mới độc lập; đã được
  gộp và triển khai trong Plan 056, sau đó được điều chỉnh thêm trong Plan 056.1.
- Plan 057 đã triển khai phần chính của Scorebook Configuration và Student Score
  Entry. Backend PMD/build blocker trước đây đã được xử lý bằng refactor được
  ghi nhận ngày `2026-09-03`; PMD hiện PASS với 0 violation và build đã được
  rerun PASS theo summary mới nhất.
- Plan 053 còn Phase 6 bị block bởi backend contract; cần xử lý nếu release
  hardening yêu cầu đầy đủ module academic.
- Plan 046 có trạng thái không đồng nhất: nội dung Developer Plan còn ghi
  `Proposed`, trong khi các summary ghi `Completed`. Cần chốt lại contract và
  trạng thái tài liệu trước khi bắt đầu Transcript Viewer UI.
- Backend Plan 038 còn e2e persistence test pending; đây là backend follow-up,
  không nên trộn vào scope FE nếu chưa được approve.
- Không suy diễn rằng backend có API là FE đã hoàn thành. Các mục remaining
  vẫn cần FE plan chi tiết, approval qua agent message, implementation,
  Storybook review khi phù hợp và validation thực tế.

## Thứ tự đề xuất

1. Chốt trạng thái/documentation cuối của Plan 057 và Plan 056.1.
2. Score Change Request UI.
3. Semester Lock & Completeness UI.
4. Đồng bộ contract Plan 046, sau đó làm Transcript Viewer UI.
5. Retake Result UI.
6. Calculation Task & Audit UI.
7. FE E2E & Release Hardening cho toàn bộ các flow.

## Definition of Done chung cho mỗi FE plan

- Có Developer Plan và approval rõ ràng qua agent message.
- Có typed API boundary, role/capability handling, loading/empty/error states
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

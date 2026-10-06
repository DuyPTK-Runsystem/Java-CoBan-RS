# Dev Note FE 088 — Điều hướng đúng revision từ danh sách TKB

- Ngày: 2026-10-02.
- Kế hoạch liên quan: [Plan 088](../../../dev-impl-plan/summary/088-spring-ai-timetable-agent-2026-10-01.md), APPROVED.
- Scope hoàn tất: chẩn đoán và sửa lỗi nhấn Biên tập ở các hàng khác nhau nhưng mở chung một revision.

## Thay đổi

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/timetable/domain/DTOs/response/ResTimetableSummaryDTO.java` và `.../timetable/service/TimetableService.java`: bổ sung `revisionId` vào summary; từng hàng được tạo từ `TimetableRevision` nên dùng `r.getId()`.
- `FE/src/types/timetable.ts`: khai báo `revisionId` trong `TimetableSummary`.
- `FE/src/views/timetable/TimetableListView.vue`: nút Biên tập và điều hướng sau tạo mới dùng revision ID, phù hợp với `GET /api/v3/timetables/{id}`, nơi `id` được tra trong revision repository.
- `FE/src/views/timetable/timetableWorkspacePath.ts`: helper route độc lập để component và regression test dùng chung; không export từ `<script setup>`.
- `FE/src/views/timetable/TimetableListView.spec.ts`: regression test đảm bảo các hàng có chung `timetableId` nhưng revision ID khác nhau tạo ra route khác nhau.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/timetable/service/TimetableServiceTest.java`: regression test `pageSummaries_exposesEachRevisionIdWhenHeadIsShared` kiểm tra hai revision cùng head giữ chung `timetableId` nhưng summary mang `revisionId` riêng.

## Quyết định và nguyên nhân

`TimetableService.pageSummaries` trả một hàng cho mỗi `TimetableRevision`, trong khi summary trước đó chỉ lộ `timetableId` của `TimetableHead`. FE đưa giá trị head ID này vào route; workspace dùng route param làm ID gọi API detail, mà backend tra theo revision ID. Summary nay mang cả hai ID và danh sách điều hướng bằng revision ID của hàng.

## Validation

| Lệnh / gate | Kết quả |
|---|---|
| Focused BE `TimetableServiceTest` | PASS — QA rerun xác nhận `pageSummaries_exposesEachRevisionIdWhenHeadIsShared` đạt |
| Full backend test | FAIL / incomplete — QA bị gián đoạn tại `ClientOptions.kt:561` với OOM |
| `checkstyleMain` / `checkstyleTest` | PASS — QA xác nhận cả hai gate |
| `pmdMain` | PASS |
| `pmdTest` | FAIL — 476 findings |
| Build excluding tests | PASS |
| Standard backend build | Incomplete — không có kết quả hoàn tất |
| FE `TimetableListView.spec.ts` | PASS — QA xác nhận route regression đạt sau khi helper chuyển sang module độc lập |
| Scoped ESLint | PASS — QA xác nhận |
| `npm run build` | PASS — QA xác nhận |

## Sai khác, blockers và bước tiếp theo

- Thay đổi mở rộng qua BE response DTO/service do lỗi nằm ở thiếu revision ID trong API summary.
- QA đã xác nhận các regression FE/BE mới và scoped build gates đạt. Full backend suite, PMD test và standard backend build vẫn là các giới hạn đã nêu trong bảng.

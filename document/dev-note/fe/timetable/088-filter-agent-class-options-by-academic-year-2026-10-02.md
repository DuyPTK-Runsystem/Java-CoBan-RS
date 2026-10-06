# Dev Note FE 088 — Lọc lớp gợi ý TKB theo năm học

- Ngày: 2026-10-02.
- Kế hoạch liên quan: [Plan 088](../../../dev-impl-plan/summary/088-spring-ai-timetable-agent-2026-10-01.md), APPROVED; nền v3 và CR-V4-001.
- Scope hoàn tất: sửa nguồn danh sách lớp của picker gợi ý TKB để chỉ tải các lớp thuộc năm học của học kỳ trên timetable.

## Thay đổi

- `FE/src/views/timetable/TimetableWorkspaceView.vue`: tải semester metadata, tìm `academicYearId` bằng `detail.semesterId`, rồi gọi `fetchSchoolClasses(token, academicYearId)`. Nếu không tìm được học kỳ tương ứng, danh sách lớp để trống thay vì gọi API không lọc năm.
- Nguyên nhân: API lớp nhận `academicYearId` tùy chọn; bỏ trống tham số trả lớp của nhiều năm. `classCode` chỉ duy nhất trong phạm vi năm học, nên các lớp như `6A1` ở năm khác nhau cùng xuất hiện. ID dedupe không loại được bản ghi khác nhau này.

## Validation

| Lệnh / gate | Kết quả |
|---|---|
| Focused Vitest — 3 files / 16 tests | PASS — QA subculi độc lập |
| Focused ESLint — 5 files | PASS — QA subculi độc lập |
| `npm run build` | PASS — QA subculi độc lập |
| Storybook build | NOT RUN |
| Browser QA | NOT RUN |

## Sai khác và việc tiếp theo

- Không mở rộng sang backend/API contract; dùng query filter đã có.
- QA độc lập xác nhận focused tests, ESLint và build đạt; Storybook/browser chưa chạy.

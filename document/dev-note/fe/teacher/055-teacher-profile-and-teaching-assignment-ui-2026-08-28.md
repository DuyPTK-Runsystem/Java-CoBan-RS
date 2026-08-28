# Dev Note 055: Teacher Profile & Teaching Assignment UI

## Liên kết và approval

- Developer Plan: `document/dev-impl-plan/fe/teacher/055-teacher-profile-and-teaching-assignment-ui-2026-08-28.md`
- Approval: Đã nhận phê duyệt qua agent message: `tôi approve plan 55`.

## Phạm vi đã hoàn thành

- Tạo route `/v2/teachers` cho hồ sơ giáo viên: lọc trạng thái, tìm kiếm client-side, xem chi tiết, tạo/sửa, xóa có xác nhận và liên kết `userId`.
- Tạo route `/v2/teaching-assignments`: context năm học/khối/lớp/học kỳ, GVCN, GVBM và lịch dạy theo giáo viên; hỗ trợ create/replace/end.
- Bám current v2 API contract; không thay đổi backend.
- Bổ sung Storybook deterministic cho các component mới.

## Files thay đổi

- Types/services: `FE/src/types/teacher.ts`, `FE/src/types/assignment.ts`, `FE/src/services/teacherApi.ts`, `FE/src/services/assignmentApi.ts` và hai file spec.
- Components: `TeacherTable`, `TeacherDialog`, `TeacherDetailDialog`, `AssignmentContextPanel`, `HomeroomAssignmentCard`, `HomeroomHistoryDialog`, `ClassSubjectAssignmentTable`, `HomeroomAssignmentDialog`, `SubjectAssignmentDialog`, `TeacherAssignmentScheduleTable` và stories.
- Views/routing: `TeacherListView.vue`, `TeachingAssignmentView.vue`, hai view spec, `FE/src/router/index.ts`, `AuthenticatedV2ShellView.vue`.
- Styling: bổ sung layout cho tab và assignment panels trong `FE/src/styles.css`.
- Documentation: cập nhật Plan Summary và Dev Note summaries.

## Quyết định triển khai

- Dùng `apiClient`/Bearer token hiện có; LocalDate giữ nguyên `yyyy-MM-dd`.
- Teacher status chỉ cho phép `ACTIVE`, `ON_LEAVE`, `INACTIVE`; assignment status hiển thị `ACTIVE`/`ENDED` theo wire contract.
- Teacher đang `INACTIVE` hoặc `ON_LEAVE` bị loại khỏi lựa chọn phân công mới ở component; backend vẫn là nguồn authorization/validation cuối cùng.
- Query hiện tại không có endpoint phân công theo class-subject, nên view tổng hợp các assignment theo từng giáo viên rồi lọc theo `classSubjectId`.

## Validation

- `npm run lint`: **PASS**
- `npm run test`: **PASS** (35 test files, 106 tests)
- `npm run test:coverage`: **PASS**
- `npm run build`: **PASS**
- `npm run build-storybook`: **PASS**

## Deviations và rủi ro còn lại

- Plan dự kiến nhiều component spec riêng; implementation đã có service/view coverage và Storybook, còn một số component chỉ dùng coverage gián tiếp qua build/test suite.
- Teacher schedule hiện dùng `classSubjects` context đang chọn để map tên lớp/môn; backend chưa cung cấp metadata đầy đủ trong assignment response. Khi API mở rộng, cần bổ sung loader metadata toàn cục.
- Quyền truy cập vẫn do backend quyết định; FE không suy diễn role từ session.

## Next steps

- Khi backend cung cấp endpoint query assignment theo class-subject hoặc metadata lớp/môn trong response, cập nhật service/view mapping và bổ sung contract tests.

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
- Teacher schedule hiện ưu tiên metadata tên lớp/môn do backend trả về; vẫn giữ fallback qua context hoặc mã để tương thích dữ liệu cũ.
- Quyền truy cập vẫn do backend quyết định; FE không suy diễn role từ session.

## Next steps

- Khi backend cung cấp endpoint query assignment theo class-subject hoặc metadata lớp/môn trong response, cập nhật service/view mapping và bổ sung contract tests.

## Amendment 55.1 — Teacher chỉ xem phân công của mình

- Approval: Đã được duyệt qua agent message: `ukm, làm 55.1 đi`.
- Actual scope: mở query theo giáo viên cho `TEACHER` với self-scope và query theo lớp-học kỳ cho Teacher có assignment trong lớp. Backend vẫn trả `403` cho teacherId/lớp ngoài scope; mutation chỉ dành cho Office/Admin.
- FE: Ở lần cập nhật 55.1 đầu tiên, Teacher được đưa thẳng vào tab lịch dạy của mình, ẩn tab theo lớp và bộ chọn giáo viên; nội dung này được mở rộng ở lần cập nhật 55.1 lần 2 bên dưới để hỗ trợ các lớp Teacher được phân công.
- Files bổ sung/cập nhật: `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/assignment/controller/AssignmentController.java`, `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/assignment/service/SubjectTeachingAssignmentAccessService.java`, `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/assignment/service/SubjectTeachingAssignmentAccessServiceTest.java`, `FE/src/views/TeachingAssignmentView.vue`, `FE/src/components/AssignmentContextPanel.vue`, `FE/src/views/TeachingAssignmentView.spec.ts`, `document/application-doc/v2/frontend-api/03-teacher-assignment-enrollment.md`.
- Backend Validation Result: `test PASS` (`./gradlew test`), `checkstyle PASS` (`./gradlew checkstyleMain`, còn warning baseline ngoài scope), `PMD PASS` (`./gradlew pmdMain` và `build` gồm `pmdTest`), `build PASS` (`./gradlew build`).
- FE validation: `npm run test -- --run src/services/assignmentApi.spec.ts src/views/TeachingAssignmentView.spec.ts` PASS (3 tests), `npm run lint` PASS, `npm run build` PASS. Backend: `test`, `checkstyleMain`, `pmdMain`, `build` PASS (24 warning Checkstyle baseline ngoài scope). Live QA cho endpoint mới NOT RUN: backend đang chạy cần restart để nạp route mới.
- Ghi chú lịch sử: trước lần cập nhật 55.1 lần 2, assignment response chỉ có `classSubjectId` nên lịch của Teacher có thể dùng fallback ID; contract hiện đã bổ sung metadata tên lớp/môn.

### Amendment 55.1 cập nhật lần 2

- Đổi nhãn tab theo role: Office/Admin dùng `Phân công theo lớp` và `Phân công theo GV`; Teacher dùng `Phân công theo lớp` và `Phân công của tôi`.
- Teacher có assignment ACTIVE được xem tab theo lớp nhưng danh sách lớp chỉ gồm lớp GVCN hoặc lớp có phân công GVBM của chính Teacher; backend từ chối class ngoài scope bằng `403`.
- Bổ sung endpoint đọc GVCN theo teacher và metadata `classId/className/classCode/subjectId/subjectName/semesterId` cho assignment response; bảng ưu tiên tên lớp/môn.
- FE Teacher tải context giới hạn theo các assignment ACTIVE của tài khoản; không gọi chuỗi assignment của giáo viên khác.
- Khắc phục lỗi Teacher nhận `403` khi tải màn hình: `GET /api/v2/grades` cho phép đọc với user đã đăng nhập, còn POST/PUT/DELETE vẫn office-only.
- Khắc phục lỗi bảng Teacher hiển thị `Chưa phân công` sai: thêm `GET /api/v2/assignments/classes/{classId}/subjects?semesterId={semesterId}`; endpoint kiểm tra class-scope rồi trả toàn bộ GVBM trong lớp-học kỳ. FE dùng endpoint này cho cả Teacher và Office thay cho việc lọc assignment của Teacher hoặc gọi N endpoint theo từng giáo viên.
- Chuẩn hóa role có hoặc không có tiền tố `ROLE_` ở FE và truyền `read-only` cho toàn bộ khu vực phân công của Teacher, bao gồm cả nút GVCN/GVBM.
- Loại bỏ các lần gọi lặp `assignments/classes/{classId}` và `classes/{classId}/subjects` khi khởi tạo/đổi context; watcher là đầu mối duy nhất tải lại lớp.
- Khôi phục bộ chọn Khối cho Teacher và chỉ hiển thị các khối chứa lớp mà Teacher được phân công; đổi Khối vẫn chỉ đưa đến các Lớp trong scope.

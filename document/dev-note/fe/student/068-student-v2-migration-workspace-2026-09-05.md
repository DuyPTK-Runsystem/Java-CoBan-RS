# Dev Note 068: Khắc phục chuyển đổi Student legacy sang V2

## 1. Developer Plan và phê duyệt

- **Developer Plan:** [`document/dev-impl-plan/FE/student/068-student-v2-migration-workspace-2026-09-05.md`](../../../dev-impl-plan/FE/student/068-student-v2-migration-workspace-2026-09-05.md)
- **Phê duyệt:** người dùng đã phê duyệt phương án khắc phục trong phiên tiếp nối.
- **Không tạo Plan 069:** theo chỉ thị trực tiếp của người dùng; đây là remediation của Plan 068.

## 2. Phạm vi thực tế đã triển khai

- Thay contract legacy của workspace `/v2/students` bằng API `/api/v2/students`: list/detail, tạo, cập nhật, xóa an toàn, sinh mã và `PATCH /{studentId}/status`.
- Bổ sung dữ liệu V2 cho hồ sơ/list: `gender`, `status`, lớp hiện tại và thông tin account được phép xem; tìm kiếm server-side theo mã/tên/ngày sinh/status/classId với phân trang và sort whitelist.
- Thêm migration `V20__add_student_gender.sql`; map dữ liệu giới tính vào `StudentInfo` và V2 DTO.
- Áp dụng quyền đọc `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`; quyền mutation chỉ `ADMIN`, `ACADEMIC_OFFICE`; FE router guard chặn `STUDENT` khỏi `/v2/students/**`.
- UI đã chuyển sang typed V2 API, hoàn thiện filter `classId`, dữ liệu thật cho bảng/chi tiết, transition `INACTIVE`/`GRADUATED`, và xử lý riêng các lỗi API. `averageScore` không còn thuộc form/workspace V2.
- Xóa cứng kiểm tra enrollment học vụ trước khi thực hiện; FE chỉ đề xuất transition trạng thái khi nhận lỗi ràng buộc tương ứng.
- Bổ sung/cập nhật test FE và BE cho V2 API, authorization, filter, lifecycle, safe delete, mã học sinh, V3 account và Flyway.
- Sửa nullable contract FE cho `dateOfBirth`, `address`, `gender`; form edit chuẩn hóa dữ liệu nullable về giá trị form-safe và chỉ nhận gender enum hợp lệ.

## 3. Files changed (theo mục đích)

### Frontend V2

- `FE/src/types/student.ts`
- `FE/src/services/studentApi.ts`, `FE/src/services/apiClient.ts`
- `FE/src/router/index.ts`
- `FE/src/views/StudentListView.vue`, `StudentFormView.vue`, `StudentDetailView.vue`
- `FE/src/components/StudentForm.vue`, `StudentSearchForm.vue`, `StudentTable.vue`
- Test liên quan: `FE/src/services/studentApi.spec.ts`, `apiClient.spec.ts`, `FE/src/router/index.spec.ts`, `FE/src/components/StudentForm.spec.ts`, `StudentSearchForm.spec.ts`, `StudentTable.spec.ts`, và `FE/src/views/StudentListView.spec.ts`, `StudentFormView.spec.ts`, `StudentDetailView.spec.ts`.

### Backend V2 và dữ liệu

- `BE/BaiTap-RS/src/main/resources/db/migration/V20__add_student_gender.sql`
- Student API/service: `student/controller/StudentV2Controller.java`, `student/service/StudentV2Service.java`, `StudentV2AccessService.java`, `StudentV2Specifications.java`, `StudentV2SortResolver.java`, `StudentV2ResponseMapper.java`, `StudentCodeGenerationService.java`.
- Student DTO/entity/repository: `StudentInfo.java`, V2 request/response DTOs, `StudentClassLookupRepository.java`, `enrollment/repository/StudentYearEnrollmentRepository.java`.
- Tương thích V3: `StudentAccountService.java`, `StudentV3Controller.java`, V3 request/response DTOs và `StudentService.java`.
- Test: `StudentV2ServiceTest.java`, `StudentAuthorizationIntegrationTest.java`, `StudentServiceCodeTest.java`, `StudentServiceAccountTest.java`, `StudentServiceMutationTest.java`, `StudentServiceFetchTest.java`, `FlywayMigrationTest.java`.

## 4. Quyết định và deviation so với Plan 068

- API V1 được giữ cho tương thích legacy, nhưng workspace V2 không còn gọi V1.
- R5 không còn cố truyền `status` qua `PUT /api/v1/students/{id}`; lifecycle sử dụng endpoint V2 riêng và quyền mutation được áp dụng ở backend.
- Dev Note trước đó ghi tất cả milestone/validation hoàn tất là không chính xác. Bản này thay thế trạng thái đó bằng kết quả validation thực tế và các rủi ro còn lại.
- Provision account V3 đã không còn hiển thị password mặc định ở FE; tuy nhiên backend hiện vẫn fallback password `12345678` khi request không có password. Chưa có contract activation/reset credential an toàn để bàn giao mật khẩu tạm hoặc buộc đổi mật khẩu. Đây là blocker còn lại, không được xem là hoàn tất bảo mật credential.

## 5. Validation Result

| Hạng mục | Kết quả thực tế | Ghi chú |
| --- | --- | --- |
| `npm --prefix FE run lint` | **PASS** | Không còn 8 lỗi lint đã phát hiện trước remediation. |
| `npm --prefix FE run test -- --run` | **PASS** | 82 test files, 427 tests. |
| `npm --prefix FE run build` | **PASS** | Build/typecheck frontend thành công. |
| `BE/BaiTap-RS/gradlew.bat test` | **PASS** | 87 suites, 376 tests. |
| `BE/BaiTap-RS/gradlew.bat checkstyleMain` | **PASS** | Có 24 warning entry baseline thuộc scorebook/teacher; task vẫn BUILD SUCCESSFUL. |
| `BE/BaiTap-RS/gradlew.bat pmdMain` | **FAIL** | 5 finding baseline ngoài student ở `SchoolClassService` và `ClassTranscriptQueryService`. |
| `BE/BaiTap-RS/gradlew.bat build` | **FAIL** | Chỉ bị chặn bởi PMD baseline nêu trên. |
| Browser/live API walkthrough | **NOT RUN** | Chưa có bằng chứng click-through với backend đang chạy. |
| Git state | **NOT RUN** | Không kiểm tra/không thao tác Git: `.git/index` đang corrupt. |

## 6. Blocker, risk và bước tiếp theo

1. Thiết kế và triển khai contract activation/reset credential V3: tạo secret ngẫu nhiên ở server, chỉ lưu hash, delivery qua luồng reset/kích hoạt được xác thực, và bắt buộc đổi password. Sau đó loại bỏ hoàn toàn fallback `12345678`.
2. Xử lý hoặc có waiver chính thức cho 5 PMD baseline finding để backend build xanh.
3. Khôi phục `.git/index` bằng quy trình do người dùng quản lý trước khi review/commit; không có Git action nào được thực hiện trong remediation này.
4. Chạy browser/live API walkthrough cho các role ADMIN, ACADEMIC_OFFICE, TEACHER, STUDENT và kiểm tra lifecycle/safe delete trên dữ liệu thực.

## 7. Amendment 2026-09-06: loại bỏ Gender và hiển thị User theo phản hồi UAT

- Không tạo Plan 069 theo chỉ thị; đây là amendment được người dùng phê duyệt trong scope remediation 068.
- Xóa toàn bộ contract `gender` của Student V2/V3: frontend type, form, bảng, chi tiết, payload và test; backend entity `StudentInfo`, request/response DTO, service/mapper, test và migration chưa áp dụng `V20__add_student_gender.sql`.
- Không tạo migration `DROP COLUMN`: người dùng xác nhận DB hiện không có `student_info.gender`; việc xóa V20 tránh tạo drift schema mới.
- Giáo viên: xóa hiển thị Gender ở form/bảng/chi tiết và xóa cột Tài khoản/User ở bảng/chi tiết. Vẫn giữ `teacher.user_id` và field API nội bộ để không phá liên kết tài khoản, phân quyền hay dữ liệu hiện hữu.
- Giữ Tổ chuyên môn (`teacher.department`), vì schema đã có cột `department VARCHAR(100)` từ V5.

### Validation amendment

| Hạng mục | Kết quả thực tế |
| --- | --- |
| `FE: npm.cmd run lint` | **PASS** |
| `FE: npm.cmd run test` | **PASS** — 82 files, 427 tests |
| `FE: npm.cmd run build` | **PASS** |
| `BE: gradlew test` | **PASS** — 87 XML reports, 0 failure/error sau khi xóa artefact `build/test-results/test` bị corrupt |
| Chrome visual QA | **PASS** — `/v2/students` không còn cột Gender; `/v2/teachers` không còn Gender/Tài khoản, vẫn có Tổ chuyên môn |
| BE Checkstyle / PMD / full build | **NOT CLOSED** — Checkstyle report hiện còn baseline ngoài scope; không đưa kết quả này thành PASS |

# Dev Note 067 — Kiểm toán và Đồng bộ Ma trận Phân quyền Thực tế & FE Remaining Plans

Ngày: `2026-09-04`  
Module / Lĩnh vực: `workflow-skill` / `cross-area`  
Trạng thái: **Completed**  
Tài liệu liên quan:
- [FE Remaining Plans](../../../dev-impl-plan/summary/FE_REMAINING_PLANS-2026-09-02.md)
- [Actual Permission Matrix](../../../application-doc/v2/ActualPermissionMatrix.md)

---

## 1. Bối cảnh & Yêu cầu

Yêu cầu từ người dùng:
> "Căn cứ vào fe remaining plans và tài liệu hiện trạng phân quyền, hãy update tài liệu dựa trên thực trạng, không giả định bất cứ thứ gì"

Sau khi hoàn thành liên tiếp các Developer Plans gần đây (053.2, 056/056.1, 057, 058, 059, 060, 061, 062, 062.1, 063, 065, 066), tài liệu hiện trạng phân quyền ([`ActualPermissionMatrix.md`](file:///home/duyptk/Coding/HoiNhapJava/Java-CoBan/document/application-doc/v2/ActualPermissionMatrix.md)) và lộ trình FE còn lại ([`FE_REMAINING_PLANS-2026-09-02.md`](file:///home/duyptk/Coding/HoiNhapJava/Java-CoBan/document/dev-impl-plan/summary/FE_REMAINING_PLANS-2026-09-02.md)) có một số điểm chưa phản ánh sát thực tế mã nguồn (code drift) hoặc thiếu sót số liệu kiểm toán.

---

## 2. Phạm vi Kiểm toán & Hiện thực Thực tế (Actual Scope Completed)

### 2.1. Kiểm toán Toàn diện Backend (Spring Boot)
- **Số lượng `@RestController` & Endpoints thực tế:** Hệ thống có chính xác **27 `@RestController`** với tổng cộng **132 endpoints** (không phải 131 như phiên bản kiểm toán nháp trước đó).
- **Endpoint và Controller bị thiếu đã được bổ sung đầy đủ:**
  1. `SchoolClassController.java`: Bổ sung endpoint `GET /api/v2/classes/accessible-for-transcript` (STT 15) phục vụ tra cứu lớp học khả dụng xem bảng điểm theo thẩm quyền GVCN/Admin (Plan 062.1).
  2. `AcademicStatisticsController.java`: Bổ sung endpoint `GET /api/v2/academic/years/{academicYearId}/statistics` (STT 40) thống kê sĩ số và cảnh báo học vụ (Plan 053.2).
  3. `ClassTranscriptQueryController.java`: Bổ sung 2 endpoints `GET /api/v2/transcripts/classes/{classId}/semesters/{semesterId}` (STT 130) và `GET /api/v2/transcripts/classes/{classId}/academic-years/{academicYearId}` (STT 131) phục vụ 4 chế độ bảng điểm lớp (Plan 062.1).
- **Chuẩn hóa số thứ tự bảng phân quyền:** Đánh số lại toàn bộ 132 endpoints theo thứ tự tuần tự tuyệt đối từ **1 đến 132** và 25 nhóm La Mã (**I đến XXV**), loại bỏ triệt để các mã STT phân mảnh (`39.1`, `85.1`, `126.1`, `126.2`).
- **Chuẩn hóa đường dẫn mã nguồn:** Rà soát và chuyển đổi toàn bộ 132 đường dẫn liên kết Windows cũ (`file:///d:/CodeLearning/...`) sang đường dẫn chuẩn của môi trường workspace Linux hiện tại (`file:///home/duyptk/Coding/HoiNhapJava/Java-CoBan/...`).

### 2.2. Kiểm toán Giao diện Frontend (Vue 3 / PrimeVue)
- **Router & Shell Navigation:**
  - Đồng bộ bảng tuyến đường: Bổ sung `/v2/class-transcripts` (`ClassTranscriptViewerView.vue`) và `/v2/scorebooks/operations` (`CalculationOperationsView.vue`).
  - Phân tách rõ ràng hành vi hiển thị Sidebar trong [`AuthenticatedV2ShellView.vue`](file:///home/duyptk/Coding/HoiNhapJava/Java-CoBan/FE/src/views/AuthenticatedV2ShellView.vue):
    - `Bảng điểm` (`/v2/transcripts`): Chỉ hiển thị cho học sinh (`STUDENT`), ẩn hoàn toàn với nhân sự (`!isNonStudent`).
    - `Bảng điểm theo lớp` (`/v2/class-transcripts`): Hiển thị cho cán bộ, giáo viên (`ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`), ẩn với học sinh.
    - `Vận hành tính điểm` (`/v2/scorebooks/operations`): Hiển thị cho `ADMIN` và `ACADEMIC_OFFICE`, ẩn với giáo viên và học sinh.
- **Phản ánh các tính năng mới trong mục Chi tiết View:**
  - View 4 (`GradeListView.vue`): Hiển thị thống kê số lớp và tổng sĩ số từng khối (Plan 053.2).
  - View 5 (`SchoolClassListView.vue`): Hiển thị thống kê năm học và cảnh báo lệch sĩ số 20% (Plan 053.2); ghi nhận endpoint `accessible-for-transcript` phục vụ lọc lớp cho GVCN (Plan 062.1).
  - View 11 (`ScorebookWorkspaceView.vue`): Bổ sung khả năng nhập điểm khi `PUBLISHED` và vòng đời reopen (Plan 065).
  - View 13 (`TranscriptViewerView.vue`): Tích hợp tham số `studentId` phục vụ drill-down từ bảng điểm lớp và tích hợp dữ liệu chuyên cần học kỳ (Plan 061, 062.1, 066).
  - View 15 (`ClassTranscriptViewerView.vue`): Ghi nhận 4 chế độ bảng điểm lớp, phân quyền GVCN và Admin/Giáo vụ qua `TranscriptAccessGuard` (Plan 062.1).
  - View 16 (`CalculationOperationsView.vue`): Ghi nhận màn hình giám sát task tính toán, retry với xử lý 409 conflict, tra cứu trạng thái đồng bộ và nhật ký kiểm toán điểm bất biến (Plan 063).

### 2.3. Cập nhật Phân tích Khoảng cách (Gap & Drift Analysis)
- **Mục 4.1 (FE Permits / BE Blocks):**
  - Cập nhật mục 4: Ghi nhận phân quyền thực tế của `accessible-for-transcript` (GVCN xem lớp mình, Student bị 403).
  - Cập nhật mục 10: Vấn đề bấm vào menu Bảng điểm bị 403 Forbidden đã được giải quyết trọn vẹn từ Plan 061 & Plan 062.1 nhờ việc tách bạch 2 menu trên sidebar và hỗ trợ drill-down với `studentId`.
- **Mục 4.2 (BE Implements / FE Missing):**
  - Tách bạch rõ 2 nhóm:
    - **Nhóm đã giải quyết xong (Resolved):** Quản lý Background Calculation Task (Plan 063), Xem Score Audit Log (Plan 063), Tra cứu bảng điểm lớp và bảng điểm cá nhân cho GVCN/Staff kèm API lọc lớp `accessible-for-transcript` (Plan 061, 062.1).
    - **Nhóm khoảng cách thực tế còn lại (Real Remaining Gaps):** Tạo học sinh kèm tài khoản V3 (`StudentV3Controller`), Cấu hình ngày học trong lịch (`CalendarController`), Xóa năm học (`AcademicYearController`).
- **Mục 4.3 (Code Thực tế vs Baseline):**
  - Cập nhật mục 4: Đã hiện thực màn hình tra cứu bảng điểm lớp cho GVCN và Giáo vụ (`ClassTranscriptViewerView.vue`) và drill-down sang bảng điểm cá nhân.
- **Mục 5 (Technical Recommendations):**
  - Đánh dấu hoàn thành các khuyến nghị số 3 (Tách biệt màn hình bảng điểm) và số 5 (Giao diện giám sát Background Task & Audit Log).
  - Giữ lại và làm rõ các khuyến nghị cần thực hiện: Role Guard tại Router (phạm vi Plan 064), Ẩn/Disable nút bấm theo role, và điều chỉnh method security cho `StudentController.java` V1.

### 2.4. Đồng bộ Tài liệu FE Remaining Plans
- Bổ sung **Plan 066 (Student Attendance History by Student ID and Transcript Integration)** vào bảng "Các hạng mục đã hoàn thành, không còn là remaining plan" và phần "Trạng thái cần lưu ý" tại [`FE_REMAINING_PLANS-2026-09-02.md`](file:///home/duyptk/Coding/HoiNhapJava/Java-CoBan/document/dev-impl-plan/summary/FE_REMAINING_PLANS-2026-09-02.md).
- Khử trùng lặp đoạn ghi chú về Plan 053.2 trong phần "Trạng thái cần lưu ý".
- Sửa lỗi đứt đoạn văn bản trong phần "Definition of Done chung cho mỗi FE plan" tại [`FE_REMAINING_PLANS-2026-09-02.md`](file:///home/duyptk/Coding/HoiNhapJava/Java-CoBan/document/dev-impl-plan/summary/FE_REMAINING_PLANS-2026-09-02.md).
- Đồng bộ Plan 066 vào [`FE_DEV_PLAN_SUMMARY.md`](file:///home/duyptk/Coding/HoiNhapJava/Java-CoBan/document/dev-impl-plan/fe/FE_DEV_PLAN_SUMMARY.md).
- Xác nhận chỉ còn **duy nhất Plan 064 (FE E2E & Release Hardening)** ở trạng thái Draft chờ phê duyệt; toàn bộ các tính năng người dùng khác đã hoàn thành.

---

## 3. Danh sách Tệp Thay đổi (Files Changed)

| Đường dẫn tệp | Mục đích |
| ------------- | -------- |
| [`document/application-doc/v2/ActualPermissionMatrix.md`](file:///home/duyptk/Coding/HoiNhapJava/Java-CoBan/document/application-doc/v2/ActualPermissionMatrix.md) | Cập nhật ma trận phân quyền thực tế: 27 RestControllers (132 endpoints), đánh số thứ tự tuần tự 1..132, bổ sung `accessible-for-transcript`, router/sidebar v2, views mới, gap & drift analysis, chuẩn hóa 132 URL liên kết Linux. |
| [`document/dev-impl-plan/summary/FE_REMAINING_PLANS-2026-09-02.md`](file:///home/duyptk/Coding/HoiNhapJava/Java-CoBan/document/dev-impl-plan/summary/FE_REMAINING_PLANS-2026-09-02.md) | Bổ sung Plan 066, khử trùng lặp Plan 053.2, sửa đoạn text bị cụt tại Definition of Done và đồng bộ trạng thái các plan. |
| [`document/dev-impl-plan/fe/FE_DEV_PLAN_SUMMARY.md`](file:///home/duyptk/Coding/HoiNhapJava/Java-CoBan/document/dev-impl-plan/fe/FE_DEV_PLAN_SUMMARY.md) | Đồng bộ Plan 066 vào danh mục kế hoạch FE. |
| [`document/dev-note/be/workflow-skill/067-actual-permission-matrix-and-fe-remaining-plans-audit-2026-09-04.md`](file:///home/duyptk/Coding/HoiNhapJava/Java-CoBan/document/dev-note/be/workflow-skill/067-actual-permission-matrix-and-fe-remaining-plans-audit-2026-09-04.md) | Dev note ghi nhận toàn bộ quá trình kiểm toán, chỉnh lý và đồng bộ tài liệu. |
| [`document/dev-note/be/BE_DEV_NOTE_SUMMARY.md`](file:///home/duyptk/Coding/HoiNhapJava/Java-CoBan/document/dev-note/be/BE_DEV_NOTE_SUMMARY.md) | Bổ sung mục Dev Note 067 vào bảng tóm tắt BE. |
| [`document/dev-note/summary/DEV_NOTE_SUMMARY.md`](file:///home/duyptk/Coding/HoiNhapJava/Java-CoBan/document/dev-note/summary/DEV_NOTE_SUMMARY.md) | Bổ sung mục Dev Note 067 vào bảng tóm tắt tổng thể hệ thống. |

---

## 4. Quyết định Kỹ thuật Quan trọng (Implementation Decisions)

1. **Tuân thủ triệt để nguyên tắc không giả định:** Mọi thông tin ghi nhận trong tài liệu phân quyền đều được đối chiếu trực tiếp với mã nguồn Java (`@PreAuthorize`, Spring Security DSL, guard method) và mã nguồn Vue 3 (router path, `v-if` condition, session store roles).
2. **Loại bỏ liên kết Windows cục bộ & số thứ tự phân mảnh:** Đảm bảo 100% đường dẫn tham chiếu dạng `file://` trỏ đúng vào cấu trúc thư mục workspace Linux `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/...`, đánh số tuần tự nhất quán từ 1 đến 132 để tránh sót endpoint khi rà soát.
3. **Phân loại rõ ràng giữa Gap đã giải quyết và Gap tồn đọng:** Tránh tình trạng tài liệu lưu giữ các nhận định lỗi thời (như cho rằng FE chưa có màn hình giám sát task tính toán hay audit log, trong khi thực tế đã được xây dựng hoàn chỉnh trong Plan 063).

---

## 5. Kết quả Xác minh (Validation Results)

- **Kiểm toán đường dẫn legacy:** `grep -c "d:/CodeLearning" document/application-doc/v2/ActualPermissionMatrix.md` -> Kết quả: `0` (Đã chuyển đổi thành công 132/132 đường dẫn).
- **Kiểm tra thống kê controller & endpoint:** Rà soát và xác nhận đầy đủ 27/27 `@RestController` với 132/132 endpoints trong bảng ma trận.
- **Frontend Test Suite:** `npm --prefix FE run test -- --run` -> `75 passed (75 test files), 324 passed (324 tests)`.
- **Frontend Build:** `npm --prefix FE run build` -> `PASS` (0 error, build time 16.96s).
- **Backend Quality Gates Check:** Chạy `./gradlew checkstyleMain pmdMain`:
  - Checkstyle hoàn thành với 24 warnings (không có error chặn build).
  - PMD ghi nhận 5 vi phạm từ code Plan 062.1 (`SchoolClassService.java` và `ClassTranscriptQueryService.java`) cần được refactor làm sạch nợ kỹ thuật trong một đợt bảo trì backend riêng biệt.
- **Git diff status:** `git diff --stat` hiển thị thay đổi chuẩn xác trên các tệp tài liệu được yêu cầu.

---

## 6. Rủi ro Tồn đọng & Đề xuất Bước tiếp theo

1. **Rủi ro Route Guard FE:** Các tuyến đường FE `/v2/*` hiện chưa có role guard tại `router.beforeEach` mà dựa hoàn toàn vào việc ẩn/hiện menu trên Sidebar và lớp bảo vệ `@PreAuthorize` của Backend (sẽ trả về 403 nếu cố tình gõ URL). Đây là mục tiêu trọng tâm cần hoàn thiện trong **Plan 064 (FE E2E & Release Hardening)**.
2. **Rủi ro Method Security `StudentController.java`:** Endpoint V1 `POST`, `PUT`, `DELETE /api/v1/students` cần bổ sung `@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")` ở mức method để khóa quyền của tài khoản giáo viên theo đúng Requirement Baseline BR-AUTH-005.
3. **Nợ kỹ thuật PMD Backend:** Các service mới thêm trong Plan 062.1 (`ClassTranscriptQueryService.java` và `SchoolClassService.java`) có 5 vi phạm PMD (TooManyMethods, ExcessiveImports, CouplingBetweenObjects, ExcessiveParameterList, CyclomaticComplexity) cần được xử lý triệt để.

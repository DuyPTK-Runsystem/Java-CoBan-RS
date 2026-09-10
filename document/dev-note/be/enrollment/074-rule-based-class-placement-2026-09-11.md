# Dev Note 074 — Xếp lớp theo quy tắc (BE + FE)

## Liên kết và approval

- Developer Plan: `document/dev-impl-plan/be/enrollment/074-rule-based-class-placement-2026-09-10.md`.
- FE Developer Plan: `document/dev-impl-plan/fe/enrollment/074-rule-based-class-placement-ui-2026-09-10.md`.
- Wireframe: `document/wireframes/fe/enrollment/074-rule-based-class-placement/README.md`.
- Approval: P1–P6 và hướng triển khai Plan 074 được người dùng approve ngày `2026-09-10`; Plan 074.1 và G1–G3 được chốt cùng phiên.

Đây là Dev Note hợp nhất duy nhất của Plan 074, bao gồm phần backend placement và phần hoàn thiện toàn bộ giao diện sản xuất frontend (Plan 074 FE continuation).

## Phạm vi thực tế

### 1. Backend placement

- Thêm module placement v3 với session/candidate/result, lifecycle, class profile `ADVANCED`/`SUPPORT`/`REGULAR`, preview và confirm cho `ADMIN`/`ACADEMIC_OFFICE`.
- Auto-placement chỉ là hỗ trợ ban đầu: lớp nâng cao lấy top-N, lớp hỗ trợ lấy nhóm điểm thấp hơn trong capacity, lớp thường cân bằng phần còn lại; thiếu điểm/giới tính hoặc tie tại ngưỡng capacity được đánh dấu `MANUAL_REQUIRED`.
- Điểm của học sinh tiếp tục do backend lấy từ annual transcript chính thức của năm trước cho `CONTINUING`; không tin score/source do FE gửi. Candidate mới nhập học hoặc năm học đầu tiên không cần lịch sử lớp, nhưng cần target grade/evidence theo policy.
- Capacity của lớp đích được backend kiểm tra từ `SchoolClass`; automatic confirm bị chặn khi vượt capacity. Flow enrollment/transfer thủ công v2 giữ capacity warning non-blocking.
- `expectedVersion = 0` là hợp lệ cho phiên mới; request âm vẫn bị validation từ chối.
- Xác nhận dùng request riêng với idempotency key bắt buộc. Replay chỉ hợp lệ khi key thuộc chính phiên trên URL; dùng key của phiên khác trả `409`.
- `GET /placement-sessions/{id}` không tải toàn bộ kết quả. Kết quả chỉ được đọc qua `/results` theo `ResultPaginationDTO`; simulation giữ tập student đã xử lý trong bộ nhớ, không tải lại toàn bộ result cho từng candidate.
- Kết quả placement được trả theo `ResultPaginationDTO`.

### 2. Frontend placement production implementation

- Sửa lệch FE contract:
  - Cập nhật `PlacementSessionStatus`: loại bỏ `SIMULATING`, sử dụng chuẩn wire `DRAFT | SIMULATED | READY_FOR_CONFIRM | CONFIRMED | CANCELLED`.
  - Cập nhật `PlacementResult`: đổi `classId` thành `targetClassId: number | null`, chuẩn hóa `issueSeverity: 'WARNING' | 'BLOCKING'`.
  - Bổ sung hằng số chuẩn `PLACEMENT_RULE_VERSION = '074-v1'`.
- Production routes & Shell navigation:
  - Thêm 2 child routes dưới `/v2`:
    - `/v2/enrollments/placement/new` (`v2-placement-new`)
    - `/v2/enrollments/placement/:placementSessionId` (`v2-placement-session`)
    - Phân quyền nghiêm ngặt theo `allowedRoles: ['ADMIN', 'ACADEMIC_OFFICE']`. Chặn `TEACHER` và `STUDENT` điều hướng vào route này.
  - Cập nhật `AuthenticatedV2ShellView.vue` duy trì active state tab "Xếp lớp" khi ở các child routes của enrollment.
  - Cập nhật `EnrollmentListView.vue` thêm nút "Xếp lớp tự động" dẫn tới màn hình tạo phiên mới; giữ nguyên luồng xếp/chuyển lớp thủ công v2.
- Component & View orchestration:
  - `PlacementSessionSetup.vue`: Form cấu hình tạo phiên nháp gồm chọn năm học, khối, lọc lớp đích theo khối (loại trừ `CLOSED`), chọn profile từng lớp (`ADVANCED`, `SUPPORT`, `REGULAR`), hiển thị capacity từ `SchoolClass` (read-only), chọn học sinh chưa xếp lớp và phân loại nguồn (`CONTINUING`, `NEW_ADMISSION`, `REPEAT`).
    - **Refactor UI/UX danh sách học sinh chưa xếp lớp (Progressive Disclosure)**:
      - Loại bỏ 2 cột cố định `eligibilityEvidence` và `approvalReference` để giữ row chính gọn gàng (4 cột: Checkbox, Mã HS, Họ và tên, Dropdown Nguồn học sinh).
      - Áp dụng Progressive Disclosure bên dưới row học sinh thông qua PrimeVue DataTable row expansion (`v-model:expanded-rows`):
        - `CONTINUING`: hiển thị dòng trạng thái tinh gọn `"✓ Đủ điều kiện lên lớp theo kết quả năm học trước. Không cần bổ sung hồ sơ."`, không hiển thị input.
        - `NEW_ADMISSION`: hiển thị section nhập bổ sung với nhãn `"Căn cứ nhập học *"` (placeholder `"VD: Hồ sơ chuyển trường từ THCS ABC, Trúng tuyển đầu cấp..."`) và `"Mã hồ sơ / quyết định tiếp nhận *"` (placeholder `"VD: QĐ-124/THCS-2026, HS-TS-2026-0045..."`).
        - `REPEAT`: hiển thị section nhập bổ sung với nhãn `"Căn cứ học lại *"` (placeholder `"VD: Học bạ năm trước chưa đủ điều kiện lên lớp..."`) và `"Mã quyết định / biên bản *"` (placeholder `"VD: BB-HDXL-09/2026, QĐ-08/LƯU-BAN..."`).
      - Cập nhật caption section 3 thành: `"Học sinh nhập học mới hoặc học lại cần có căn cứ và thông tin phê duyệt trước khi xếp lớp."`.
      - Giữ nguyên validation bắt buộc căn cứ/phê duyệt cho học sinh mới hoặc học lại, giữ nguyên API payload (`CreatePlacementSessionRequest` với `score: null`, `scoreSourceReference: null`, `genderSnapshot: null`, `capacity: null`).
  - `PlacementWorkspaceReview.vue`: Nhận dữ liệu session và results trang hiện tại qua props độc lập; hỗ trợ PrimeVue paginator phân trang kết quả; map tên/mã học sinh từ cache và tên lớp đề xuất từ danh sách lớp đích; cảnh báo lỗi chặn sĩ số khi session `SIMULATED` hoặc có result `BLOCKING` và khóa nút xác nhận; hiển thị `MANUAL_REQUIRED` không khóa xác nhận; hỗ trợ sửa cách phân lớp ở trạng thái `DRAFT`; hiển thị trạng thái `CONFIRMED`/`CANCELLED` chỉ đọc.
  - `PlacementResultDetailDialog.vue`: Modal xem chi tiết giải thích, điểm dùng để xếp lớp, mã vấn đề và mức độ cảnh báo/chặn; cung cấp action điều hướng tiếp tục xếp thủ công tại v2 cho học sinh `MANUAL_REQUIRED`.
  - `PlacementWorkspaceView.vue`: Orchestration view xử lý route param, gọi API thật qua `placementApi`, quản lý cache thông tin học sinh (`getStudent`) cho trang hiện tại, xử lý phân trang zero-based, sinh idempotency key UUID cho mỗi confirm intent, chống double-submit và xử lý rõ `401`, `403`, `404`, `409`, network error. Mọi mutation `403` chuyển sang trạng thái không có quyền, giữ phiên đăng nhập.

## Files changed

### Backend placement

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/placement/**`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/common/contract/ResultPaginationDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/repository/AcademicYearRepository.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/repository/StudentAnnualTranscriptRepository.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/service/SchoolClassService.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/service/SchoolClassTranscriptAccessService.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/ClassTranscriptQueryService.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/ClassTermTranscriptReader.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/ClassAnnualTranscriptReader.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/ClassTranscriptRosterReader.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/ClassTranscriptScopeReader.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/ClassAnnualResultReader.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/TranscriptTermResponseMapper.java`
- `BE/BaiTap-RS/src/main/resources/db/migration/V21__create_placement_session_result_tables.sql`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/placement/service/PlacementServiceTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/placement/controller/PlacementRequestValidationTest.java`

### Frontend placement implementation & tests

- `FE/src/types/placement.ts`
- `FE/src/fixtures/placementFixture.ts`
- `FE/src/services/placementApi.ts`
- `FE/src/services/placementApi.spec.ts`
- `FE/src/components/enrollment/PlacementResultDetailDialog.vue`
- `FE/src/components/enrollment/PlacementResultDetailDialog.spec.ts`
- `FE/src/components/enrollment/PlacementSessionSetup.vue`
- `FE/src/components/enrollment/PlacementSessionSetup.spec.ts`
- `FE/src/components/enrollment/PlacementWorkspaceReview.vue`
- `FE/src/components/enrollment/PlacementWorkspaceReview.spec.ts`
- `FE/src/components/enrollment/PlacementWorkspaceReview.stories.ts`
- `FE/src/views/enrollment/PlacementWorkspaceView.vue`
- `FE/src/views/enrollment/PlacementWorkspaceView.spec.ts`
- `FE/src/views/enrollment/EnrollmentListView.vue`
- `FE/src/views/enrollment/EnrollmentListView.spec.ts`
- `FE/src/views/shell/AuthenticatedV2ShellView.vue`
- `FE/src/router/index.ts`
- `FE/src/router/index.spec.ts`

## Validation Result

### PASS

- Backend focused placement/gender regression tests: `23` tests passed, gồm version `0`/negative, idempotency replay/cross-session, pagination và clear gender nullable.
- Backend `./gradlew test`: `PASS`.
- Backend `./gradlew checkstyleMain`: task `PASS` (248 warnings hiện hữu/style).
- FE `npm run lint`: passed (0 errors, 0 warnings).
- FE `npm run test`: passed (94 test files, 521 tests green).
- FE `npm run test:coverage`: passed (85.22% statements; bao phủ service, view, component, router và regression tests mới).
- FE `npm run build`: passed (tạo production build thành công, chunk `PlacementWorkspaceView` hợp lệ).
- FE `npm run build-storybook`: passed (build tĩnh Storybook thành công vào `storybook-static`).
- `git diff --check`: passed (không có whitespace/newline vi phạm).

### FAIL / BLOCKED

- Backend `./gradlew -g /tmp/plan074-pmd-production pmdMain`: `PASS` với `0` violations. Đã tách quyền truy cập lớp, scope transcript, roster, kết quả annual và hai reader term/annual; các finding `TooManyMethods`, `ExcessiveImports`, `CouplingBetweenObjects`, `ExcessiveParameterList`, `CyclomaticComplexity` không còn.
- Backend `./gradlew -g /tmp/plan074-pmd-production build`: `FAIL` do `pmdTest` còn `123` violations trên test suite toàn repository; compile, bootJar, full test (403/403), JaCoCo và `pmdMain` vẫn chạy thành công nhưng không thay thế build gate.

### NOT RUN / UNVERIFIED

- Runtime integration validation: `NOT RUN`.
- Browser visual walkthrough, live API và role walkthrough trên môi trường runtime thật: `NOT RUN` (do không có backend runtime trực tiếp tương tác trong phiên agent này; Vitest/Storybook không thay thế browser validation).
- Migration execution and Flyway history check trên database thực tế: `NOT RUN`.

## Deviations and remaining risks

- Do Storybook mặc định cố gắng lưu telemetry vào thư mục ngoài workspace (`~/.storybook/settings.json`) và gửi qua mạng, trong môi trường sandbox không có mạng lệnh `storybook build` bị timeout. Giải pháp kiểm tra sử dụng config cục bộ trong workspace đã xác nhận `build-storybook` thành công hoàn toàn.
- Phần FE không gọi bất kỳ fixture runtime nào trong production code; tất cả tương tác thông qua `placementApi`, `academicApi`, `enrollmentApi`, `studentApi`.
- `PlacementService` được thu gọn còn `118` dòng và giữ transaction boundary; các trách nhiệm session/rules/scope/snapshot/simulation/allocation/confirmation/response được tách sang collaborator dưới `placement/service/support/`.
- `SchoolClassService` và `ClassTranscriptQueryService` tiếp tục giữ endpoint, DTO, transaction/authorization và query semantics; production PMD đã xanh. `pmdTest` vẫn chưa xanh do baseline và cảnh báo chất lượng trong test suite, nên không coi full backend build đã đạt.

## Next steps

- Triển khai kiểm thử tích hợp BE-FE trên môi trường staging/dev có live database.
- Thực hiện browser walkthrough theo kịch bản mục 12 của Plan 074 FE khi hệ thống backend khởi chạy.
- Xử lý PMD baseline ngoài placement và PMD test theo plan/scope riêng, rồi chạy lại build trước khi đánh dấu Plan 074 hoàn tất.

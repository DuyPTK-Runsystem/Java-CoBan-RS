# Developer Plan 047: BE Operational Closure

## 1. Trạng thái và phiên bản áp dụng

- **Status**: `Completed` — đã được người dùng phê duyệt qua agent ngày `2026-08-26` và triển khai hoàn tất.
- **Application-document version**: `v2` — theo yêu cầu prompt và tài liệu v2.
- **Ngày lập plan**: `2026-08-26`.
- **Module**: Backend `scorebook` / `audit` / `calculation` / operations.
- **Phụ thuộc**:
  - Plan 036 (Scorebook foundation), Plan 037 (Score entry), Plan 038 (Score change request);
  - Plan 039 (Semester lock), Plan 040 (Notification CR-SEM-001);
  - Plan 041, 042, 044, 045 (Calculation engine, worker, transcript aggregation, retake);
  - Plan 046 (Transcript query API).

## 2. Mục tiêu

Đóng các phần vận hành còn thiếu theo yêu cầu nghiệp vụ và tài liệu v2:

1. **API xem audit log điểm** (`FR-SCORE-007`, `NFR-AUDITABILITY-003..009`): Cho phép giáo vụ (`ADMIN`, `ACADEMIC_OFFICE`) và giáo viên tra cứu lịch sử audit log về nhập điểm, sửa điểm, duyệt sửa điểm, thi lại, trọng số môn kỹ năng và tác vụ tính toán.
2. **API xem calculation task lỗi** (`FR-CALC-007`, `NFR-CALC-011`, `NFR-CALC-012`): Tra cứu danh sách và chi tiết các calculation task có trạng thái `FAILED`, kèm thông tin lỗi gần nhất (`lastError`), số lần thử (`attemptCount`), và thời điểm phát sinh.
3. **API yêu cầu chạy lại task** (`FR-CALC-007`, `NFR-AUDITABILITY-009`, `NFR-RELIABILITY-001`): Cho phép giáo vụ kích hoạt chạy lại (retry) task lỗi đơn lẻ hoặc hàng loạt, ghi nhận audit log và đưa task về `PENDING` an toàn, idempotent.
4. **API xem trạng thái tính tổng kết** (`BR-CALC-001`, `BR-CALC-002`, `BR-SUMMARY-001..006`, `NFR-CALC-009`): API polling/tra cứu nhanh trạng thái tính toán (`IN_PROGRESS` | `FINISH`, `sourceVersion`, `calculatedVersion`, `isUpToDate`) cho học kỳ và năm học mà không phải tải toàn bộ cấu trúc điểm chi tiết.
5. **Kiểm tra ma trận quyền** (`ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`, `STUDENT`): Xây dựng bộ test phân quyền toàn diện kiểm chứng ma trận bảo mật mục 23 (`07-AccessQualityAndAcceptanceModule.md`) trên tất cả các endpoint nghiệp vụ.
6. **Hoàn thiện Postman flow cho các nghiệp vụ chính**: Cập nhật collection `Java-CoBan.postman_collection.json` với chuỗi kịch bản end-to-end hoàn chỉnh từ khởi tạo, xếp lớp, phân công, nhập điểm, sửa điểm, tính toán nền, thi lại, tra cứu bảng điểm, xem audit log đến khóa học kỳ.

## 3. Requirement và ràng buộc liên quan

- `FR-SCORE-007`: Xem audit log điểm.
- `FR-CALC-001` đến `FR-CALC-007`: Quản lý lifecycle calculation task, trạng thái `IN_PROGRESS`/`FINISH`, worker background, ghi nhận lỗi và yêu cầu chạy lại task lỗi.
- `BR-CALC-001` đến `BR-CALC-004`, `BR-SUMMARY-001` đến `BR-SUMMARY-006`: Quy tắc trạng thái bảng tổng kết, tính phụ thuộc, bảo vệ version và không coi `IN_PROGRESS` là kết quả chính thức.
- `NFR-PERFORMANCE-001`: Không tính điểm trung bình trong HTTP request.
- `NFR-RELIABILITY-001` đến `NFR-RELIABILITY-004`: Idempotency, lưu trữ bền vững, không đánh dấu `FINISH` khi lỗi, bảo vệ version.
- `NFR-SECURITY-003` đến `NFR-SECURITY-005`: Quyền theo phân công thực tế; trả `401`/`403` chuẩn xác.
- `NFR-AUDITABILITY-001` đến `NFR-AUDITABILITY-009`: Bắt buộc ghi audit log cho mọi thay đổi điểm, duyệt request, đổi trọng số, khóa kỳ, thi lại, và chạy lại calculation task.
- Ma trận quyền mục 23 (`07-AccessQualityAndAcceptanceModule.md`):
  - Giáo vụ (`ACADEMIC_OFFICE`, `ADMIN`): Toàn quyền quản lý sổ điểm, khóa kỳ, duyệt sửa điểm, thi lại, xem bảng điểm toàn trường, xem audit log, retry task.
  - Giáo viên chủ nhiệm (`GVCN`): Điểm danh lớp mình, xem bảng điểm lớp mình, theo dõi trạng thái học sinh lớp mình.
  - Giáo viên bộ môn (`GVBM`): Nhập điểm môn mình, cấu hình cột điểm môn mình, tạo request sửa điểm môn mình, xem bảng điểm môn/lớp được phân công dạy.
  - Học sinh (`STUDENT`): Chỉ xem thông tin, điểm số, chuyên cần và bảng điểm của chính mình qua `/me`. Tuyệt đối cấm thao tác ghi, quản trị, retry hoặc xem audit log.

## 4. Phạm vi

### 4.1. In-scope

1. **Score Audit Log API**:
   - `GET /api/v2/scorebooks/audit-logs`: Endpoint tra cứu audit log với bộ filter `ReqFilterScoreAuditLogDTO` (lọc theo `studentId`, `studentCode`, `entityType`, `entityId`, `action`, `actorUserId`, khoảng thời gian `fromOccurredAt`/`toOccurredAt`, phân trang `page`, `size`, sắp xếp `occurredAt` DESC).
   - Response DTO `ResScoreAuditLogDTO` trả thông tin audit rõ ràng (actor info, action, entity, before/after JSON parsed, timestamp).
   - Guard phân quyền: `ADMIN`, `ACADEMIC_OFFICE` xem toàn trường; `TEACHER` xem trong phạm vi phân công; `STUDENT` bị `403`.
2. **Failed Calculation Tasks API & Task Management**:
   - `GET /api/v2/scorebooks/calculation-tasks/failed` (hoặc filter `status=FAILED` chuẩn hóa qua `ReqFilterCalculationTaskDTO`): Trả danh sách task lỗi kèm phân trang, mã học sinh, năm học, số lần thử, lỗi chi tiết.
   - `POST /api/v2/scorebooks/calculation-tasks/{taskId}/retry`: Retry 1 task FAILED cụ thể.
   - `POST /api/v2/scorebooks/calculation-tasks/retry-all-failed`: Hỗ trợ retry hàng loạt tất cả task đang FAILED (tiện ích vận hành cho giáo vụ).
   - `POST /api/v2/students/{studentCode}/transcripts/recalculate` và `{studentId}`: Kích hoạt tính lại bảng điểm năm học cho học sinh.
3. **Transcript Calculation Status API**:
   - `GET /api/v2/transcripts/students/me/semesters/{semesterId}/status`
   - `GET /api/v2/transcripts/students/me/academic-years/{academicYearId}/status`
   - `GET /api/v2/transcripts/students/{studentId}/semesters/{semesterId}/status`
   - `GET /api/v2/transcripts/students/{studentId}/academic-years/{academicYearId}/status`
   - Response DTO `ResTranscriptCalculationStatusDTO`: trả `studentId`, `studentCode`, `academicYearId`/`semesterId`, `calculationStatus`, `sourceVersion`, `calculatedVersion`, `isUpToDate`, `calculatedAt`, `lastError`.
4. **Security Matrix Verification Suite**:
   - Xây dựng bộ test ma trận phân quyền tích hợp (`SecurityMatrixVerificationTest`) bao phủ cả 4 vai trò `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`, `STUDENT` đối với tất cả các nhóm API: Scorebook, Score Entry, Score Change Request, Semester Lock, Retake Exam, Calculation Tasks, Transcript Query, và Score Audit Logs.
5. **Hoàn thiện Postman Flow**:
   - Cập nhật và bổ sung các thư mục, request, pre-request script và test assertions trong `document/postman/Java-CoBan.postman_collection.json` theo đúng kỹ năng `postman-collection`.
   - Bao phủ trọn vẹn 10 luồng nghiệp vụ chính từ khởi tạo hệ thống đến vận hành đóng sổ điểm.

### 4.2. Out-of-scope

- Không thay đổi schema DB hay tạo Flyway migration mới (các bảng `audit_log`, `calculation_task`, `student_annual_transcript`, `student_term_transcript` đã có sẵn đầy đủ từ các plan trước).
- Không chuyển background calculation vào HTTP request.
- Không can thiệp vào nghiệp vụ chấm điểm hoặc công thức tính toán đã hoàn thành ở Plan 042, 044, 045.
- Không triển khai UI frontend (thuộc module FE).

## 5. Thiết kế đề xuất

### 5.1. API Contracts

#### A. Score Audit Log API
```http
GET /api/v2/scorebooks/audit-logs
Query params:
  - entityType: String (STUDENT_SCORE, SCOREBOOK, SCORE_CHANGE_REQUEST, RETAKE_EXAM, SKILL_WEIGHT_CONFIG, CALCULATION_TASK)
  - entityId: String
  - studentId: Long
  - studentCode: String
  - action: String
  - actorUserId: Long
  - fromDate: ISO-8601 DateTime
  - toDate: ISO-8601 DateTime
  - page: int (default 0)
  - size: int (default 10)
Authorization: ADMIN, ACADEMIC_OFFICE (toàn quyền); TEACHER (theo assignment scope).
```

#### B. Calculation Task API (Bổ sung & chuẩn hóa)
```http
GET /api/v2/scorebooks/calculation-tasks?status=FAILED&page=0&size=10
GET /api/v2/scorebooks/calculation-tasks/failed?page=0&size=10
POST /api/v2/scorebooks/calculation-tasks/{taskId}/retry
POST /api/v2/scorebooks/calculation-tasks/retry-all-failed
POST /api/v2/students/{studentCode}/transcripts/recalculate?academicYearId={id}
POST /api/v2/students/{studentId}/transcripts/recalculate?academicYearId={id}
Authorization: ADMIN, ACADEMIC_OFFICE.
```

#### C. Transcript Calculation Status API
```http
GET /api/v2/transcripts/students/me/semesters/{semesterId}/status
GET /api/v2/transcripts/students/me/academic-years/{academicYearId}/status
GET /api/v2/transcripts/students/{studentId}/semesters/{semesterId}/status
GET /api/v2/transcripts/students/{studentId}/academic-years/{academicYearId}/status
Authorization:
  - /me: STUDENT (bản thân)
  - /{studentId}: ADMIN, ACADEMIC_OFFICE (toàn quyền), TEACHER (theo phân công lớp/môn của học sinh).
```

### 5.2. Luồng xử lý và Security Matrix

```text
[HTTP Request]
       │
       ▼
[Security / JWT Filter] ──(401 Unauthorized nếu thiếu/sai token)
       │
       ▼
[Controller @PreAuthorize] ──(403 Forbidden nếu sai role cơ bản)
       │
       ▼
[Service / Access Guard] ──(403 Forbidden nếu TEACHER ngoài assignment scope / STUDENT truy cập id người khác)
       │
       ├── Tra cứu Audit Log ──> AuditLogRepository + AuditContext
       ├── Quản lý Calculation Task ──> CalculationTaskRepository + ScorebookAuditService
       └── Kiểm tra Status Tổng kết ──> Transcript State / Repository (Read-only)
```

**Bảng Ma trận Quyền Vận Hành:**

| Chức năng / Endpoint                          |     ADMIN      | ACADEMIC_OFFICE |   TEACHER (GVCN)    |   TEACHER (GVBM)    |       STUDENT       |
| --------------------------------------------- | :------------: | :-------------: | :-----------------: | :-----------------: | :-----------------: |
| `GET /scorebooks/audit-logs`                  | Cho phép (All) | Cho phép (All)  | Cho phép (Lớp mình) | Cho phép (Môn mình) |    403 Forbidden    |
| `GET /scorebooks/calculation-tasks`           |    Cho phép    |    Cho phép     |    403 Forbidden    |    403 Forbidden    |    403 Forbidden    |
| `POST /calculation-tasks/{id}/retry`          |    Cho phép    |    Cho phép     |    403 Forbidden    |    403 Forbidden    |    403 Forbidden    |
| `POST /calculation-tasks/retry-all-failed`    |    Cho phép    |    Cho phép     |    403 Forbidden    |    403 Forbidden    |    403 Forbidden    |
| `POST /students/{id}/transcripts/recalculate` |    Cho phép    |    Cho phép     |    403 Forbidden    |    403 Forbidden    |    403 Forbidden    |
| `GET /transcripts/students/me/.../status`     | 403 Forbidden  |  403 Forbidden  |    403 Forbidden    |    403 Forbidden    | Cho phép (Bản thân) |
| `GET /transcripts/students/{id}/.../status`   | Cho phép (All) | Cho phép (All)  | Cho phép (Lớp mình) | Cho phép (Lớp dạy)  |    403 Forbidden    |

### 5.3. Postman Collection Structure

Cập nhật `Java-CoBan.postman_collection.json` với các folder chính:
1. `01. Authentication & Users` (Admin, Academic Office, Teachers, Students)
2. `02. Academic Catalog & Structure` (Year, Semester, Grade, Class, Subject)
3. `03. Student & Enrollment` (Create Student with Account, Assign Class, Transfer Class)
4. `04. Teaching Assignments` (Homeroom Assignment, Subject Teaching Assignment)
5. `05. Scorebook & Assessment Columns` (Open Scorebook, Add Columns, Skill Weights)
6. `06. Score Entry & Correction Requests` (Bulk Score Entry, Direct Edit, Request Score Change, Approve/Reject)
7. `07. Calculation Tasks & Operations` (Query Tasks, Filter Failed, Retry Task, Recalculate Transcript)
8. `08. Retake Exams` (Create Retake Exam, Enter Retake Score)
9. `09. Transcript & Calculation Status` (Query Term/Annual Transcripts, Check Calculation Status)
10. `10. Score Audit Logs & Semester Lock` (Query Score Audit Logs, Semester Completeness, Lock Semester)

## 6. Phạm vi mã nguồn dự kiến

### 6.1. Tạo mới / Bổ sung DTOs & Services

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/controller/ScoreAuditLogController.java`: API tra cứu audit log điểm.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/ScoreAuditLogService.java`: Query, filter và mapping DTO cho audit logs.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/DTOs/requests/ReqFilterScoreAuditLogDTO.java`: Filter request cho audit log.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/DTOs/response/ResScoreAuditLogDTO.java`: Response DTO cho audit log.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/DTOs/response/ResTranscriptCalculationStatusDTO.java`: Response DTO cho trạng thái tính tổng kết.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/security/SecurityMatrixVerificationTest.java`: Bộ test kiểm tra ma trận quyền 4 roles.
- `document/postman/Java-CoBan.postman_collection.json`: Cập nhật hoàn thiện collection.

### 6.2. Chỉnh sửa tối thiểu

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/controller/CalculationTaskController.java`: Thêm endpoint `failed` và `retry-all-failed`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/CalculationTaskService.java`: Bổ sung method `retryAllFailedTasks()` và tối ưu filter.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/controller/TranscriptQueryController.java`: Thêm endpoints tra cứu status tính tổng kết (`/status`).
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/TranscriptQueryService.java`: Thêm logic đọc status nhẹ (lightweight status query).
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/common/audit/repository/AuditLogRepository.java`: Thêm query specification / filter methods nếu cần.

## 7. Kế hoạch test và validation

### 7.1. Unit & Integration Tests

- **Score Audit Log Tests**:
  - Giáo vụ tra cứu audit log có kết quả đúng theo filter `entityType`, `studentId`, `action`, khoảng thời gian.
  - Phân trang, sort DESC theo `occurredAt`.
  - Parse đúng `beforeData` và `afterData`.
  - Giáo viên chỉ xem audit log trong phạm vi phân công; học sinh bị `403`.
- **Calculation Task Operational Tests**:
  - Lọc danh sách task FAILED trả đúng các task có lỗi và thông tin `lastError`.
  - Retry 1 task FAILED: reset `attemptCount`, đổi status `PENDING`, cập nhật version, ghi audit log `CALCULATION_TASK_RETRIED`.
  - Retry task không phải FAILED bị `409 CONFLICT`.
  - Retry all failed tasks xử lý thành công tất cả task lỗi.
- **Transcript Calculation Status Tests**:
  - Tra cứu status trả đúng `IN_PROGRESS` khi `sourceVersion > calculatedVersion`.
  - Trả đúng `FINISH` và `isUpToDate = true` khi `sourceVersion == calculatedVersion`.
  - Endpoint `/me/.../status` hoạt động đúng cho học sinh đăng nhập; endpoint theo `{studentId}` kiểm tra đúng quyền giáo viên/giáo vụ.
- **Security Matrix Verification Tests**:
  - Test tự động chạy ma trận role: ADMIN (100% PASS), ACADEMIC_OFFICE (quản trị/vận hành PASS, `/me` của student bị 403), TEACHER (theo scope PASS, quản trị/retry/audit ngoài scope bị 403), STUDENT (chỉ `/me` PASS, toàn bộ endpoint khác bị 403).

### 7.2. Backend Validation Pipeline

Chạy theo quy trình skill `backend-validation`:
- `./gradlew clean test --no-daemon --console=plain`
- `./gradlew checkstyleMain pmdMain`
- `./gradlew build -x test -x pmdTest -x checkstyleTest --no-daemon --console=plain`
- Đọc kết quả JaCoCo coverage cho các component mới và sửa đổi.

## 8. Rủi ro và giải pháp

- **Rủi ro rò rỉ dữ liệu qua Audit Log**: Audit log có thể chứa dữ liệu nhạy cảm. Giới hạn role nghiêm ngặt, chỉ cho phép `ADMIN` và `ACADEMIC_OFFICE` xem toàn trường; giáo viên chỉ xem log của ô điểm thuộc môn/lớp mình; học sinh không được xem.
- **Rủi ro thắt cổ chai khi query JSON audit log**: Dùng paging bắt buộc với max size 50, sort DESC theo `occurredAt`, tận dụng index `(entity_type, entity_id, occurred_at)`.
- **Rủi ro race condition khi retry task**: Sử dụng optimistic locking / version check và cập nhật trạng thái trong `@Transactional`, bảo đảm idempotency key không bị trùng lặp.

## 9. Output dự kiến

1. Đầy đủ 4 API vận hành còn thiếu: Audit log điểm, Calculation task lỗi, Retry task (đơn lẻ & hàng loạt), Transcript calculation status.
2. Bộ test ma trận phân quyền `SecurityMatrixVerificationTest` xác nhận tính tuân thủ 100% của 4 role.
3. Postman collection `Java-CoBan.postman_collection.json` hoàn chỉnh bao phủ toàn bộ luồng nghiệp vụ chính.
4. Dev Note 047 ghi nhận chi tiết kết quả triển khai và validation.

## 10. Approval gate

Plan đã được User phê duyệt bằng tin nhắn qua agent ngày `2026-08-26`. Được phép triển khai đúng phạm vi đã nêu.

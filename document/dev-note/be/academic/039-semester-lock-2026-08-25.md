# Dev Note: Kế hoạch 039 - Triển khai Semester Lock Lifecycle, Service & Batch

- **Kế hoạch liên quan**: [`document/dev-impl-plan/be/academic/039-semester-lock-2026-08-25.md`](../../../../dev-impl-plan/be/academic/039-semester-lock-2026-08-25.md)
- **Trạng thái phê duyệt**: `Approved` (2026-08-25)
- **Ngày thực hiện**: 2026-08-25

---

## 1. Phạm vi thực tế hoàn thành

1. **Flyway Migration `V14__create_semester_lock_run_and_report.sql`**:
   - Thêm cột `reopen_until TIMESTAMP NULL` vào bảng `semester`.
   - Tạo bảng `semester_lock_run` lưu lịch sử các lần chạy lock batch.
   - Tạo bảng `semester_lock_report` lưu kết quả đánh giá mức độ hoàn thiện điểm (`summary_payload` JSON, chỉ số, checkpoint code, unique constraint `uk_lock_report_run_sem_chk`).
2. **Domain Model & Entities**:
   - Cập nhật entity `Semester`: thêm trường `reopenUntil` (`LocalDateTime`).
   - Tạo enums: `LockSource` (`MANUAL`, `AUTOMATIC`), `SemesterLockRunStatus` (`RUNNING`, `SUCCEEDED`, `FAILED`), `SemesterLockReportStatus` (`COMPLETE`, `INCOMPLETE`, `FAILED`).
   - Tạo entities `SemesterLockRun`, `SemesterLockReport`.
3. **Repository Layer**:
   - `SemesterRepository`: thêm pessimistic write lock `findByIdForUpdate`, `findAllByStatusIn`.
   - `SemesterLockRunRepository`, `SemesterLockReportRepository`.
   - `StudentYearEnrollmentRepository`: thêm `findByAcademicYearIdAndStatusOrderByStudentIdAsc`.
   - `ClassSubjectRepository`: thêm `findAllBySemesterIdAndStatus`, `findAllBySemesterId`.
   - `ScoreChangeRequestRepository`: thêm `countByAssessmentColumnIdInAndStatus`.
4. **Service & DTOs**:
   - `SemesterCompletenessService`: dịch vụ đánh giá tính đầy đủ dữ liệu điểm (sổ điểm published, cột KTĐK, cột KTCK, 3 cột môn kỹ năng, điểm từng học sinh, đơn sửa điểm pending), hỗ trợ preview, lưu idempotent báo cáo per run, cách ly lỗi exception khi lưu report.
   - `SemesterLockService`: dịch vụ khóa/mở học kỳ (`ACTIVE -> LOCKED`, `LOCKED -> ACTIVE` kèm hạn 3 ngày `reopenUntil`), kích hoạt `touchTranscripts` và `ensureRecalcTask` cho toàn bộ học sinh active trong năm học, ghi audit log `SEMESTER_LOCKED`, `SEMESTER_REOPENED`.
   - `SemesterService`: ủy quyền lock/reopen cho `SemesterLockService`, cập nhật `evaluateCompletenessCheckpoint` hỗ trợ 11 mốc thời gian từ CR-SEM-001 (`-45, -30, -14, -7, -3, -1, 0, 1, 3, 7, 14`).
   - `SemesterMapper`: bổ sung ánh xạ `reopenUntil`.
5. **Spring Batch & Scheduler**:
   - `SemesterLockTasklet`: thực thi quy trình batch xử lý các học kỳ, tự động lock tại checkpoint `t` (offset 0), đánh giá & lưu báo cáo completeness.
   - `SemesterLockBatchConfiguration`: cấu hình bean `semesterLockJob` và `semesterLockStep`.
   - `SemesterLockScheduler`: trigger tự động hàng ngày lúc `02:00` (`Asia/Ho_Chi_Minh`) và phương thức programmatic `runJobForDate(LocalDate)`.
6. **REST API Endpoint**:
   - `GET /api/v2/semesters/{semesterId}/completeness-report` (phân quyền `ADMIN`, `ACADEMIC_OFFICE`).
7. **Unit & Integration Tests**:
   - `SemesterLockServiceTest`: kiểm tra manual lock, auto lock idempotency, reopen window, validation conflict, audit logging, recalculation task enqueueing.
   - `SemesterCompletenessServiceTest`: kiểm tra các kịch bản đánh giá completeness, preview, lưu idempotent, đọc latest report.
   - `SemesterLockSchedulerAndBatchTest`: kiểm tra tasklet và scheduler launch.
   - `SemesterLockFlywayMigrationTest`: kiểm tra migration Flyway V14 trên H2.
   - `SemesterLockAuthorizationIntegrationTest`: kiểm tra phân quyền bảo vệ endpoints lock, reopen và completeness-report.
   - Cập nhật `SemesterStatusServiceTest` và `Plan027AcademicServiceTest`.

---

## 2. Danh sách file thay đổi

### Flyway Migration
- `BE/BaiTap-RS/src/main/resources/db/migration/V14__create_semester_lock_run_and_report.sql`

### Domain & DTOs
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/domain/entity/Semester.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/domain/entity/LockSource.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/domain/entity/SemesterLockRunStatus.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/domain/entity/SemesterLockReportStatus.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/domain/entity/SemesterLockRun.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/domain/entity/SemesterLockReport.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/domain/DTOs/response/SemesterCompletenessSummaryDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/domain/DTOs/response/ResSemesterCompletenessReportDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/domain/DTOs/response/ResSemesterDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/domain/DTOs/requests/ReqReopenSemesterDTO.java`

### Repositories
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/repository/SemesterLockRunRepository.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/repository/SemesterLockReportRepository.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/repository/SemesterRepository.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/repository/ClassSubjectRepository.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/enrollment/repository/StudentYearEnrollmentRepository.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/repository/ScoreChangeRequestRepository.java`

### Services & Controllers
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/service/SemesterMapper.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/service/SemesterLockService.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/service/SemesterCompletenessService.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/service/SemesterService.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/controller/SemesterController.java`

### Batch & Scheduler
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/batch/semesterlock/SemesterLockTasklet.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/batch/semesterlock/SemesterLockBatchConfiguration.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/batch/semesterlock/SemesterLockScheduler.java`

### Unit & Integration Tests
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/academic/service/SemesterLockServiceTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/academic/service/SemesterCompletenessServiceTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/batch/semesterlock/SemesterLockSchedulerAndBatchTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/config/SemesterLockFlywayMigrationTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/academic/controller/SemesterLockAuthorizationIntegrationTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/academic/service/SemesterStatusServiceTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/academic/service/Plan027AcademicServiceTest.java`

---

## 3. Quyết định kỹ thuật quan trọng

- **Spring Batch 6 Package Compatibility**: Sử dụng đúng gói lớp của Spring Batch 6 (`org.springframework.batch.core.job.Job`, `org.springframework.batch.core.step.Step`, `org.springframework.batch.infrastructure.repeat.RepeatStatus`).
- **Pessimistic Locking**: `SemesterRepository.findByIdForUpdate` đảm bảo không bị race condition khi thực hiện lock hoặc reopen đồng thời giữa scheduler và admin.
- **Idempotency**: Automatic lock bỏ qua và trả về dữ liệu nếu học kỳ đã ở trạng thái `LOCKED`. Báo cáo completeness cũng kiểm tra unique `(runId, semesterId, checkpointCode)` trước khi persist.
- **Error Isolation**: Nếu quá trình đánh giá tính đầy đủ dữ liệu gặp lỗi, bản ghi `SemesterLockReport` với trạng thái `FAILED` được lưu lại mà không rollback transaction khóa học kỳ.
- **Transcript Recalculation Enqueue**: Mỗi lần khóa hoặc mở lại học kỳ đều cập nhật phiên bản transcript và xếp hàng recalculation task cho tất cả học sinh đang học trong năm học tương ứng.

---

## 4. Kết quả Validation

| Lệnh kiểm tra                                         | Kết quả | Ghi chú                                                                                                                                                                |
| :---------------------------------------------------- | :-----: | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `./gradlew --no-daemon checkstyleMain checkstyleTest` | `PASS`  | 0 cảnh báo/lỗi, tuân thủ thứ tự import và độ dài dòng.                                                                                                                 |
| `./gradlew --no-daemon pmdMain pmdTest`               | `PASS`  | 0 vi phạm PMD.                                                                                                                                                         |
| `./gradlew --no-daemon test jacocoTestReport`         | `PASS`  | 100% test pass. `SemesterLockService`: 100% line coverage, `SemesterLockBatchConfiguration`: 100%, `SemesterCompletenessService`: 83.7%, `SemesterLockTasklet`: 83.6%. |
| `./gradlew --no-daemon build`                         | `PASS`  | Build và đóng gói ứng dụng thành công.                                                                                                                                 |

---

## 5. Sai lệch so với Kế hoạch

- Không có sai lệch về mặt nghiệp vụ hoặc kiến trúc so với Developer Plan 039.

---

## 6. Rủi ro còn lại & Bước tiếp theo

- Cấu hình Cron Scheduler (`0 0 2 * * *`) chạy tự động trên môi trường sản xuất theo múi giờ `Asia/Ho_Chi_Minh`.
- Sẵn sàng tích hợp frontend UI hiển thị báo cáo completeness và nút lock/reopen cho cán bộ quản lý đào tạo.


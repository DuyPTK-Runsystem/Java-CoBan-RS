# Application Context

## 1. Vai trò của tài liệu

Đây là file chính và entry point của bộ tài liệu ứng dụng. Chỉ đọc file này trước; mở tài liệu phụ theo module đang xử lý để giảm context và tránh nạp toàn bộ đặc tả vào mỗi task.

Nguồn nghiệp vụ cập nhật ngày 20/08/2026 được phân rã từ:

- Requirement Baseline v1.1.
- Data Structure v2.
- Code và contract hiện có trong repository.

Khi có mâu thuẫn, ưu tiên theo thứ tự:

1. Yêu cầu đã được chủ dự án phê duyệt.
2. Requirement module tương ứng.
3. Data model v2.
4. Contract/code hiện tại.
5. Suy luận của người triển khai.

Mục chưa chốt phải ghi rõ `TBD` hoặc `Needs confirmation`, không tự suy diễn thành contract.

## 2. Bản đồ tài liệu

### 2.1. Baseline nghiệp vụ

[`RequirementBaseline.md`](RequirementBaseline.md) là index của toàn bộ requirement baseline. Các requirement chi tiết nằm trong các file module bên dưới và giữ nguyên mã `FR-*`, `BR-*`, `NFR-*`, `AC-*`.

| Nhóm | Tài liệu |
|---|---|
| Quy tắc chung, auth và phạm vi | [`modules/00-CommonAndAuthModule.md`](modules/00-CommonAndAuthModule.md) |
| Khối, năm học, học kỳ, lớp | [`modules/01-AcademicStructureModule.md`](modules/01-AcademicStructureModule.md) |
| Xếp lớp, giáo viên, phân công, lịch học | [`modules/02-EnrollmentAndTeachingModule.md`](modules/02-EnrollmentAndTeachingModule.md) |
| Điểm danh và môn học | [`modules/03-AttendanceAndSubjectModule.md`](modules/03-AttendanceAndSubjectModule.md) |
| Sổ điểm, điểm thành phần, điểm trung bình | [`modules/04-AssessmentAndScoringModule.md`](modules/04-AssessmentAndScoringModule.md) |
| Sửa điểm, khóa kỳ, background calculation | [`modules/05-ScoreChangeAndCalculationModule.md`](modules/05-ScoreChangeAndCalculationModule.md) |
| Thi lại và bảng điểm tổng kết | [`modules/06-RetakeAndTranscriptModule.md`](modules/06-RetakeAndTranscriptModule.md) |
| Ma trận quyền, NFR, acceptance và DoD | [`modules/07-AccessQualityAndAcceptanceModule.md`](modules/07-AccessQualityAndAcceptanceModule.md) |

### 2.2. Mô hình dữ liệu

[`DataStructure.md`](DataStructure.md) là index của schema mục tiêu. Chi tiết schema được tách theo nhóm bảng trong thư mục [`data-model/`](data-model/).

| Nhóm bảng | Tài liệu |
|---|---|
| Nguyên tắc, schema cũ và migration | [`data-model/00-OverviewAndMigration.md`](data-model/00-OverviewAndMigration.md) |
| Tài khoản, role và giáo viên | [`data-model/01-IdentityAndAccess.md`](data-model/01-IdentityAndAccess.md) |
| Năm học, học kỳ, khối, lớp, môn | [`data-model/02-AcademicCatalog.md`](data-model/02-AcademicCatalog.md) |
| Học sinh, hồ sơ và xếp lớp | [`data-model/03-StudentsAndEnrollment.md`](data-model/03-StudentsAndEnrollment.md) |
| Phân công GVCN/GVBM | [`data-model/04-TeachingAssignments.md`](data-model/04-TeachingAssignments.md) |
| Sổ điểm và điểm học sinh | [`data-model/05-AssessmentAndScores.md`](data-model/05-AssessmentAndScores.md) |
| Request sửa điểm và điểm danh | [`data-model/06-ChangesAndAttendance.md`](data-model/06-ChangesAndAttendance.md) |
| Bảng điểm, thi lại và calculation task | [`data-model/07-ResultsAndCalculation.md`](data-model/07-ResultsAndCalculation.md) |
| Audit, FK, index và unique constraint | [`data-model/08-AuditAndConstraints.md`](data-model/08-AuditAndConstraints.md) |
| Migration, DDL skeleton và JPA mapping | [`data-model/09-MigrationAndJPA.md`](data-model/09-MigrationAndJPA.md) |

### 2.3. Contract của nền tảng hiện tại

- [`modules/UserModule.md`](modules/UserModule.md): auth contract hiện tại và hướng mở rộng identity v2.
- [`modules/StudentModule.md`](modules/StudentModule.md): CRUD student hiện tại và ranh giới với enrollment v2.
- [`ContractMigrationScopeFreeze.md`](ContractMigrationScopeFreeze.md): compatibility contract, migration foundation và các scope freeze đã chốt cho Plan 025.

## 3. Phạm vi hệ thống

Hệ thống là modular monolith gồm Spring Boot REST, Vue 3/PrimeVue, MySQL, Spring Data JPA và Spring Batch.

Phạm vi MVP mở rộng gồm:

- Tài khoản, role và phân quyền.
- Khối, năm học, học kỳ, lớp và môn học.
- Hồ sơ học sinh, xếp lớp và lịch sử chuyển lớp.
- Hồ sơ giáo viên và phân công GVCN/GVBM.
- Lịch học và điểm danh theo cơ chế ngoại lệ.
- Sổ điểm, điểm thành phần, điểm kỹ năng và các loại điểm trung bình.
- Sửa điểm, khóa học kỳ, background calculation, thi lại và bảng điểm tổng kết.
- Audit log và các yêu cầu phi chức năng.

Ngoài phạm vi MVP: phụ huynh, học phí, thư viện/tài sản, tuyển sinh, nhân sự-tiền lương, học bạ điện tử hoàn chỉnh và quy tắc xếp loại/xếp hạng/lên lớp chưa có CR riêng.

## 4. Quy tắc làm việc với tài liệu

- Đọc file chính rồi chỉ đọc module liên quan đến task.
- Không copy toàn bộ baseline vào prompt hoặc vào module khác.
- Không đổi mã requirement khi tách file.
- Không đưa `average_score` cũ thành nguồn điểm chính thức; điểm thành phần là nguồn, điểm trung bình là dữ liệu dẫn xuất.
- Không triển khai migration/schema mới khi các mục `TBD` chưa được chốt.
- Mọi thay đổi requirement phải được ghi nhận bằng Change Request và cập nhật đúng module.

## 5. Kiến trúc logic

```text
Vue 3 / PrimeVue
        |
        v
Spring Boot REST API
        |
        +--> feature modules: auth, academic, enrollment, teaching,
        |                    attendance, assessment, scoring, transcript
        |
        +--> Spring Data JPA --> MySQL
        |
        +--> Spring Batch --> background calculation / CSV export
```

Backend là nguồn validation và authorization cuối cùng. Frontend chỉ chịu trách nhiệm trải nghiệm, điều hướng, validation sớm và hiển thị trạng thái.

## 6. Các quyết định quan trọng đã chốt

- Access token và user summary được lưu trong `sessionStorage`; không lưu password hoặc password hash.
- `401 Unauthorized`: xóa auth state và về Login.
- `403 Forbidden`: giữ auth state và hiển thị không đủ quyền.
- Một giáo viên có thể đồng thời là GVCN và GVBM; quyền nhập điểm dựa trên `SUBJECT_TEACHING` assignment tương ứng.
- `student_info.student_id` là FK duy nhất để duy trì quan hệ một-một.
- Điểm trung bình được tính ở background worker; HTTP request chỉ lưu nguồn và tạo calculation task.
- Không xóa cứng dữ liệu đã phát sinh nghiệp vụ học tập.

## 7. Các điểm còn mở

- Migration tool chính thức: Flyway/Liquibase hay schema được quản lý theo cách khác.
- Chiến lược chuyển đổi dữ liệu từ schema ba bảng cũ sang schema v2.
- Chi tiết các quy tắc xếp loại, xếp hạng, thi lại theo điều kiện và lên lớp.
- Chính sách triển khai worker, retry và concurrency ở production.

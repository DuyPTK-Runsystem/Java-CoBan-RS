# Requirement Baseline

## Mục đích

Đây là file index của Requirement Baseline v1.1. Nội dung chi tiết đã được tách theo domain để mỗi task chỉ cần đọc module liên quan. Các mã requirement được giữ nguyên và không được đổi khi di chuyển giữa các file.

| Module | Mã chính | Nội dung |
|---|---|---|
| Common & Auth | `FR/BR-COMMON`, `FR/BR-AUTH`, `BR-TIME` | Phạm vi, tác nhân, thuật ngữ, trạng thái, lịch sử, phân quyền, thời gian |
| Academic Structure | `FR/BR-GRADE`, `FR/BR-AY`, `FR/BR-SEM`, `FR/BR-CLASS` | Khối, năm học, học kỳ, lớp và sĩ số |
| Enrollment & Teaching | `FR/BR-ENROLL`, `FR/BR-TEACHER`, `FR/BR-ASSIGN` | Xếp lớp, chuyển lớp, giáo viên và phân công |
| Attendance & Subject | `FR/BR-CALENDAR`, `FR/BR-ATTENDANCE`, `FR/BR-SUBJECT` | Lịch học, buổi học, điểm danh, môn học |
| Assessment & Scoring | `FR/BR-SCORE`, `FR/BR-SKILL`, `FR/BR-AVERAGE`, `FR/BR-CALC` | Điểm thành phần, môn kỹ năng, điểm trung bình |
| Score Change & Calculation | `FR/BR-SCORECHANGE`, `FR/BR-CALC` | Sửa điểm, khóa kỳ, tính nền, task và consistency |
| Retake & Transcript | `FR/BR-RETAKE`, `FR/BR-SUMMARY` | Thi lại và bảng điểm tổng kết |
| Access & Quality | `NFR-*`, `AC-*` | Ma trận quyền, performance, reliability, security, audit, usability, acceptance, DoD |

## Quy tắc đọc

1. Đọc [`ApplicationContext.md`](ApplicationContext.md).
2. Chọn đúng module bên dưới.
3. Nếu task liên quan bảng hoặc quan hệ dữ liệu, đọc thêm [`DataStructure.md`](DataStructure.md) và data-model tương ứng.
4. Nếu có thay đổi yêu cầu, cập nhật module chứa mã đó và ghi nhận CR.

## Các module chi tiết

- [`modules/00-CommonAndAuthModule.md`](modules/00-CommonAndAuthModule.md)
- [`modules/01-AcademicStructureModule.md`](modules/01-AcademicStructureModule.md)
- [`modules/02-EnrollmentAndTeachingModule.md`](modules/02-EnrollmentAndTeachingModule.md)
- [`modules/03-AttendanceAndSubjectModule.md`](modules/03-AttendanceAndSubjectModule.md)
- [`modules/04-AssessmentAndScoringModule.md`](modules/04-AssessmentAndScoringModule.md)
- [`modules/05-ScoreChangeAndCalculationModule.md`](modules/05-ScoreChangeAndCalculationModule.md)
- [`modules/06-RetakeAndTranscriptModule.md`](modules/06-RetakeAndTranscriptModule.md)
- [`modules/07-AccessQualityAndAcceptanceModule.md`](modules/07-AccessQualityAndAcceptanceModule.md)

## Trạng thái baseline

- Phiên bản: `1.1`.
- Ngày cập nhật nguồn: `20/08/2026`.
- Phạm vi tổ chức: một trường THCS.
- Kiến trúc: Spring Boot REST, Vue 3/PrimeVue, MySQL, Spring Data JPA, Spring Batch.
- Baseline là source of truth nghiệp vụ cho phần mở rộng; implementation hiện tại chỉ là contract nền đã tồn tại và phải được chuẩn hóa dần theo baseline được phê duyệt.


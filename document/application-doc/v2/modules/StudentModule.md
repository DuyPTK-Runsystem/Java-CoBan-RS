# Student Module

## Vai trò

File này mô tả contract CRUD student hiện tại. Requirement mở rộng về khối, lớp, xếp lớp và lịch sử học sinh nằm tại [`01-AcademicStructureModule.md`](01-AcademicStructureModule.md), [`02-EnrollmentAndTeachingModule.md`](02-EnrollmentAndTeachingModule.md); schema nằm tại [`../data-model/03-StudentsAndEnrollment.md`](../data-model/03-StudentsAndEnrollment.md).

## Contract hiện tại

```text
GET    /api/v1/students
GET    /api/v1/students/{studentId}
POST   /api/v1/students
PUT    /api/v1/students/{studentId}
DELETE /api/v1/students/{studentId}
POST   /api/v1/students/code
```

Quy ước hiện tại:

- Pagination server-side, mặc định 10 bản ghi/trang.
- Filter nhiều trường dùng AND.
- Sort chỉ nhận field thuộc allow-list.
- Student code dạng `STU` + 7 chữ số và phải unique.
- `student` và `student_info` được xử lý như một aggregate trong transaction.

## Ranh giới module

HTTP/controller không chứa query nghiệp vụ. Service điều phối aggregate, repository xử lý CRUD/search/page/sort. Frontend gọi API qua service, không rải HTTP call trong component markup.

Chi tiết requirement mở rộng: [`../RequirementBaseline.md`](../RequirementBaseline.md).


# v3 Data Model Boundary

Tài liệu này mô tả ownership và invariant ở mức logic; migration/JPA table name chỉ được
chốt trong plan tương ứng sau contract approval.

## Logical aggregates

| Aggregate | Dữ liệu chính | Invariant |
|---|---|---|
| PlacementSession | scope, rule version, criteria, status | confirm có version/audit; không phá lịch sử enrollment |
| PlacementResult | student, target class, score/explanation, warnings | tối đa một result active trong session |
| TimetableRevision | semester, slots, status, policy version | published revision không có blocking conflict |
| TeacherUnavailability | giáo viên, học kỳ, ngày/buổi/tiết, trạng thái/quyết định duyệt | đăng ký self; chỉ bản đã duyệt và có hiệu lực chặn TKB; duyệt bởi ADMIN/ACADEMIC_OFFICE |
| FunctionalRoom / SubjectFunctionalRoom | phòng chức năng và gán môn học | không có phòng thường; cardinality chờ Plan 075; giữ lịch sử tham chiếu |
| TeacherLoadPolicy | source, effective date, limits/exceptions | không có source thì không được coi là policy active |
| Notification/Receipt | audience, content, recipient/read state | recipient không vượt scope; idempotent |
| ScoreImportBatch/Row | target column, file, row validation, version | commit chỉ ghi target column |
| LessonLogEntry/Revision | lesson identity, evaluation, audit | không orphan/duplicate ngoài policy |

## Cross-cutting

- Mọi mutation nghiệp vụ quan trọng có actor/time/correlation và version khi cần.
- Foreign key/unique/index cụ thể là output của từng Developer Plan, không suy diễn từ
  bảng logic này.
- Dữ liệu cá nhân, điểm và nhận xét sổ đầu bài phải có scope/retention decision trước
  migration production.

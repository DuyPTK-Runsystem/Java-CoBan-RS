# Plan 079 BE — Sổ đầu bài: liên kết tiết học, vòng đời và kiểm toán

## 1. Trạng thái và nguồn

- Application-document version: **v3**; cập nhật review: **2026-09-14**.
- **DESIGN/CONTRACT APPROVED — D01–D10 đã được người dùng phê duyệt ngày 2026-09-14.**
- **Implementation approval BE/FE: APPROVED** theo xác nhận người dùng ngày 2026-09-14; D01–D10 đã được duyệt.
- Đi cùng [Plan FE](../../fe/lesson-log/079-lesson-log-ui-2026-09-14.md), [wireframe](../../../wireframes/fe/lesson-log/079-lesson-log/README.md), [Dev Note](../../../dev-note/summary/079-lesson-log-plan-wireframe-2026-09-14.md).
- Nguồn: [ApplicationContext](../../../application-doc/v3/ApplicationContext.md), [RequirementBaseline](../../../application-doc/v3/RequirementBaseline.md), [Module 05](../../../application-doc/v3/modules/05-LessonLog.md), [CR-V3-001](../../../application-doc/v3/change-request/CR-V3-001-academic-operations-and-targeted-communication.md), [data boundary](../../../application-doc/v3/data-model/README.md), [FE API boundary](../../../application-doc/v3/frontend-api/README.md), [Master Plan](../../summary/MASTER_PLAN_V3-2026-09-09.md).
- Requirement: `FR-V3-LESSON-001..003`, `BR-V3-LESSON-001`, `NFR-V3-001..002`, `AC-V3-001..002`.

Mục tiêu: ghi nội dung, tiến độ, nhận xét và sĩ số của một tiết học có thật; xem lịch sử theo lớp/giáo viên/ngày; chỉnh sửa có quyền, thời hạn và audit. Không cập nhật ngầm attendance, scorebook hoặc transcript.

## 2. Hiện trạng đã đối chiếu và phạm vi

- `V4__create_academic_structure_enrollment_and_audit.sql` dùng **`school_class(class_id)`**, không có bảng `class` cho FK của sổ đầu bài.
- `V5__create_semester_subject_teacher_assignment.sql` cung cấp `class_subject`, `subject_teaching_assignment`, `homeroom_assignment` và `semester`.
- `V22__create_timetable_and_teacher_load.sql` cung cấp head/revision/entry/period/calendar. Entry tham chiếu `assignment_id`, `period_id`; period có thứ, buổi, số tiết, giờ bắt đầu/kết thúc.
- `TimetableService` sao chép entry với ID mới khi tạo revision; `TimetablePublishService.closePreviousRevision` chuyển revision trước sang `ARCHIVED` và cắt `effectiveTo` trước ngày revision mới có hiệu lực. Chỉ tìm revision hiện đang `PUBLISHED` sẽ bỏ sót lịch sử.
- Chưa có module/migration lesson log trong source đã kiểm tra; chưa kiểm tra database live.

In-scope đề xuất: package `lessonlog`; policy có phiên bản; entry; audit; tổng kết tuần; lịch cá nhân, sổ tuần theo lớp; ghi/nháp/nộp/duyệt/điều chỉnh; guard tích hợp với publish/calendar của TKB để giữ lịch sử. File migration dự kiến `V23__create_lesson_log_and_audit.sql`; phải kiểm tra số migration và Flyway history trước khi triển khai, không coi V23 đã được giữ chỗ.

Out-of-scope đợt đầu đề xuất: học sinh/phụ huynh tra cứu; chữ ký số; in/PDF; duyệt hàng loạt; tạo tiết tự do; workflow phân công dạy thay riêng; tự sinh tiết bù; tự đồng bộ điểm danh/sổ điểm. Dạy thay/bù chỉ được phản ánh nếu TKB và assignment hợp lệ đã biểu diễn đúng tiết thực tế; không có ô chọn giáo viên tùy ý.

## 3. Quyết định đề xuất chờ duyệt

Các quyết định D01–D10 bên dưới đã được **APPROVED ở mức design/contract**. Wireframe minh họa contract đã duyệt; chưa phải bằng chứng module đã được triển khai hay kiểm chứng runtime.

| ID | Đề xuất | Điểm cần phê duyệt |
|---|---|---|
| D01 | Lesson identity ổn định `(class_id, lesson_date, session, period_index)`; giữ FK entry/revision/assignment nguồn và snapshot | Một lớp chỉ có một tiết tại một buổi/số tiết/ngày; không hỗ trợ nhiều nhóm học song song trong cùng lớp |
| D02 | GVBM xem/ghi các tiết của mình; GVCN xem cả lớp và ký tuần; ADMIN/ACADEMIC_OFFICE ngang quyền quản lý sổ đầu bài | Đây là đề xuất riêng cho `TBD-003` lesson log; không suy rộng approval của Plan 075 |
| D03 | `FIXED_HOURS` mặc định minh họa 48 giờ; lựa chọn thay thế `END_OF_WEEK`; timezone đề xuất `Asia/Ho_Chi_Minh` | Chọn đúng một mode; hours đề xuất 1–168, mặc định 48; chốt timezone và quy tắc ghi muộn tại mục 5 |
| D04 | Mã cố định A/B/C/D; nhãn Tốt/Khá/Trung bình/Yếu; mô tả rubric theo policy version | Không gọi là “chuẩn ngành”, không tự quy đổi điểm 9–10/7–8 khi chưa có nguồn |
| D05 | Sĩ số là đánh giá độc lập; snapshot tổng số HS theo enrollment tại ngày học; khi nộp có mặt + vắng = tổng | Không đọc attendance để tự điền; cần kiểm chứng khả năng truy xuất enrollment lịch sử trước code |
| D06 | Hoãn workflow dạy thay riêng; giáo viên được suy ra từ assignment của tiết, readonly | Nếu cần chỉ định người dạy thay ngoài TKB, phải có amendment riêng về nguồn duyệt/API/quyền |
| D07 | `expectedVersion` cho mọi cập nhật entry, ký tuần và policy; khóa chung phạm vi tuần và TKB khi cần | Chốt giao thức tại mục 7, tránh chỉ so version ở FE |
| D08 | Audit append-only, FK `RESTRICT`, không API xóa; mutation và audit cùng transaction | Retention và quyền truy cập lịch sử tại D10; không hứa lưu vĩnh viễn |
| D09 | Chỉ ký tuần đã kết thúc, còn ít nhất một tiết và không còn thiếu/nháp; sửa sau ký làm `STALE`, cần ký lại | Ký tuần không đồng nghĩa duyệt từng tiết và không khóa thay edit window |
| D10 | Không có purge trong đợt đầu; phạm vi đọc theo D02 | Thời hạn giữ dữ liệu, xử lý dữ liệu cá nhân/ghi chú HS và quyền sau thay GVCN vẫn cần chốt trước production |

## 4. Định danh và bảo toàn lịch sử TKB

1. Server nhận `timetableEntryId` + `lessonDate`; tự suy ra semester/class/subject/assignment/teacher/buổi/số tiết. Không nhận các giá trị identity còn lại từ client.
2. Kiểm tra ngày trong semester, trong khoảng hiệu lực revision và entry (hai đầu inclusive), đúng thứ của period, không thuộc ngày nghỉ; assignment phù hợp lớp/môn/ngày. Chỉ nhận revision có bằng chứng đã publish (`PUBLISHED` hoặc `ARCHIVED` đã publish trong audit) và có hiệu lực ở ngày học. Nếu có nhiều revision cùng hiệu lực, trả `409`, không chọn bản mới nhất tùy ý.
3. Unique theo D01 bảo vệ cả race tạo mới và entry ID mới qua revision. Unique phụ `(timetable_entry_id, lesson_date)` có thể giữ để tra cứu nhưng không thay D01.
4. Entry giữ nguyên FK nguồn; snapshot gồm tên lớp/môn/GV, ngày/buổi/tiết, giờ bắt đầu/kết thúc, assignment ID, revision ID, policy ID, deadline và sĩ số. Khi xem lịch sử dùng snapshot; không tính lại từ tên/giờ/policy hiện tại.
5. Lịch ngày/tuần resolve revision theo từng ngày, rồi ghép entry theo identity ổn định. Trạng thái `UNLOGGED` là ô TKB chưa có entry, không phải bản ghi DB. Ô không có lịch không mở form tạo tự do.
6. **Guard tích hợp bắt buộc**: publish không được hồi tố làm mất/đổi nguồn tiết đã có sổ (kể cả draft), đổi calendar/giờ/ngày nghỉ không được làm sai occurrence đã ghi hoặc tuần đã ký. Đề xuất chỉ publish hiệu lực từ ngày tương lai chưa có sổ, đồng thời không cắt mất các ngày đã có sổ/tuần ký; nếu vi phạm trả `409` với thông báo ngày bị ảnh hưởng. Không remap, xóa hay ghi đè lịch sử tự động. Đây là thay đổi dự kiến trong timetable service, chưa được triển khai trong task tài liệu.
7. Khi cập nhật entry cũ, kiểm tra quyền từ nguồn/snapshot lịch sử, trạng thái và deadline đã lưu; không yêu cầu nguồn vẫn đang `PUBLISHED`. Khi assignment/calendar lịch sử không đủ dữ liệu để xác minh, chặn ghi và báo lỗi rõ; không suy diễn từ assignment hiện tại.

## 5. Policy, validation và vòng đời

### 5.1 Policy theo ngày học

- Policy immutable theo phiên bản, hiệu lực từ ngày, không cho hồi tố. `PUT /policy` tạo version mới sau khi so `expectedVersion` của version mới nhất; serialize activation để không có hai bản cùng hiệu lực. GET chọn bản có `effectiveFrom` lớn nhất không vượt ngày học.
- Hai mode loại trừ nhau: `FIXED_HOURS`: `expiresAt = lessonEndsAt + editWindowHours`; `END_OF_WEEK`: hết hạn tại **00:00 thứ Hai kế tiếp**, exclusive, theo timezone policy. Không dùng boolean phối hợp hai deadline.
- `lessonEndsAt <= now < expiresAt`: giáo viên được tạo/nháp/nộp/sửa. Đúng `expiresAt` là hết hạn. Tiết tương lai hoặc đang diễn ra: chỉ xem. Nháp không được miễn hạn.
- Week là thứ Hai–Chủ nhật; `weekStart` luôn là thứ Hai, ngày nằm ngoài semester không có tiết. Ngày học hiển thị theo calendar, 2 buổi × 4 tiết; ngày nghỉ/không lịch phải phân biệt với chưa ghi.
- Policy mới chỉ có hiệu lực từ một ngày tương lai, không thay đổi deadline/rubric của tiết cũ, kể cả tiết chưa ghi. Response trả policy version và deadline cụ thể; FE không hard-code 48h.

### 5.2 Field bắt buộc

- Draft: cho phép title/content/count/grade chưa điền (NULL), nhưng field đã điền phải đúng type, enum, độ dài và count nguyên không âm. Không mặc định grade A hoặc count 0 để che thiếu dữ liệu.
- Submit, amend hoặc ghi bổ sung bởi quản lý: title 1–255 ký tự sau trim, completionStatus trong `ON_SCHEDULE/BEHIND_SCHEDULE/AHEAD_OF_SCHEDULE`, grade A/B/C/D, presentCount/absentCount nguyên không âm và tổng bằng `rosterCountSnapshot`. Content/comments tối đa 4000 ký tự; absentStudentNotes/homework/reason tối đa 500. Reason bắt buộc không trắng khi amend/ghi bổ sung/thay policy/ký lại.
- Nếu không resolve được sĩ số lịch sử thì chặn submit với `422`; không lấy sĩ số hôm nay thay thế. Không đồng bộ ngầm attendance.

### 5.3 Ma trận chuyển trạng thái đề xuất

| Thao tác | Actor | Từ → Đến | Điều kiện |
|---|---|---|---|
| Create | GVBM được phân công | Chưa có → DRAFT | Đúng nguồn tiết; đã kết thúc và còn hạn |
| Update | GVBM được phân công | DRAFT → DRAFT; SUBMITTED → SUBMITTED | Còn hạn; expectedVersion; không cho hạ SUBMITTED về DRAFT |
| Submit | GVBM được phân công | DRAFT → SUBMITTED | Còn hạn; đủ field; expectedVersion |
| Review | ADMIN/ACADEMIC_OFFICE | SUBMITTED hoặc AMENDED → REVIEWED | expectedVersion; chưa duyệt đúng version này |
| Amend | ADMIN/ACADEMIC_OFFICE | SUBMITTED/REVIEWED/AMENDED → AMENDED | Đủ field, expectedVersion, amendReason; clear reviewedAt/reviewedBy, giữ thông tin cũ trong audit |
| Complete overdue draft | ADMIN/ACADEMIC_OFFICE | DRAFT → AMENDED | Chỉ sau deadline; đủ field, expectedVersion, amendReason |
| Record missing lesson | ADMIN/ACADEMIC_OFFICE | Chưa có → AMENDED | Chỉ sau deadline; nguồn TKB hợp lệ; đủ field, reason; unique/race guard |

Quản lý không dùng PUT thường để né reason. GVCN không được review/amend từng tiết chỉ nhờ quyền chủ nhiệm; nếu đồng thời là GVBM thì quyền ghi áp dụng riêng cho tiết được phân công. AMENDED readonly với giáo viên, kể cả còn hạn. Không DELETE hoặc mở lại nháp trong đợt đầu. Học kỳ đóng: vẫn đọc; mọi mutation nội dung/ký tuần bị `422`, cần quy trình mở học kỳ hiện hữu nếu muốn bổ sung.

## 6. Schema logic dự kiến (không phải migration đã chạy)

Tất cả FK dùng đúng tên/cột schema hiện tại; không `ON DELETE CASCADE` cho lịch sử. Tất cả thời điểm lưu UTC, ngày học là local date theo policy; `created_by/actor_id` trỏ `app_user(user_id)`, không nhầm teacher ID và user ID.

| Bảng | Cột/ràng buộc chính |
|---|---|
| `lesson_log_policy` | `policy_id` PK, `policy_version` unique, `effective_from` unique, `timezone`, `deadline_mode`, `edit_window_hours` nullable theo mode, `require_homeroom_review`, `rubric_json` (đúng bốn mã/nhãn/mô tả), `version` BIGINT, actor/time. Version cũ bất biến; transaction lock cấu hình khi tạo version mới |
| `lesson_log_entry` | `entry_id` PK; FK `timetable_entry_id → timetable_entry(entry_id)`, `timetable_revision_id → timetable_revision(revision_id)`, `assignment_id → subject_teaching_assignment(assignment_id)`, `semester_id → semester(semester_id)`, **`class_id → school_class(class_id)`**, `subject_id → subject(subject_id)`, `assigned_teacher_id → teacher(teacher_id)`, `policy_id → lesson_log_policy(policy_id)`; `lesson_date`, `session`, `period_index`; unique `(class_id, lesson_date, session, period_index)`; `source_snapshot_json`, `roster_count_snapshot`, `lesson_ends_at`, `edit_window_expires_at`; các field tại 5.2; status, submitted/reviewed actor/time, `version BIGINT DEFAULT 0` |
| `lesson_log_weekly_review` | `review_id` PK; FK **`class_id → school_class(class_id)`**, semester, `homeroom_assignment_id → homeroom_assignment(assignment_id)` nullable khi chưa ký; unique `(class_id, semester_id, week_start)`; status `UNSIGNED/SIGNED/STALE`, weekly_comment/grade nullable trước ký, `signed_snapshot_json`, signed actor/time, `version BIGINT DEFAULT 0`. Khởi tạo khi occurrence đầu tiên của tuần bị ghi; dùng làm khóa chung cho mutation tuần |
| `lesson_log_revision` | `revision_id` PK; đúng một FK trong `entry_id`, `weekly_review_id`, `policy_id`; CHECK exactly-one-target; action, actor FK, correlationId, reason nullable cho thao tác thường, before_state_json nullable chỉ khi tạo, after_state_json NOT NULL, created_at. FK `RESTRICT`, append-only; lưu CREATE/UPDATE/SUBMIT/REVIEW/AMEND/LATE_RECORD/SIGN_WEEK/INVALIDATE_WEEK/POLICY_CREATE |

Các CHECK: session MORNING/AFTERNOON, period_index 1..4; count không âm; enum hợp lệ; required-field theo trạng thái; mode/hours tương thích. Guard xác minh FK dư thừa đều cùng nguồn entry; FK riêng lẻ không chứng minh class/teacher/subject khớp nhau. Index truy vấn `(class_id, lesson_date)`, `(assigned_teacher_id, lesson_date)`, `(status, lesson_date)`, audit `(entry_id, created_at, revision_id)` và tương tự cho tuần/policy. H2 không thay kiểm tra dialect MySQL thật.

## 7. Ký tuần, concurrency và audit

- Chỉ GVCN có assignment hiệu lực cho lớp tại ngày kết thúc phần tuần nằm trong semester và còn quyền tại thời điểm ký được ký tuần. Quyền đọc sau thay GVCN còn mở tại D10; không tự cấp quyền lịch sử toàn lớp cho mọi giáo viên.
- `canSignWeek` chỉ true khi tuần đã kết thúc, học kỳ mở, có tiết, không còn `UNLOGGED/DRAFT`; SUBMITTED/REVIEWED/AMENDED đều đủ điều kiện. `requireHomeroomReview=false` chỉ bỏ bắt buộc, không tự tạo chữ ký. Nếu policy thay đổi giữa tuần, tuần bắt buộc ký khi ít nhất một occurrence áp dụng policy yêu cầu ký; phiên policy của từng occurrence được giữ trong snapshot.
- GET tuần trả danh sách mọi occurrence dự kiến, tổng thiếu/nháp, version tuần, trạng thái chữ ký và snapshot phiên ký. Bộ lọc FE không thay tập dùng để ký/tính điều kiện.
- POST ký yêu cầu `expectedVersion` tuần và `expectedEntries: [{entryId, version}]` của toàn bộ tập vừa xem. Tập thiếu/thừa hoặc version thay đổi → `409`. Ký lần đầu lưu snapshot toàn tập + nhận xét/rubric + actor/time; ký lại khi STALE bắt buộc reason và lưu snapshot mới trong audit, giữ phiên cũ.
- Mọi mutation entry làm tăng version tuần; nếu SIGNED thì chuyển STALE và ghi `INVALIDATE_WEEK` trong cùng transaction. Review từng tiết cũng thay version và cần ký lại theo đề xuất bảo thủ này. Không tự xóa chữ ký/snapshot cũ.
- Thứ tự khóa thống nhất: timetable head → hàng tuần → entry; serialize tạo hàng tuần bằng unique key và retry transaction nội bộ có giới hạn khi cạnh tranh tạo hàng, không retry mutation nghiệp vụ mù. Publish/calendar guard dùng cùng khóa timetable head, nên kiểm tra rồi ghi không bị race với tạo sổ. Policy activation được serialize riêng trong service và không đổi quá khứ.
- `@Version` thực thi optimistic lock; kiểm tra `expectedVersion` trước mutation và xử lý lỗi flush/commit thành `409`. CREATE cạnh tranh cùng identity chỉ một request thành công. Transaction rollback không để entry thiếu audit hoặc có audit cho mutation thất bại. Audit không có API UPDATE/DELETE; ứng dụng không cascade xóa nguồn.

## 8. Contract checkpoint BE/FE

Giữ `RestResponse<T>`; paged list dùng `ResultPaginationDTO` (`meta.page` zero-based). Schedule một ngày và ma trận một tuần là DTO bounded, không ép pagination. Không tạo v3 error code/typed error payload riêng; HTTP status + envelope/message hiện có.

### 8.1 Endpoints

| Method / URI (prefix `/api/v3/lesson-logs`) | Request chính | Response / quyền |
|---|---|---|
| GET `/my-schedule?date=YYYY-MM-DD` | Một ngày | `TeacherDailyScheduleResponse`; TEACHER theo assignment |
| GET `/classes?semesterId=...` | Lookup bounded theo học kỳ | Danh sách lớp trong scope; GVCN/manager; GVBM không có quyền xem toàn lớp chỉ nhờ dạy một môn |
| GET `/class/:classId?semesterId=...&weekStart=YYYY-MM-DD` | weekStart thứ Hai | `ClassWeeklyLessonLogResponse`; GVCN/manager |
| GET `/entries/:entryId` | ID | `LessonLogEntryResponse`; GVBM của tiết/GVCN của lớp/manager |
| GET `/entries/:entryId/revisions?page=0&pageSize=20` | Page | `ResultPaginationDTO`; cùng scope đọc entry |
| POST `/entries` | `{timetableEntryId, lessonDate, ...draftFields}` | 201 entry DRAFT; chỉ GVBM |
| PUT `/entries/:entryId` | `{expectedVersion, ...editableFields}` | 200 entry; chỉ GVBM theo mục 5 |
| POST `/entries/:entryId/submit` | `{expectedVersion}` | 200 entry; GVBM |
| POST `/entries/:entryId/review` | `{expectedVersion, reviewComment?}` | 200 entry; manager |
| POST `/entries/:entryId/amend` | `{expectedVersion, amendReason, ...completeFields}` | 200 entry; manager |
| POST `/entries/late-record` | `{timetableEntryId, lessonDate, reason, ...completeFields}` | 201 AMENDED; manager sau hạn |
| POST `/class/:classId/weekly-review` | `{semesterId, weekStart, expectedVersion, expectedEntries, weeklyComment, weeklyGrade, reason?}` | 200 `WeeklyReviewResponse`; GVCN |
| GET `/class/:classId/weekly-review/revisions?semesterId=...&weekStart=...&page=0&pageSize=20` | Scope tuần | Audit paged; GVCN/manager |
| GET `/policy?lessonDate=YYYY-MM-DD` | Bỏ ngày để xem version mới nhất trong settings | `LessonLogPolicyResponse`; manager; snapshot rubric/deadline cần cho GV nằm trong schedule/entry |
| PUT `/policy` | `{expectedVersion, effectiveFrom, timezone, deadlineMode, editWindowHours, requireHomeroomReview, rubric, reason}` | 200 policy version mới; manager |
| GET `/policy/revisions?page=0&pageSize=20` | Page | Audit paged; manager |

Manager = ADMIN hoặc ACADEMIC_OFFICE theo D02 đang chờ duyệt. Không nhận `actualTeacherId`, `status`, class/subject hoặc identity sửa được trong request. Create luôn DRAFT; FE muốn “Nộp” thì lưu/create thành công rồi submit version trả về; nếu bước submit lỗi, báo “Đã lưu nháp, chưa nộp” và giữ draft ID để không tạo trùng.

### 8.2 Shape tối thiểu dùng chung

- `ScheduleItem`: identity nguồn, localDate, class/subject/teacher names, session/periodIndex, lessonEndsAt, functionalRoomName nullable, rosterCountSnapshot, policy snapshot, entry nullable, `canCreate`, `canLateRecord`, `blockedReason`. Không có phòng thường.
- `LessonLogEntryResponse`: entryId, timetableEntryId, timetableRevisionId, assignmentId, lessonDate, classId/subjectId/assignedTeacherId + snapshot names, session/periodIndex, tất cả evaluation fields, status, submittedAt/by, reviewedAt/by/comment, version, policyId, rubric, rosterCountSnapshot, editWindowExpiresAt; `canTeacherEdit/canSubmit/canReview/canAmend`, blockedReason. Capabilities do BE tính cho actor, không phải quyền do FE tự tạo.
- `TeacherDailyScheduleResponse`: date, timezone, items; chỉ tiết của actor, gồm cả chưa ghi. Lịch tương lai chỉ xem.
- `ClassWeeklyLessonLogResponse`: classId, semesterId, weekStart, weekEnd, calendarDays (thứ Hai–Chủ nhật, học/nghỉ/ngoài kỳ), items, summary (scheduled/unlogged/draft/submitted/reviewed/amended và grade counts của bản đã nộp trở lên), weeklyReview.
- `WeeklyReviewResponse`: reviewId nullable trước khi có hàng, version (0 cho tuần chưa có hàng, canSign=false), status, comment/grade, signedAt/by, signedSnapshot, expectedEntries, canSignWeek, blockedReasons. Snapshot cũ vẫn xem được khi STALE.
- `LessonLogPolicyResponse`: policyId, policyVersion, version, effectiveFrom, timezone, deadlineMode, editWindowHours nullable, requireHomeroomReview, rubric gồm `{code,label,description}`.
- Ví dụ: tiết kết thúc 15/09/2026 07:45 tại Asia/Ho_Chi_Minh → `lessonEndsAt=2026-09-15T00:45:00Z`; 48 giờ → `editWindowExpiresAt=2026-09-17T00:45:00Z`.

### 8.3 HTTP errors cố định

| Status | Trường hợp |
|---|---|
| 400 | JSON/type/enum/độ dài/date format/thiếu expectedVersion sai contract |
| 401 | Chưa xác thực; apiClient xóa session/redirect theo cơ chế hiện tại |
| 403 | Sai role hoặc ngoài assignment/class scope; giữ session |
| 404 | ID không tồn tại, sau khi kiểm tra quyền scope cần thiết |
| 409 | Duplicate identity, stale version/tập entry ký tuần, nhiều revision hiệu lực, thay TKB đụng lịch sử |
| 422 | Hết hạn/chưa đến giờ ghi/học kỳ đóng, nguồn chưa publish/ngày không có tiết, chuyển trạng thái không hợp lệ, sĩ số chưa đủ hoặc không khớp, tuần chưa đủ điều kiện ký |

FE giữ input khi 409/422; reload dữ liệu mới vào vùng so sánh trước khi người dùng chọn thay nội dung đang gõ. Không retry mutation tự động; lỗi 422 hiển thị message/capability mới từ GET, không suy diễn mọi 422 đều là hết hạn.

## 9. File dự kiến và delivery

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/lessonlog/`: controller Entry/Query/Policy/WeeklyReview; service tương ứng + Guard/Audit; repository Policy/Entry/WeeklyReview/Revision; entity/DTOs requests/response/enums theo contract trên.
- `BE/BaiTap-RS/src/main/resources/db/migration/V23__create_lesson_log_and_audit.sql` (số provisional).
- `timetable/service/TimetablePublishService.java`, `TimetableCalendarService.java` và guard/repository liên quan: chặn thay lịch ảnh hưởng dữ liệu sổ đầu bài, dùng cùng khóa transaction. Đây là dependency của D01, không được bỏ khỏi estimate/approval.
- Tests trong package lessonlog và regression timetable. Khả năng lịch sử enrollment/assignment cần kiểm chứng; nếu thiếu phải đề xuất amendment, không sửa ngầm v2.
- Sau contract approval: BE schema/guard/tests và FE fixtures/Storybook chạy song song theo lát dọc; nối API rồi kiểm tra các flow thực, không chờ toàn BE mới bắt đầu FE.

## 10. Test plan và acceptance

| Nhóm | Bằng chứng bắt buộc sau implementation |
|---|---|
| Identity/history | FK đúng school_class; đúng thứ/khoảng ngày/ngày nghỉ; ARCHIVED đã publish đọc/ghi theo ngày; entry ID mới không tạo trùng; publish/calendar bị chặn khi đụng lịch sử; race publish–create |
| Quyền | GVBM khác bị 403 trên GET/mutation/audit; GVCN chỉ lớp mình, không duyệt từng tiết; manager parity; reject actualTeacherId; student bị 403 |
| Deadline | Clock cố định; trước giờ kết thúc, đúng giờ kết thúc, trước/đúng deadline; hai mode và timezone; draft quá hạn không PUT/submit; late-record/complete draft có reason; closed semester |
| Lifecycle/data | Draft thiếu field lưu được; submit thiếu field bị chặn; SUBMITTED không về DRAFT; AMENDED giáo viên readonly; amend clear review metadata; count sum; policy mới không đổi tiết cũ |
| Concurrency/audit | Hai create cùng slot chỉ một thành công; stale version trên mọi mutation; tuần ký cạnh tranh amend; mọi mutation có before/after/actor; rollback audit; FK RESTRICT; policy cạnh tranh activation |
| Weekly | Thiếu/nháp/future/empty chặn ký; filter không che tiết thiếu; ký snapshot; mutation chuyển STALE; ký lại có reason, giữ audit phiên cũ |
| API/FE integration | 201/200 và 400/401/403/404/409/422 chính xác; create thành công submit lỗi giữ draft; lookup scope; bounded matrix; paged audit zero-based; không ghi attendance/scorebook |

DoD sau code: unit/integration + JaCoCo; Checkstyle main/test; PMD main/test; full build; MySQL/Flyway thực và H2 báo riêng; FE lint/test/build/build-storybook; browser/live role/expiry/conflict/weekly flows. Từng gate ghi PASS/FAIL/NOT RUN với lệnh và bằng chứng, không coi focused tests là full build PASS.

**Open trước code:** implementation approval BE/FE, kiểm chứng khả năng resolve enrollment lịch sử và chuẩn bị migration/Flyway; D01–D10 và contract/quyền/guard TKB đã được duyệt ở mức thiết kế. **Open trước production:** D10 retention/privacy và quyền lịch sử sau thay GVCN cần được hiện thực và kiểm chứng. Task hiện tại chỉ cập nhật tài liệu/prototype; không chạy các gate sản phẩm này.

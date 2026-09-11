# Plan 075 BE — Thời khóa biểu và định mức tiết dạy

## 1. Trạng thái, mục tiêu và nguồn

- Application-document version: **v3**; ngày lập: **2026-09-11**.
- Status: **DRAFT — chờ duyệt contract, wireframe và quyết định mở; chưa triển khai**.
- Người dùng yêu cầu viết plan + wireframe và xác nhận v3. Đây là authorization soạn tài liệu, chưa phải approval triển khai.
- Đồng hành: [Plan FE](../../fe/timetable/075-timetable-teacher-load-ui-2026-09-11.md), [wireframe](../../../wireframes/fe/timetable/075-timetable-teacher-load/README.md).
- Đọc theo thứ tự: [ApplicationContext](../../../application-doc/v3/ApplicationContext.md), [RequirementBaseline](../../../application-doc/v3/RequirementBaseline.md), [CR-V3-001](../../../application-doc/v3/change-request/CR-V3-001-academic-operations-and-targeted-communication.md), [Module 02](../../../application-doc/v3/modules/02-TimetableAndTeachingLoad.md), [data boundary](../../../application-doc/v3/data-model/README.md), [FE API boundary](../../../application-doc/v3/frontend-api/README.md), [Master Plan](../../summary/MASTER_PLAN_V3-2026-09-09.md).

Đáp ứng FR-V3-TT-001..004, BR-V3-TT-001..002 và NFR-V3-001..002: tạo lịch học kỳ, xem theo lớp/giáo viên/phòng/tiết, lưu nháp có vấn đề, kiểm tra từ backend trước công bố, đối chiếu định mức có nguồn và giữ lịch sử revision.

## 2. Phạm vi và baseline đã kiểm tra

- Hiện có `academic/domain/entity/Semester.java`: ngày bắt đầu/kết thúc và lifecycle; `assignment/domain/entity/SubjectTeachingAssignment.java`: classSubjectId, teacherId, validFrom/validTo, status; `HomeroomAssignment.java` cho chủ nhiệm. Các đường dẫn Java bên dưới tương đối với `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/`.
- FE có `academicApi.ts`, `assignmentApi.ts`, `teacherApi.ts`, shell/router v2 và review foundation v3. Chưa tìm thấy package timetable, room/calendar-slot hoặc teacher-load policy trong phạm vi mã nguồn đã rà.
- `academic/domain/entity/Subject.java` hiện chưa có mapping phòng; FE `views/academic/SubjectListView.vue` và `components/academic/SubjectDialog.vue` là điểm mở rộng gán phòng. Không tạo danh mục môn học trùng.
- Teacher hiện chưa cung cấp nguồn điều kiện nuôi con nhỏ phục vụ policy. Không suy eligibility từ tên, giới tính hoặc ngày sinh của giáo viên; cần bản xác nhận riêng.
- In-scope: lịch tuần lặp trong khoảng hiệu lực của học kỳ, tiết/ngày nghỉ, module phòng chức năng, gán phòng chức năng trong môn học, đăng ký lịch bận giáo viên, kiểm tra phân công, policy + điều kiện miễn giảm, draft/validate/publish/revision, audit, API thật, FE production, kiểm thử tích hợp.
- Out-of-scope: thuật toán tự xếp tối ưu, kéo thả bắt buộc, đổi tiết đột xuất/dạy bù tùy ngày, thông báo Plan 076, sổ đầu bài Plan 079, import/export lịch và sửa nghiệp vụ điểm/điểm danh v2.
- Không khởi động lại Plan 077; list mới có pagination riêng theo `ResultPaginationDTO`, không chuyển các lookup mảng v2 sang phân trang.

## 3. Quyết định đã chốt và phần còn mở

### Amendment theo người dùng — 2026-09-11

- **D02 đã chốt quyền**: TEACHER được đăng ký lịch bận của mình. ADMIN và ACADEMIC_OFFICE ngang quyền trong toàn phạm vi Plan 075, gồm xem, tạo/sửa, kiểm tra/công bố, cấu hình, phòng chức năng, gán phòng môn học và xác nhận miễn giảm. Không giữ hạn chế “Giáo vụ chỉ thấy mức giảm” của bản trước.
- **D09 đã chốt cơ chế duyệt**: lịch bận giáo viên cần ADMIN hoặc ACADEMIC_OFFICE duyệt trước; đăng ký chờ duyệt chưa chặn xếp lịch. Chỉ đăng ký đã duyệt có hiệu lực mới chặn tiết trùng.
- **D04 đã chốt cấu trúc và tài nguyên**: 4 tiết/buổi, 2 buổi/ngày; trình bày Sáng 1–4, Chiều 1–4. Chỉ quản lý/gắn phòng chức năng, không gán phòng học thông thường. Bổ sung module quản lý phòng chức năng và tính năng gán phòng chức năng trong quản lý môn học.
- TKB vẫn theo học kỳ, lặp theo tuần trong khoảng áp dụng; chọn tuần để xem. Đăng ký lịch bận không đồng nghĩa tạo một TKB mới cho mỗi tuần.
- Chưa phê duyệt implementation toàn plan. Giờ chuông/ngày học, cách xử lý lịch bận trùng lịch đã công bố và cardinality/điều kiện bắt buộc phòng còn cần chốt dưới đây.


Mọi thiết kế ở mục 4–9 là **đề xuất**, chưa phải API/schema hiện hữu.

| ID | Nội dung cần chốt | Đề xuất review | Gate bị ảnh hưởng |
|---|---|---|---|
| D01 | Nguồn, ngày hiệu lực, version policy | Người dùng cung cấp metadata chính thức; không seed policy active giả | Activate policy, publish |
| D02 | **RESOLVED — quyền** | ADMIN = ACADEMIC_OFFICE toàn phạm vi; TEACHER xem lịch của mình và đăng ký lịch bận của mình; STUDENT chưa thêm màn hình | Security, routes; semantics lịch bận ở D09 |
| D03 | Vượt/thiếu định mức có chặn? | Trùng lịch luôn chặn; vượt/thiếu là cảnh báo; thiếu policy/eligibility là chưa thể kết luận và chặn publish | Load evaluation, publish |
| D04 | **PARTIALLY RESOLVED** | Đã chốt 2 buổi/ngày × 4 tiết/buổi; chỉ phòng chức năng, có module quản lý và gán phòng trong môn học. Còn ngày học/giờ chuông/timezone | Calendar, phòng chức năng, môn học |
| D05 | Thay thế lịch đã công bố | Một đầu lịch hiện hành cho mỗi học kỳ/phạm vi trường; revision mới chỉ áp dụng từ ngày tương lai đã chọn; lịch cũ vẫn tra cứu theo ngày | Transaction, lịch sử, Plan 079 |
| D06 | Nguồn miễn giảm và quyền đọc | ADMIN và ACADEMIC_OFFICE ngang quyền quản lý/xem xác nhận; còn nguồn, retention và khoảng hiệu lực; chưa có xác nhận khác với không được giảm | Policy data/privacy |
| D07 | Thay đổi định mức trong tuần, nhiều chủ nhiệm | GVCN giảm một lần; chia báo cáo theo tuần/khoảng hiệu lực, không lấy trung bình học kỳ; tuần đổi eligibility yêu cầu review trước publish | Công thức, boundary tests |
| D08 | Học kỳ khóa và thay đổi assignment sau publish | Chặn mutation khi học kỳ không cho phép; assignment/calendar/phòng chức năng/gán phòng môn học/lịch bận mutation liên quan phải kiểm tra lịch hiện hành trong cùng transaction, yêu cầu revision trước thay đổi gây vô hiệu lịch | Integration với v2 |
| D09 | **PARTIALLY RESOLVED — cần duyệt trước** | Đã chốt ADMIN/ACADEMIC_OFFICE duyệt, chỉ APPROVED chặn lịch. Còn chốt một ngày/lặp theo tuần, sửa/rút sau duyệt và conflict với lịch published. Đề xuất từ chối approve khi trùng published, xử lý revision trước | Lifecycle, concurrency, published invariant |
| D10 | **OPEN — gán phòng môn học** | Đề xuất một môn có danh sách phòng chức năng phù hợp, chọn tối đa một phòng cho từng tiết; chưa chốt môn đã gán phòng có bắt buộc dùng mọi tiết hay chỉ tiết thực hành. Không suy thành đã duyệt | Mapping schema, editor, validation |

Các trị số đã có trong requirement: chuẩn 19; GVCN giảm 4; GV nữ nuôi con dưới 12 tháng giảm thêm 3; cộng dồn. Không tìm hoặc tự thay văn bản pháp lý trong task soạn plan này.

## 4. Slot identity, calendar và conflict

- Revision sở hữu lịch toàn học kỳ trong phạm vi trường hiện hữu; không thêm multi-tenant schoolId do suy đoán. Lọc một lớp chỉ là chế độ đọc, không làm thu hẹp kiểm tra publish.
- Calendar period có ID, `dayOfWeek`, `session=MORNING|AFTERNOON`, `periodIndex=1..4`, nhãn và giờ bắt đầu/kết thúc; mỗi ngày có 2 buổi × 4 tiết, unique `(calendarId, dayOfWeek, session, periodIndex)`; ngày nghỉ có ngày cụ thể hoặc period bị khóa. Khoảng giờ dùng [start, end), hai tiết tiếp giáp không trùng. Timezone trường phải được chốt tại checkpoint.
- Entry có ID bất biến trong revision, `assignmentId`, `periodId`, `validFrom`, `validTo`, `functionalRoomId?`. Lớp/môn/giáo viên được resolve từ assignment, không tin giá trị FE gửi thay thế. Khoảng ngày bao gồm hai đầu; backend mở rộng thành các lần học hợp lệ để kiểm tra.
- Môn không sử dụng phòng chức năng lưu `functionalRoomId=null`; không nhập phòng học thông thường. Khi có chọn phòng, backend kiểm tra phòng active và thuộc mapping môn học; policy bắt buộc phòng theo loại tiết/môn chờ D10. Hai môn/lớp khác nhau dùng cùng phòng chức năng vẫn phải bắt trùng toàn scope.
- Lịch bận là dependency riêng theo giáo viên, học kỳ và ngày/buổi/tiết hoặc khoảng lặp. Chỉ lịch bận `APPROVED` và có hiệu lực theo ngày mới tham gia chặn; `PENDING`, `REJECTED`, `WITHDRAWN` không chặn. Sáng tiết 1 và chiều tiết 1 là hai slot khác nhau.
- Nhận diện một lần học phục vụ Plan 079: `revisionId + entryId + date`; revision nguồn và lịch sử không xóa khi có tham chiếu.
- So sánh khoảng giờ thực và ngày giao nhau, kể cả hai periodId khác nhau nhưng chồng giờ. Kiểm tra toàn scope gồm draft đích và lịch hiện hành liên quan; loại bản cũ trong khoảng đang được thay thế để tránh tự báo trùng.
- Lưu draft vẫn validate và trả issue; sai JSON/FK/scope không được lưu. Conflict nghiệp vụ như trùng lịch có thể lưu vào draft. Không đặt unique constraint class/teacher/functionalRoom + slot trên draft vì sẽ cấm lưu lỗi.

| Code đề xuất (chỉ trong DTO kết quả kiểm tra) | Ý nghĩa | Severity |
|---|---|---|
| CLASS_OVERLAP | Lớp có hai tiết chồng nhau | BLOCKING |
| TEACHER_OVERLAP | Giáo viên có hai lớp chồng nhau | BLOCKING |
| ROOM_OVERLAP | Phòng chức năng đã có tiết trong khoảng đó | BLOCKING |
| FUNCTIONAL_ROOM_INVALID | Phòng không active hoặc không được gán cho môn | BLOCKING khi entry chọn phòng |
| TEACHER_UNAVAILABLE | Tiết học trùng lịch bận có hiệu lực của giáo viên | BLOCKING; chỉ APPROVED trong khoảng hiệu lực |
| PERIOD_UNAVAILABLE | Tiết nghỉ hoặc ngoài calendar | BLOCKING |
| ASSIGNMENT_INVALID | Sai scope/status/ngày hiệu lực | BLOCKING |
| POLICY_UNAVAILABLE | Chưa có policy có nguồn và hiệu lực bao phủ | BLOCKING đề xuất D03 |
| LOAD_UNDETERMINED | Thiếu xác nhận hoặc chưa xử lý tuần đổi điều kiện | BLOCKING đề xuất D03/D07 |
| LOAD_ABOVE_TARGET / LOAD_BELOW_TARGET | Lệch định mức | WARNING đề xuất D03 |

Mỗi issue có code, severity, entryIds, date/period và nhãn lớp/giáo viên/phòng cần thiết để điều hướng sửa; không tiết lộ lớp ngoài scope người xem. Không thêm shared error-code framework: HTTP lỗi tiếp tục dùng `RestResponse` hiện hữu.

## 5. Policy và lifecycle

Policy lưu `version`, `source`, `effectiveFrom`, `effectiveTo?`, `basePeriods=19`, các rule giảm có mã và trị số `4`, `3`, trạng thái. Policy active bất biến; thay đổi tạo version mới. Không activate khi thiếu metadata hoặc khoảng hiệu lực chồng nhau không giải quyết được.

Backend nhận policy/rule đã lưu, tính `target = basePeriods - sum(applicable reductions)`; fixture 19, 15, 16, 12 cho bốn tổ hợp. Không rải literal nghiệp vụ vào engine. Ghi nguồn xác nhận, thời hạn, người xác nhận; không ghi thông tin trẻ em vào audit/log rộng. Chi tiết dữ liệu nhạy cảm/retention phải duyệt ở D06 trước migration.

Load result gồm teacherId/name, weekStart, assignedPeriods, basePeriods, reductions, targetPeriods, difference, policyVersion và evaluationStatus. Tính trên toàn lịch của giáo viên, không riêng lớp đang lọc; không dùng tổng số entry thô nếu ngày hiệu lực khác nhau. Cách tính tuần nghỉ/đổi eligibility phải chốt trong D07; không tự coi tuần nghỉ là thiếu định mức.

`DRAFT -> VALIDATED -> PUBLISHED -> ARCHIVED`:

1. Tạo/lưu draft tăng optimistic version; mỗi sửa đổi hủy validation cũ về DRAFT.
2. Validate đọc lại assignment/calendar/phòng chức năng/mapping môn học/lịch bận có hiệu lực/policy/eligibility, lưu snapshot/fingerprint và kết quả toàn lịch; chỉ chuyển VALIDATED khi không có lỗi chặn.
3. Publish yêu cầu expectedVersion + intent idempotency key, kiểm tra quyền và học kỳ, khóa đầu lịch học kỳ, đọc lại dependency và chạy validation lần cuối trong transaction.
4. Nếu dependency đổi hoặc có conflict, trả 409 cho stale state hoặc 422 cho lỗi nghiệp vụ; không ghi một phần. Không tin status VALIDATED cũ.
5. Publish đồng thời hai draft: serialization trên đầu lịch bảo đảm chỉ một request với expected head thắng. Những mutation assignment/calendar/phòng/mapping/lịch bận liên quan dùng cùng invariant/locking strategy theo D08.
6. Revision đã publish read-only; action tạo revision sao chép entry và lineage, có effectiveFrom. Publish revision mới đóng khoảng hiệu lực bản trước một cách atomic, không xóa lịch quá khứ; archive chỉ khi không còn là lịch hiện hành. Không sửa entry cũ mà lesson log có thể tham chiếu.
7. Request publish lặp cùng key/body trả cùng kết quả; cùng key khác body trả 409; audit một lần. Sau timeout, client đọc lại trạng thái trước khi quyết định retry cùng intent.

### 5.1 Lịch bận giáo viên — cần duyệt trước

- TEACHER tạo/xem/sửa/rút đăng ký của chính mình; backend resolve teacherId từ principal, từ chối ID của giáo viên khác. ADMIN/ACADEMIC_OFFICE ngang quyền xem, duyệt hoặc từ chối đăng ký. Quyền ngang nhau không có nghĩa TEACHER tự duyệt.
- Dữ liệu đề xuất: id, teacherId, semesterId, date hoặc dayOfWeek + validFrom/To, session, periodIndexes, note?, status, version, submittedAt, decidedBy/At, decisionReason. Buổi thuộc hai giá trị, tiết 1–4, khoảng ngày trong học kỳ. Ghi chú không hiển thị trên lịch chung.
- Lifecycle đề xuất theo cơ chế duyệt đã chốt: tạo → `PENDING`; quản lý → `APPROVED` hoặc `REJECTED`; chủ đăng ký rút → `WITHDRAWN`. Chỉ `APPROVED` còn hiệu lực được kiểm tra thành `TEACHER_UNAVAILABLE`. Trả counts pending riêng, không cộng vào lỗi chặn.
- Đề xuất sửa nội dung chỉ khi `PENDING`; sau duyệt giữ bản đã duyệt bất biến, muốn thay đổi thì rút và gửi đăng ký mới để duyệt lại. Sửa/rút/approve/reject đều có expectedVersion và audit. Thời hạn rút sau duyệt và tái gửi từ chối còn thuộc D09, chưa tự mở auto-approval.
- Duyệt phải đọc lại published revision/assignment, khóa cùng scope publish và recheck. **Đề xuất D09**: nếu trùng tiết đã công bố, trả 422 cùng tiết bị ảnh hưởng, giữ PENDING; Giáo vụ/Admin điều chỉnh TKB trước rồi duyệt lại. Không tự hủy/dời tiết hoặc tự cho lịch bận thắng lịch đã công bố.
- Nếu trùng draft: duyệt thành công làm validation cũ stale; publish sau đó phải bắt lỗi lịch bận. Race approve/publish: chỉ thứ tự không phá invariant được commit; kết quả conflict trả 409/422, không ghi audit/approval nửa chừng. Hai người duyệt đồng thời chỉ một transition/audit thành công.

### 5.2 Module phòng chức năng và gán môn học

- Module `functionalroom/` sở hữu danh mục mã/tên/trạng thái/version và CRUD phù hợp; ADMIN/ACADEMIC_OFFICE ngang quyền. Không quản lý phòng lớp thông thường trong module này.
- Quản lý môn học hiện hữu có vùng `Phòng chức năng`; mapping đề xuất nhiều phòng phù hợp cho một môn (D10). Mỗi tiết chọn một phòng trong danh sách phù hợp hoặc không dùng theo rule được duyệt.
- Chỉ ngừng sử dụng khi đã xử lý các tiết hiện hành/tương lai liên quan; xóa chỉ được khi chưa có tham chiếu. Đổi mapping không làm mất liên kết phòng trên lịch sử revision. Snapshot hoặc mapping version tham gia fingerprint publish.
- Chưa tự thêm capacity, thiết bị, đặt phòng ngoài TKB hoặc module quản lý tòa nhà.

## 6. Contract checkpoint đề xuất

Base `/api/v3`; endpoint chỉ được triển khai sau khi duyệt. Các mutation draft trả detail mới gồm version và review; mọi DTO được freeze bằng JSON fixture chung BE/FE.

| Method/path | Request chính | Response |
|---|---|---|
| GET /timetables?semesterId=&page=&size= | Scope, zero-based paging | ResultPaginationDTO<Summary> |
| POST /timetables | semesterId, effectiveFrom/To, policyId, expectedHeadVersion | 201 RevisionDetail DRAFT |
| GET /timetables/{id} | id | RevisionDetail, capabilities |
| GET /timetables/{id}/entries?weekStart=&classId=&teacherId=&functionalRoomId=&session=&periodId= | Tuần bắt buộc; filter server-side | Entry[] của cửa sổ tuần hữu hạn |
| PUT /timetables/{id}/entries | expectedVersion, upserts[], deletedEntryIds[] | Detail + review; atomic |
| POST /timetables/{id}/validate | expectedVersion | ReviewResult + version/status |
| GET /timetables/{id}/review | id | Toàn bộ counts, issues, load results theo tuần |
| POST /timetables/{id}/publish | expectedVersion, expectedHeadVersion; Idempotency-Key header | Detail PUBLISHED |
| POST /timetables/{id}/revisions | expectedVersion, effectiveFrom | 201 Detail DRAFT của revision mới |
| GET /timetable-calendars?semesterId= | semester | Periods (2 × 4) + closedDates |
| PUT /timetable-calendars/{id} | expectedVersion, periods (2 × 4), closedDates | Calendar; bảo vệ lịch published |
| GET /functional-rooms?page=&size=&search=&status= | Danh sách quản lý phân trang | ResultPaginationDTO<FunctionalRoom> |
| GET /functional-rooms/lookup?subjectId=&status= | Lookup phòng chức năng theo môn | FunctionalRoom[] |
| POST /functional-rooms | code, name | 201 FunctionalRoom |
| PUT /functional-rooms/{id} | expectedVersion, code, name, status | FunctionalRoom; bảo vệ lịch liên quan |
| DELETE /functional-rooms/{id}?expectedVersion= | Chỉ phòng chưa được tham chiếu | 204; nếu đang được dùng trả 409 |
| GET /subjects/{subjectId}/functional-rooms | Môn hiện hữu v2 | Mapping + version |
| PUT /subjects/{subjectId}/functional-rooms | expectedVersion, functionalRoomIds[] (đề xuất D10) | Mapping mới; recheck lịch |
| GET /teacher-unavailability?semesterId=&from=&to=&teacherId= | Teacher bị ép self; ADMIN/ACADEMIC_OFFICE lọc toàn scope | Registration[] theo khoảng ngày hữu hạn |
| POST /teacher-unavailability | scope thời gian/buổi/tiết/note; teacherId resolve từ principal trong flow TEACHER | 201 Registration PENDING |
| PUT /teacher-unavailability/{id} | expectedVersion, nội dung sửa | Registration PENDING; chỉ sửa pending theo đề xuất |
| POST /teacher-unavailability/{id}/approve | expectedVersion; ADMIN/ACADEMIC_OFFICE | APPROVED hoặc lỗi conflict lịch published theo D09 |
| POST /teacher-unavailability/{id}/reject | expectedVersion, reason | REJECTED + audit quyết định |
| POST /teacher-unavailability/{id}/withdraw | expectedVersion | Registration sau rút; giữ audit |
| GET /teacher-load-policies | page, size | ResultPaginationDTO<PolicySummary> |
| POST /teacher-load-policies | nguồn, version, ngày, tham số | 201 Policy DRAFT |
| POST /teacher-load-policies/{id}/activate | expectedVersion | Active policy, metadata bắt buộc |
| GET /teacher-load-eligibilities?teacherId= | teacher, khoảng ngày | Xác nhận theo quyền D06 |
| POST /teacher-load-eligibilities | teacherId, ruleCode, validFrom/To, evidenceReference | 201 Eligibility |
| PUT /teacher-load-eligibilities/{id} | expectedVersion, thời hạn/trạng thái/xác nhận | Eligibility; tái kiểm tra tác động lịch |

Policy không sửa nội dung sau tạo: tạo version mới nếu cần sửa; màn hình phải thể hiện rõ. Calendar lần đầu được tạo cùng timetable với cấu hình đã duyệt; chi tiết create body `calendarDefinition` là phần bắt buộc nếu học kỳ chưa có calendar. Phòng chức năng không xóa khi được tham chiếu; ngừng sử dụng phải kiểm tra ảnh hưởng lịch. API mapping mới đặt ở v3, không đổi wire shape CRUD môn v2 ngầm.

RevisionDetail tối thiểu: id, semesterId/name, revisionNumber, status, version, headVersion, effectiveFrom/To, policyId/version, validationFingerprint, blockingCount, warningCount, capabilities. ReviewResult: revisionVersion, validatedAt, issues[], teacherLoads[], totals. Calendar/week result không phân trang cắt cụt lịch; nếu cần giới hạn kích thước phải báo rõ và chốt limit tại checkpoint.

400 malformed; 401 chưa đăng nhập; 403 sai quyền/scope; 404 không tìm thấy trong scope; 409 stale version/lifecycle/idempotency; 422 kiểm tra publish không đạt. Reuse `RestResponse`, `ResultPaginationDTO`; không dùng `V3Page`/`V3PageResponse` dù foundation cũ còn type này.

## 7. Database và phạm vi file dự kiến

Tạo module `timetable/`: `controller/TimetableController.java`, `TimetableCalendarController.java`, `TeacherLoadPolicyController.java`, `TeacherLoadEligibilityController.java`, `TeacherUnavailabilityController.java`; `service/TimetableService.java` (draft/revise), `TimetableValidationService.java`, `TimetablePublishService.java`, `TeacherLoadEvaluator.java`; repositories, entities và request/response DTO dưới đúng domain package. Controller mỏng, quyền/transaction/invariant đặt tại application service và guard.

Các bảng đề xuất: `timetable_head` (unique semester), `timetable_revision` (unique semester/revision number, version, lineage), `timetable_entry` (FK revision/assignment/period/functional_room), `timetable_period`, `timetable_closed_date`, `timetable_calendar`, `functional_room`, `subject_functional_room`, `teacher_unavailability`, `teacher_load_policy`, `teacher_load_rule`, `teacher_load_eligibility`, `timetable_validation`, `timetable_audit`, `timetable_publish_intent` (unique actor/scope/key + payload hash/result). Index cho revision, teacher/assignment, period/date ranges; FK giữ lịch sử. Các association thực tế phải đối chiếu schema v2 tại checkpoint, không thêm duplicate teacher/class/subject.

- Tạo `functionalroom/controller/FunctionalRoomController.java`, `service/FunctionalRoomService.java`, repository, entity và request/response DTO; tạo `academic/controller/SubjectFunctionalRoomController.java` + `academic/service/SubjectFunctionalRoomService.java` cho mapping. Dùng Subject hiện hữu làm FK; cardinality chờ D10.
- Tạo `timetable/service/TeacherUnavailabilityService.java`, repository, entity/DTO và authorization self-scope; đưa version lịch bận/phòng/mapping vào validation fingerprint.
- Tests bổ sung dưới `functionalroom/` và `academic/` cho CRUD/mapping; file FE mở rộng môn học nêu trong plan FE.
- Migration mới `BE/BaiTap-RS/src/main/resources/db/migration/V<next>__create_timetable_and_teacher_load.sql`; xác định số kế tiếp lúc triển khai, không sửa migration cũ.
- Chỉnh các mutation service trong `assignment/service/` và `academic/service/` đúng D08 để bảo vệ dependency; review cụ thể method trước code. Không thay đổi policy khóa học kỳ hiện hữu ngầm.
- Tests tương ứng dưới `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/timetable/`.
- Kết thúc triển khai cập nhật Dev Note, validation result và các summary; không tạo Postman nếu chưa được yêu cầu.

## 8. Unit test và integration plan

| Class/method | Fixture / dependency | Assertion |
|---|---|---|
| TimetableValidationService.validate | Fake calendar/repository; cùng lớp, GV, phòng; overlap time | Đủ code/entry liên quan; khác phòng không bỏ sót GV; slot ID khác vẫn bắt overlap |
| validate boundaries | Adjacent periods, validFrom/To, ngày nghỉ, empty, invalid FK, inactive assignment | Hai tiết tiếp giáp hợp lệ; assignment bao phủ mọi occurrence; empty draft không bị nhận là lịch sẵn sàng publish (đề xuất checkpoint) |
| TeacherLoadEvaluator.evaluate | Policy parameter fixture 19/4/3; fake clock, evidence | 19/15/16/12; cộng dồn đúng; không nhân đôi giảm chủ nhiệm; thiếu xác nhận trả UNDETERMINED |
| evaluate dates | Đủ 12 tháng, trước/sau effectiveFrom/To, thay policy giữa kỳ, tuần nghỉ | Biên thời gian theo D07 đã duyệt, không hard-code suy đoán |
| TimetableService.save/revise | Mock repository + guard | Version tăng, invalidate review, published không sửa, draft có conflict lưu được, không side effect trên lỗi scope |
| TimetablePublishService.publish | Guard/repo/audit; transaction thật cho concurrency | Validate lại, rollback mọi write khi fail, snapshot/audit đúng; lặp key không double audit |
| Controllers | Parameterized ADMIN + ACADEMIC_OFFICE và TEACHER khác nhau | ADMIN/ACADEMIC_OFFICE cùng allow/deny mọi action; teacher chỉ đăng ký self, không sửa người khác/publish/cấu hình; JSON đúng |
| TeacherUnavailabilityService + validate | Lịch bận một ngày/lặp, cùng sáng/chiều, update/withdraw | Biên hiệu lực, 1–4 tiết, self ownership; PENDING/REJECTED/WITHDRAWN không chặn, APPROVED chặn; teacher không duyệt, cả hai role quản lý duyệt được; đăng ký không tự đổi lịch published |
| FunctionalRoomService / SubjectFunctionalRoomService | Trùng mã, inactive room, sai mapping, phòng có tham chiếu | CRUD, mapping/version; không xóa lịch sử; môn không dùng phòng nhận null; rule bắt buộc chờ D10 |
| Calendar + slot validation | MORNING/AFTERNOON, 1/4 hợp lệ, 0/5 sai | 8 slot/ngày; sáng 1 không bằng chiều 1, vẫn kiểm tra giờ overlap thật |

Integration MySQL: migration từ schema hiện tại; FK/index; hai publish đồng thời; publish đua assignment update, approve lịch bận đồng thời/two-approver race, phòng ngừng hoạt động và mapping đổi; stale head; rollback không archive lịch cũ nếu publish lỗi; reload đọc đúng revision theo ngày; intent retry sau timeout. Không dùng mock repository để chứng minh transaction/locking. Fixture xác định, cleanup scope test, không sửa dữ liệu live ngoài kịch bản được phép.

Chạy trong `BE/BaiTap-RS`: `./gradlew test jacocoTestReport checkstyleMain checkstyleTest pmdMain pmdTest build`. Đọc JaCoCo HTML/XML cho branch overlap, eligibility, publish rollback; không tự đặt coverage threshold mới. Phân biệt failure baseline với regression. Gate FE và browser theo plan đồng hành.

## 9. Delivery, rủi ro và acceptance

- 075.0: giữ quyền D02 và cấu trúc D04 đã chốt, duyệt phần còn mở D01/D03–D10, API/schema/fixture; cập nhật boundary v3 khi contract được duyệt.
- 075.1: BE draft/calendar 2 × 4, phòng chức năng/mapping môn học, lịch bận và FE editor/module tương ứng dùng fixture đã khóa có thể làm song song.
- 075.2: conflict/load engine và FE review/policy forms làm song song.
- 075.3: publish/revision/audit và FE confirmation/read-only; tích hợp endpoint thật cùng plan.
- 075.4: automated quality gates, browser/live, migration readiness, Dev Note và review.

Rủi ro chính: lịch sử bị ghi đè (revision bất biến), publish race (scope lock + revalidate), lộ điều kiện miễn giảm (authorization/projection), lịch lớn (query theo tuần + index, kiểm tra toàn scope ở publish), policy thiếu nguồn (không active), assignment thay đổi làm lịch vô hiệu (D08). Không claim xử lý xong các rủi ro khi chỉ có fixture.

Acceptance: 2 buổi × 4 tiết/ngày; teacher đăng ký lịch bận self qua phê duyệt ADMIN/ACADEMIC_OFFICE theo D09; ADMIN/ACADEMIC_OFFICE ngang quyền; quản lý phòng chức năng/gán môn và kiểm tra trùng phòng; tạo/sửa/tải lại nháp thật; đủ bốn bộ lọc; lưu conflict nhưng không công bố được; 19/15/16/12 được tính từ policy; metadata chính thức được lưu; direct API đúng quyền; version/idempotency/concurrent publish được kiểm thử; revision giữ lịch sử; FE route thật tích hợp BE; browser thao tác và reload có evidence. Mọi gate implementation hiện **NOT RUN**. Plan chỉ được đánh dấu completed khi acceptance thực sự đạt, không chỉ có wireframe/Storybook.

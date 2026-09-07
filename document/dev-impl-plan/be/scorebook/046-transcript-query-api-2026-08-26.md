# Developer Plan 046: Transcript Query API

## 1. Trạng thái và phiên bản áp dụng

- **Status**: `Proposed` — chưa được phê duyệt, chưa triển khai.
- **Application-document version**: `v2` — user xác nhận ngày `2026-08-26`.
- **Ngày lập plan**: `2026-08-26`.
- **Module**: Backend `scorebook` / transcript read model.
- **Phụ thuộc**: Plan 041 (transcript schema), Plan 042 (calculation lifecycle), Plan 044 (transcript aggregation) và Plan 045 (retake integration) đã cung cấp dữ liệu đọc.

## 2. Mục tiêu

Tạo API read-only cho FE để:

- xem bảng điểm theo học kỳ;
- xem bảng điểm cả năm;
- xem chi tiết kết quả từng môn;
- trả chính xác trạng thái calculation `IN_PROGRESS` hoặc `FINISH`;
- cho học sinh chỉ xem dữ liệu của chính mình;
- cho GVCN xem trong phạm vi lớp được phân công;
- cho GVBM xem bảng điểm đầy đủ của học sinh trong lớp mình dạy để nắm thông tin bổ sung về lớp;
- cho `ACADEMIC_OFFICE` và `ADMIN` xem toàn trường;
- bảo đảm mọi `GET` không tạo task, không tính lại điểm và không ghi dữ liệu.

## 3. Requirement và ràng buộc liên quan

- `FR-SUMMARY-001` đến `FR-SUMMARY-011`: hiển thị điểm thành phần/kết quả môn, học kỳ, cả năm, điểm kỹ năng riêng, retake, trạng thái, thời điểm tính, nguồn kết quả và ghi chú chuyển lớp/thi lại.
- `FR-SCORE-002`, `FR-SCORE-003`, `FR-SCORE-005`: cột điểm được cấu hình theo sổ điểm và ô điểm chưa nhập phải được thể hiện rõ, không suy ra thành `0`.
- `BR-SUMMARY-005`: `IN_PROGRESS` chỉ là trạng thái đang cập nhật, không được coi là kết quả chính thức.
- `BR-SUMMARY-006`: `FINISH` mới cho phép sử dụng kết quả chính thức.
- `BR-SUMMARY-007`: khi có thi lại phải trả đồng thời điểm trước thi lại, `retakeScore` và điểm chính thức sau thi lại.
- `BR-SUMMARY-008`: có chuyển lớp phải trả ghi chú lớp cũ, lớp mới và ngày chuyển.
- Ma trận quyền mục 23 của `07-AccessQualityAndAcceptanceModule.md`: học sinh xem bản thân; GVCN xem lớp mình; giáo vụ xem toàn trường.
- `NFR-PERFORMANCE-001`: API đọc không được thực hiện calculation trong HTTP request.
- `NFR-SECURITY-003` đến `NFR-SECURITY-005`: kiểm soát quyền dựa trên assignment thực tế; trả `401`/`403` đúng ngữ nghĩa.

## 4. Phạm vi

### 4.1. In-scope

- Read-only API cho transcript học kỳ, cả năm và chi tiết từng môn.
- Response DTO lồng dữ liệu transcript, result môn, chi tiết cột điểm học kỳ, trạng thái/version/calculated time, nguồn `REGULAR`/`RETAKE`, retake và ghi chú chuyển lớp.
- Resolver xác định student hiện tại từ `AuditContext.currentUserId()` cho endpoint `/me`.
- Authorization ở service cho `STUDENT`, `TEACHER` (GVCN hoặc GVBM có assignment hợp lệ), `ACADEMIC_OFFICE` và `ADMIN`.
- Query repository tối thiểu, deterministic ordering và bulk loading để tránh N+1 query.
- Unit/integration tests cho query, authorization, trạng thái và tính read-only.

### 4.2. Out-of-scope

- Không tính, tạo `CalculationTask`, chạm `sourceVersion`, cập nhật transcript/result hay retake.
- Không thay đổi công thức, worker, migration/schema, enum hoặc dữ liệu lịch sử.
- Không tạo API write cho transcript/retake, không sửa score entry/score change.
- Không triển khai FE, Storybook hoặc Postman collection.
- GVBM không được xem transcript của học sinh ngoài các lớp được phân công dạy; không suy diễn quyền từ role `TEACHER` đơn thuần.

## 5. Thiết kế đề xuất

### 5.1. API contract

```http
GET /api/v2/transcripts/students/me/semesters/{semesterId}
GET /api/v2/transcripts/students/me/academic-years/{academicYearId}

GET /api/v2/transcripts/students/{studentId}/semesters/{semesterId}
GET /api/v2/transcripts/students/{studentId}/academic-years/{academicYearId}
```

- Hai endpoint `/me` chỉ nhận `STUDENT`; student id luôn resolve từ authenticated user, không nhận từ request.
- Hai endpoint theo `{studentId}` nhận `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`; service tiếp tục kiểm tra scope, không tin riêng annotation controller.
- `TEACHER` chỉ được trả dữ liệu khi teacher hiện tại có assignment hợp lệ với lớp mà học sinh thuộc trong phạm vi học kỳ/năm được truy vấn: GVCN theo `HomeroomAssignment` hoặc GVBM theo `SubjectTeachingAssignment`. GVBM được trả toàn bộ các môn trong transcript của học sinh thuộc lớp mình dạy, không chỉ môn được phân công, nhưng không được đọc transcript ngoài lớp scope. `ACADEMIC_OFFICE` và `ADMIN` không bị giới hạn lớp.
- Không có transcript/result phù hợp trả `404`; dữ liệu hợp lệ nhưng chưa được worker hoàn tất vẫn trả `200` với `calculationStatus = IN_PROGRESS` và điểm `null` nếu chưa có.

### 5.2. Response dự kiến

Term response chứa `studentId`, `academicYearId`, `semesterId`, `calculationStatus`, `sourceVersion`, `calculatedVersion`, `calculatedAt`, `dtbhk`, `lastError` (nếu được phép expose theo convention hiện có), ghi chú chuyển lớp và danh sách môn. Mỗi môn trả `subjectId`, thông tin hiển thị môn, `subjectType`, `dtbmh` hoặc `skillScore`, calculated version/time và `assessmentColumns`.

`assessmentColumns` là danh sách cột theo đúng cấu hình `AssessmentColumn` của sổ điểm môn/lớp/học kỳ, sắp xếp theo nhóm `assessmentType` rồi `columnNo`. Mỗi phần tử gồm `columnId`, `assessmentType`, `columnNo`, `columnName`, `scoreStatus`, `scoreValue`. API không hard-code số lượng cột: nhóm kỹ thuật hiện có là `KTTT`, `KTĐK`, `KTCK`; FE có thể render theo metadata thành các header như phác thảo `KTTX 1...4`, `KTĐK 1...2`, `KTCK 1` khi `columnName`/quy ước hiển thị được chốt. Ô chưa nhập phải trả `scoreValue: null` cùng `scoreStatus` thực tế, không trả `0`.

Ví dụ một môn học kỳ:

```json
{
  "subjectId": 11,
  "subjectName": "Toán",
  "subjectType": "NORMAL",
  "assessmentColumns": [
    { "columnId": 201, "assessmentType": "KTTT", "columnNo": 1, "columnName": "KTTX 1", "scoreStatus": "SCORED", "scoreValue": 8.0 },
    { "columnId": 202, "assessmentType": "KTTT", "columnNo": 2, "columnName": "KTTX 2", "scoreStatus": "SCORED", "scoreValue": 7.0 },
    { "columnId": 203, "assessmentType": "KTDK", "columnNo": 1, "columnName": "KTĐK 1", "scoreStatus": "SCORED", "scoreValue": 8.0 },
    { "columnId": 204, "assessmentType": "KTCK", "columnNo": 1, "columnName": "KTCK", "scoreStatus": "SCORED", "scoreValue": 7.0 }
  ],
  "dtbmh": 7.5,
  "skillScore": null
}
```

Annual response chứa metadata annual transcript, `regularDtbcn`, `finalDtbcn`, `resultSource`, `lastCalculationTaskId`, trạng thái/version/time, ghi chú chuyển lớp và danh sách môn. Mỗi môn trả `hk1`, `hk2`, `regularDtbmhCn`, `officialDtbmhCn`, `calculationSource`, và khi `retakeId` khác `null` thì trả `preRetakeScore`, `retakeScore`, ngày thi, trạng thái retake và note.

Không suy diễn điểm thiếu thành `0`. Môn `SKILL` luôn trả độc lập bằng `skillScore` và không bị diễn đạt như một thành phần của `dtbhk`/`dtbcn`.

### 5.3. Luồng đọc và kiểm soát quyền

```text
GET request
  -> Controller: validate path, resolve endpoint kind
  -> TranscriptQueryService: resolve student + transcript
  -> TranscriptAccessGuard: xác thực self / GVCN-GVBM scope / office-admin scope
  -> bulk-load term/annual results, subject, scorebook, assessment column, student score, retake, enrollment history
  -> map DTO theo dữ liệu đã tính sẵn
  -> trả response, không gọi TranscriptStateService, CalculationTaskService hay worker
```

`TranscriptAccessGuard` dùng `AuditContext.currentUserId()`; với teacher, resolve `Teacher` theo `userId` rồi kiểm tra `HomeroomAssignment` hoặc `SubjectTeachingAssignment` có hiệu lực tương ứng với enrollment của học sinh trong phạm vi dữ liệu trả về. Không chấp nhận `teacherId`, `classId` hoặc `studentId` từ body/query để thay thế identity/scope server-side.

### 5.4. Quyết định cần approval

Plan đề xuất policy teacher scope như sau: với query học kỳ, teacher phải là GVCN hoặc GVBM có assignment hợp lệ của lớp enrollment trong học kỳ đó; với query cả năm, teacher phải có một trong hai assignment hợp lệ cho ít nhất một enrollment của học sinh trong năm. Response cả năm vẫn chứa toàn bộ kết quả năm và note chuyển lớp. GVBM được xem đầy đủ điểm các môn của học sinh trong lớp mình dạy nhằm nắm thông tin bổ sung của lớp, không bị giới hạn vào môn được phân công. Đây là cách diễn giải "phạm vi lớp" cần được phê duyệt rõ; nếu cần giới hạn annual response theo từng lớp/đợt enrollment, contract phải được điều chỉnh trước implementation.

## 6. Phạm vi mã nguồn dự kiến

### 6.1. Tạo mới

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/controller/TranscriptQueryController.java`: bốn `GET` endpoint và `@PreAuthorize` theo nhóm role.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/TranscriptQueryService.java`: orchestration read-only, lookup và mapping response.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/TranscriptAccessGuard.java`: ownership và GVCN/GVBM scope guard dùng identity server-side.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/DTOs/response/ResStudentTermTranscriptDTO.java`: contract học kỳ và nested subject result.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/DTOs/response/ResStudentAnnualTranscriptDTO.java`: contract cả năm và nested annual subject/retake result.
- Test service/controller tương ứng trong `src/test/java/.../scorebook/`.

### 6.2. Chỉnh sửa tối thiểu

- `StudentAnnualTranscriptRepository`, `StudentTermTranscriptRepository`, `StudentSubjectAnnualResultRepository`, `StudentSubjectTermResultRepository`, `ScorebookRepository`, `AssessmentColumnRepository`, `StudentScoreRepository`: thêm projection/fetch query chỉ khi method hiện có không đủ bulk-load cho API và chi tiết cột điểm.
- `RetakeExamRepository`: thêm lookup theo tập `retakeId` chỉ khi cần để map annual detail không gây N+1.
- `StudentRepository`, `TeacherRepository`, `HomeroomAssignmentRepository`, `SubjectTeachingAssignmentRepository`, enrollment repository: chỉ bổ sung query identity/scope còn thiếu.

Không chỉnh sửa `TranscriptRecalculationService`, `TranscriptStateService`, `CalculationTaskService`, worker, entity mapping hoặc migration.

## 7. Kế hoạch test và validation

### 7.1. Unit test

- Student gọi `/me` lấy đúng transcript của mình; không có đường truyền `studentId` tùy ý.
- Admin/giáo vụ xem bất kỳ student hợp lệ; teacher không phải GVCN/GVBM hoặc sai scope bị `403`.
- GVCN hoặc GVBM đúng scope xem được học kỳ/cả năm theo policy đã chốt; GVBM nhận đủ điểm các môn của học sinh trong lớp dạy, nhưng student khác/teacher khác không lộ dữ liệu.
- Term response trả `dtbmh` cho môn thường, `skillScore` riêng cho môn kỹ năng, giữ `null` cho dữ liệu chưa tính.
- Term response trả đúng danh sách `assessmentColumns` theo cấu hình scorebook, đúng thứ tự nhóm/cột, đúng `scoreStatus`/`scoreValue`; cột chưa nhập giữ `null`, không thành `0`.
- Không hard-code số cột KTTT/KTĐK/KTCK; chỉ trả cột ACTIVE thuộc scorebook của môn/lớp/học kỳ được truy vấn.
- Annual response trả HK1/HK2, regular/final score, `REGULAR`/`RETAKE`, chi tiết retake và note chuyển lớp đúng nguồn.
- `IN_PROGRESS` và `FINISH` được trả nguyên trạng; API không đổi status/version, không gọi state/task/worker dependencies.
- Không tìm thấy student/transcript/semester phù hợp trả `404`; path id không hợp lệ bị Bean Validation từ chối.
- Repository query/load có deterministic subject order và không tạo N+1 theo số môn.

### 7.2. Integration/security test

- `401` khi không có JWT; `403` cho role không phù hợp hoặc scope GVCN/GVBM sai.
- JWT student không thể thay URL `{studentId}` để đọc transcript người khác.
- JWT teacher chỉ truy cập được transcript thuộc GVCN hoặc GVBM scope; GVBM không truy cập được học sinh ngoài lớp dạy; `ACADEMIC_OFFICE`/`ADMIN` đọc toàn trường.
- Kiểm chứng một `GET` không tạo `calculation_task` và không cập nhật `updated_at`/version/status transcript.

### 7.3. Validation

Sau implementation, chạy skill `backend-validation` để lấy `Validation Result`, gồm test, Checkstyle, PMD và build theo script thực tế của `BE/BaiTap-RS`. Đọc JaCoCo cho các nhánh access control, `IN_PROGRESS`/`FINISH`, retake/no-retake và not-found; không tự đặt coverage threshold.

## 8. Rủi ro và giảm thiểu

- **Lộ dữ liệu bảng điểm qua ID URL**: endpoint theo `{studentId}` bắt buộc authorization ở service; student chỉ dùng `/me`.
- **Scope teacher không rõ khi học sinh chuyển lớp**: áp dụng policy ở mục 5.4 sau approval; GVBM chỉ có quyền khi subject-teaching assignment khớp lớp/enrollment trong phạm vi truy vấn.
- **N+1 query khi nhiều môn/cột điểm/retake**: ưu tiên bulk query/projection và map in-memory theo id.
- **Nhầm read API với calculation trigger**: tách query service; cấm dependency tới state/task/worker mutation services.
- **Hiển thị nhầm dữ liệu chưa chính thức**: trả rõ calculation status và không thay `null` bằng zero.
- **Rò rỉ error/audit nội bộ**: chỉ expose `lastError` nếu project contract cho phép; mặc định response không trả actor/audit metadata nội bộ.

## 9. Output dự kiến

- FE có contract backend để xem bảng điểm học kỳ, cả năm và chi tiết từng môn; bảng học kỳ có đủ cột điểm cấu hình cùng từng ô điểm, bao gồm retake khi có.
- Trạng thái calculation rõ ràng `IN_PROGRESS`/`FINISH`; mọi điểm là dữ liệu đã lưu, không có calculation trong `GET`.
- Học sinh chỉ đọc bản thân; GVCN/GVBM đúng scope đọc học sinh lớp mình, trong đó GVBM xem đầy đủ các môn; giáo vụ/admin đọc toàn trường.

## 10. Approval gate

Chỉ sau khi user phê duyệt rõ Developer Plan 046 và policy GVCN/GVBM ở mục 5.4 qua agent mới được sửa production code, test hoặc documentation trạng thái implementation. Sau implementation phải tạo Dev Note theo workflow dự án.

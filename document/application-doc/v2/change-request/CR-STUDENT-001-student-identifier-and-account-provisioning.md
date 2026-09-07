# CR-STUDENT-001: Student Identifier Resolution & Student Account Provisioning

## 1. Metadata

- **Status**: `Approved / Recorded / Implementation-backed`
- **Ngày ghi nhận**: `2026-08-26`
- **Application-document version**: `v2`
- **Scope owner**: Student, User/Auth; tác động chéo Enrollment, Scorebook, Attendance và Academic
- **Nguồn thay đổi**:
  - `document/dev-impl-plan/be/student/042.1-student-code-input-resolution-and-display-2026-08-25.md`
  - `document/dev-note/be/student/042.1-student-code-input-resolution-and-display-2026-08-25.md`
  - `document/dev-impl-plan/be/student/043-student-create-with-account-2026-08-26.md`
  - `document/dev-note/be/student/043-student-create-with-account-2026-08-26.md`
- **Source branch**: `docs/modular-application-doc-v2`
- **Source snapshot**: `69c64a192e3f7b8616f9de783aad2756e4ded59a`
- **Baseline đối chiếu**:
  - `THCS_Student_Management_Requirement_Baseline_v1.1.md`
  - `DataStructure-v2.md`
  - `document/application-doc/v2/modules/StudentModule.md`
  - `document/application-doc/v2/modules/UserModule.md`

> Tài liệu này chỉ ghi nhận các requirement/decision mới hoặc làm rõ baseline phát sinh từ Plan 042.1 và Plan 043.
> Chi tiết class, repository, test class, command validation và cách tổ chức source code vẫn thuộc Dev Plan/Dev Note, không phải nội dung chuẩn tắc của CR này.
>
> Dev Note được dùng như **evidence của trạng thái implementation thực tế**. Khi Dev Note làm rõ một quyết định contract/business behavior mà Dev Plan còn để mở, CR này ghi nhận decision đã triển khai; các chi tiết thuần túy về cấu trúc code vẫn không được nâng thành requirement.

---

## 2. Lý do thay đổi

Baseline hiện tại đã xác định:

- `student_id` là khóa kỹ thuật để liên kết dữ liệu;
- `student_code` là mã học sinh duy nhất và là dữ liệu nghiệp vụ người dùng có thể quan sát;
- `student.user_id` có thể liên kết hồ sơ học sinh với tài khoản;
- hệ thống có role `STUDENT`;
- học sinh không được tự đăng ký tài khoản tự do;
- password/password hash không được trả hoặc lưu trong dữ liệu frontend.

Tuy nhiên baseline chưa định nghĩa đầy đủ hai use case sau:

1. **Người dùng thao tác bằng mã học sinh thay vì phải biết `studentId`.**
   Nhiều contract Enrollment, Scorebook và Attendance hiện dùng `studentId` như input hoặc chỉ hiển thị `studentId`, dù đây là khóa kỹ thuật không phù hợp cho giao diện nghiệp vụ.

2. **Cấp tài khoản đăng nhập ngay trong lúc tạo học sinh.**
   Schema đã cho phép `student.user_id`, nhưng chưa có contract tạo đồng thời `User + role STUDENT + Student + StudentInfo`, chưa có rule sinh username/password mặc định và chưa quy định atomicity của flow này.

CR này bổ sung hai nhóm yêu cầu đó mà **không thay thế `student_id` làm khóa quan hệ trong database** và **không phá vỡ các API Student v1/v2 hiện có**.

---

## 3. Change A — Student Identifier Resolution

### 3.1. Quy ước định danh

Đối với các nghiệp vụ cần xác định một học sinh, contract mới phải hỗ trợ `studentCode` như một định danh nghiệp vụ bên cạnh `studentId`.

Quy tắc phân giải:

| Input | Kết quả |
|---|---|
| Chỉ có `studentId` | Resolve theo ID; giữ backward compatibility |
| Chỉ có `studentCode` | Resolve theo mã học sinh |
| Có cả `studentId` và `studentCode`, cùng chỉ một học sinh | Hợp lệ |
| Có cả hai nhưng không cùng một học sinh | `400 Bad Request` |
| Cả hai đều null/rỗng khi contract yêu cầu định danh | `400 Bad Request` |
| Không tìm thấy học sinh theo định danh đã cung cấp | `404 Not Found` |

### 3.2. Quy tắc source of truth

- `studentId` tiếp tục là **khóa kỹ thuật/canonical foreign key** trong database.
- `studentCode` là **business-facing identifier** để người dùng nhập, tìm kiếm và nhận diện học sinh.
- Việc hỗ trợ `studentCode` ở API **không làm thay đổi foreign key** của Enrollment, Attendance, Scorebook, Transcript hoặc các bảng nghiệp vụ khác.
- `studentCode` phải tiếp tục unique theo baseline hiện hành.
- Khi client cung cấp đồng thời ID và code, hệ thống không được ưu tiên mù quáng một giá trị; phải xác minh tính nhất quán giữa hai định danh.
- Theo implementation đã hoàn thành của Plan 042.1, bulk lookup loại bỏ Student trùng sau khi resolve; các flow Enrollment và bulk score kiểm tra duplicate trước khi bắt đầu mutation để tránh partial mutation do input lặp.

### 3.3. Phạm vi contract bắt buộc hỗ trợ `studentCode`

Các flow sau phải hỗ trợ thao tác bằng `studentCode` theo phạm vi Plan 042.1:

#### Student lookup

Bổ sung contract:

```http
GET /api/v1/students/code/{studentCode}
```

Response sử dụng Student response contract hiện có và không yêu cầu client biết `studentId` trước khi lookup.

#### Enrollment

Các flow xếp lớp đơn/hàng loạt phải cho phép xác định học sinh bằng `studentCode` bên cạnh `studentId`.

Bổ sung contract tra lịch sử phân lớp theo mã học sinh:

```http
GET /api/v2/students/by-code/{studentCode}/enrollments
```

#### Scorebook / Score Entry

- Nhập điểm đơn phải có variant nhận `studentCode`.
- Nhập điểm hàng loạt phải cho phép item xác định học sinh bằng `studentCode`.
- Tạo yêu cầu sửa điểm phải cho phép `studentCode`.
- Bộ lọc yêu cầu sửa điểm phải hỗ trợ `studentCode`.

Contract nhập điểm đơn theo mã:

```http
PUT /api/v2/assessment-columns/{columnId}/students/by-code/{studentCode}/score
```

#### Attendance

Bổ sung các variant thao tác ngoại lệ điểm danh theo mã học sinh:

```http
PUT    /api/v2/attendance/sessions/{sessionId}/exceptions/by-code/{studentCode}
DELETE /api/v2/attendance/sessions/{sessionId}/exceptions/by-code/{studentCode}

PUT    /api/v2/academic-office/attendance/sessions/{sessionId}/exceptions/by-code/{studentCode}
DELETE /api/v2/academic-office/attendance/sessions/{sessionId}/exceptions/by-code/{studentCode}
```

### 3.4. Quy tắc hiển thị danh tính học sinh

Các response nghiệp vụ hướng tới người dùng không nên chỉ trả `studentId` khi học sinh là đối tượng chính của record.

Tối thiểu các response sau phải bổ sung:

```text
studentId
studentCode
studentName
```

Phạm vi được Plan 042.1 ghi nhận gồm:

- Student score response;
- Attendance exception response;
- Score change request summary;
- Score change request detail.

Các thông báo/cảnh báo thiếu dữ liệu điểm của Semester Completeness phải dùng thông tin thân thiện với người dùng, tối thiểu gồm:

```text
studentCode + studentName
```

thay cho thông báo chỉ chứa numeric `studentId`.

### 3.5. Compatibility

- API cũ đang nhận `studentId` phải tiếp tục hoạt động.
- Không migration schema chỉ để hỗ trợ lookup bằng `studentCode`.
- Không thay `student_id` bằng `student_code` trong foreign key hoặc result tables.
- Không yêu cầu FE hiển thị internal ID như định danh chính của học sinh.

---

## 4. Change B — Create Student With Student Account

### 4.1. Use case mới

Bổ sung API mới:

```http
POST /api/v3/students
```

API này tạo trong **một transaction**:

```text
User
  + role STUDENT
  + Student
  + StudentInfo
  + Student.userId -> User.userId
```

Nếu bất kỳ bước bắt buộc nào thất bại, toàn bộ operation phải rollback để không tạo `User` hoặc `Student` mồ côi.

API v1/v2 hiện có không bị thay đổi; flow tạo Student không kèm account vẫn tiếp tục tồn tại.

### 4.2. Request contract

Request v3 giữ các dữ liệu tạo học sinh hiện hành và bổ sung:

```text
username: nullable
password: nullable
```

`nullable` mang nghĩa **không cung cấp giá trị**. Plan 043 chưa chốt rằng blank string được coi như null; vì vậy không được tự động đồng nhất `""`/whitespace với null nếu chưa có requirement bổ sung.

### 4.3. Quy tắc username mặc định

Khi `username == null`, hệ thống sinh username từ `studentName` và `studentCode`.

#### Quy tắc chuẩn

1. Chuyển `studentName` về chữ thường.
2. Loại bỏ dấu Unicode.
3. Loại bỏ khoảng trắng và ký tự không phải chữ/số.
4. Nối với **7 ký tự cuối của `studentCode`**.

Ví dụ:

```text
studentName = "Khánh Duy"
studentCode = "STU1234567"

username = "khanhduy1234567"
```

#### Fallback khi vượt giới hạn hiện tại

Plan 043 được lập theo giới hạn implementation hiện tại:

```text
app_user.user_name: 20 characters
```

Nếu username theo quy tắc chuẩn vượt giới hạn:

1. lấy chữ cái đầu của từng từ trong họ tên;
2. chuẩn hóa thành chữ thường/không dấu;
3. nối 7 ký tự cuối của `studentCode`.

Ví dụ:

```text
"Phạm Trần Khánh Duy" + "STU1234567"
-> "ptkd1234567"
```

Nếu phần viết tắt vẫn làm username vượt giới hạn hiện hành, phần viết tắt phải được giới hạn để tổng username không vượt 20 ký tự.

### 4.4. Duplicate username

- Username phải unique.
- Duplicate username, dù là username client cung cấp hay username sinh tự động, trả:

```http
409 Conflict
```

- Không tự động thêm counter/suffix để né collision nếu chưa có CR khác phê duyệt.
- Khi conflict, transaction không được lưu Student/User một phần.

### 4.5. Quy tắc password mặc định

Khi `password == null`, giá trị ban đầu được Plan 043 chốt là:

```text
12345678
```

Yêu cầu bắt buộc:

- Password phải đi qua `PasswordEncoder` trước khi persistence.
- Không trả plaintext password hoặc password hash trong API response.
- Không log request password hoặc encoded password.
- Không lưu password/password hash trong frontend state.
- CR này **không định nghĩa** cơ chế đổi mật khẩu lần đầu, reset password, cấp lại credential hoặc cách giao mật khẩu ban đầu cho người vận hành/học sinh.

> `12345678` là quyết định chức năng được ghi nhận từ Plan 043. Đây là credential mặc định dùng chung và có rủi ro bảo mật; nếu hệ thống chuyển sang môi trường production thực tế, nên có một CR bảo mật riêng để thay bằng credential ngẫu nhiên/one-time hoặc bắt buộc đổi mật khẩu lần đầu.

### 4.6. Role và liên kết account

- User mới phải được gán role:

```text
STUDENT
```

- Nếu role `STUDENT` không tồn tại trong seed/configuration, operation thất bại với lỗi cấu hình server và toàn bộ transaction rollback.
- `student.userId` phải liên kết với `userId` vừa tạo.
- Flow này không phải self-registration của học sinh; do đó không thay đổi `NFR-SECURITY-002`.
- Theo Dev Note 043 và implementation đã hoàn thành, `POST /api/v3/students` chỉ cho phép role `ADMIN` và `ACADEMIC_OFFICE`; endpoint không được public và role `STUDENT` không được tự gọi flow cấp tài khoản này.

### 4.7. Response contract

Response thành công:

```http
201 Created
```

Phải trả thông tin học sinh và account summary an toàn, tối thiểu:

```json
{
  "studentId": 1,
  "studentCode": "STU1234567",
  "studentName": "Khánh Duy",
  "account": {
    "userId": 10,
    "username": "khanhduy1234567",
    "role": "STUDENT"
  }
}
```

Response **không được chứa**:

```text
password
passwordHash
encodedPassword
```

### 4.8. Failure semantics

| Trường hợp | Kết quả chuẩn tắc |
|---|---|
| Duplicate `studentCode` | `409 Conflict`, không tạo account |
| Duplicate username | `409 Conflict`, rollback toàn bộ |
| Thiếu role `STUDENT` trong configuration/seed | Server configuration error (`500` theo Plan 043), rollback toàn bộ |
| Student request validation không hợp lệ | `400 Bad Request` |
| Lỗi khi lưu Student sau khi đã bắt đầu tạo User | Rollback User và toàn transaction |

---

## 5. Tác động lên baseline

### 5.1. Requirement Baseline

CR này bổ sung các nguyên tắc sau:

1. **Human-facing student identity**
   - `studentCode` là định danh nghiệp vụ ưu tiên cho input/display.
   - `studentId` vẫn là định danh kỹ thuật và foreign-key source of truth.

2. **Flexible identifier input**
   - Các flow được nêu trong Change A có thể resolve bằng ID hoặc code.
   - Nếu cung cấp cả hai phải kiểm tra consistency.

3. **Student account provisioning**
   - Hệ thống hỗ trợ operator tạo Student kèm account `STUDENT` trong một atomic transaction.
   - Đây không phải self-registration.

4. **Credential safety**
   - Password chỉ tồn tại trong request/processing cần thiết, phải hash trước persistence và không được expose trong response/log/frontend state.

### 5.2. Data model

CR này **không yêu cầu migration mới**.

Các cấu trúc đã có đủ để hỗ trợ Plan 043:

```text
app_user
role
user_role
student.user_id
student_info.student_id
```

Plan 042.1 cũng không đổi schema: mọi relation vẫn lưu `student_id`.

### 5.3. Điểm cần đồng bộ giữa tài liệu mục tiêu và implementation hiện tại

`DataStructure-v2` mô tả schema mục tiêu:

```text
app_user.user_name VARCHAR(100)
```

trong khi implementation tại source snapshot của Plan 043 đang giới hạn:

```text
User.username length = 20
```

và Plan 043 đã thiết kế username fallback theo giới hạn 20 ký tự.

Quyết định của CR này:

- **Không tự sửa schema mục tiêu từ 100 xuống 20.**
- Với contract/implementation Plan 043 hiện tại, username sinh tự động phải tương thích giới hạn 20 ký tự.
- Khi project chính thức migrate/đồng bộ `app_user.user_name` sang giới hạn mục tiêu khác, rule fallback/validation username phải được rà soát lại và tài liệu liên quan phải được cập nhật đồng bộ.

---

## 6. Implementation evidence từ Dev Note

### 6.1. Plan 042.1

Dev Note 042.1 xác nhận scope đã `Completed` và behavior thực tế phù hợp với Change A:

- `StudentLookupService` đã resolve được theo `studentId`, `studentCode` hoặc cả hai.
- Mismatch ID/code trả `400 Bad Request`; not-found được xử lý rõ ràng.
- Student lookup by code, Enrollment, Scorebook, Score Change và Attendance by-code đã được triển khai.
- Response Score/Change/Attendance đã được enrich bằng `studentCode` và `studentName`.
- Semester Completeness đã hiển thị `studentCode (studentName)` và có fallback khi hồ sơ không tồn tại.
- Alias attendance by-code được thêm nhưng path cũ vẫn giữ để bảo đảm backward compatibility.
- Không đổi schema hoặc foreign key; quan hệ vẫn dùng `student_id`.
- Bulk resolve loại bỏ Student trùng sau lookup; Enrollment/bulk score kiểm tra duplicate trước mutation.

Validation được Dev Note ghi nhận:

```text
./gradlew test                         PASS
./gradlew checkstyleMain checkstyleTest PASS
./gradlew pmdMain pmdTest              PASS
./gradlew build                        PASS
```

Do đó các requirement ở Change A không còn chỉ là planned contract mà đã có implementation evidence.

### 6.2. Plan 043

Dev Note 043 xác nhận scope đã hoàn thành và làm rõ thêm các decision:

- `POST /api/v3/students` đã được triển khai.
- Account provisioning nằm trong cùng transaction với Student/StudentInfo.
- Endpoint thực tế bị giới hạn cho `ADMIN` và `ACADEMIC_OFFICE`.
- Duplicate explicit/generated username và duplicate student code trả `409 Conflict`.
- Generated username tôn trọng giới hạn 20 ký tự hiện tại và dùng initials fallback khi cần.
- Thiếu seeded role `STUDENT` trả typed `500` **trước user persistence**.
- Password mặc định `12345678` chỉ được lưu sau khi qua `PasswordEncoder`.
- Response chỉ expose `userId`, `username`, `role`; không expose password/hash.
- Existing v1 Student create flow được giữ nguyên.
- Username collision tiếp tục là conflict; không có auto-suffix/retry.

Validation được Dev Note ghi nhận:

```text
./gradlew test            PASS
./gradlew checkstyleMain  PASS
./gradlew pmdMain         PASS
./gradlew build           PASS
```

Không có database migration cho Plan 043 vì implementation tái sử dụng `student.user_id`, `app_user`, `role` và `user_role`.

---

## 7. Compatibility và non-goals

### Compatibility

- Không breaking change các API Student v1/v2.
- Các client đang gửi `studentId` tiếp tục hoạt động.
- Database tiếp tục dùng numeric `student_id` cho PK/FK.
- Existing account/auth JWT flow không bị thay thế.
- `NFR-SECURITY-002` về việc không cho học sinh tự đăng ký tự do vẫn giữ nguyên.
- `NFR-SECURITY-006` về không lưu credential trong frontend vẫn giữ nguyên.

### Non-goals

CR này không định nghĩa:

- đổi/reset/forgot password;
- bắt buộc đổi password lần đầu;
- giao credential ban đầu cho học sinh;
- parent account;
- tự động sửa username khi collision;
- thay đổi schema PK/FK từ `student_id` sang `student_code`;
- thay đổi API v1/v2;
- thay đổi schema chỉ để phục vụ Plan 042.1/043;
- authorization matrix mới cho endpoint v3;
- normalization rule mới ngoài các bước đã chốt trong Plan 043.

---

## 8. Acceptance criteria ở mức requirement

### Student identifier

- [ ] Một API thuộc phạm vi Change A có thể xác định học sinh bằng `studentCode` mà client không cần biết `studentId`.
- [ ] Contract cũ bằng `studentId` vẫn hoạt động.
- [ ] ID + code không cùng một student bị từ chối bằng `400`.
- [ ] Không có định danh bị từ chối bằng `400`.
- [ ] Student không tồn tại trả `404`.
- [ ] Response nghiệp vụ thuộc phạm vi CR có `studentCode` và `studentName`.
- [ ] Semester-completeness warning không còn dùng numeric student ID làm thông tin nhận diện duy nhất.
- [ ] Database relation vẫn lưu `student_id`.

### Student account provisioning

- [ ] `POST /api/v3/students` tạo User + role STUDENT + Student + StudentInfo trong một transaction.
- [ ] Chỉ `ADMIN` và `ACADEMIC_OFFICE` được gọi `POST /api/v3/students` theo authorization đã triển khai.
- [ ] Username null sinh đúng normalization rule.
- [ ] Username dài dùng initials fallback và không vượt giới hạn implementation hiện tại.
- [ ] Password null dùng giá trị mặc định đã được phê duyệt và chỉ persistence sau khi encode.
- [ ] Duplicate username trả `409` và không để lại dữ liệu một phần.
- [ ] Thiếu role `STUDENT` rollback toàn bộ operation.
- [ ] `student.userId` trỏ đúng User vừa tạo.
- [ ] Response chỉ chứa account summary an toàn và không chứa credential.
- [ ] API Student v1/v2 giữ hành vi cũ.

---

## 9. Open decisions / follow-up CR

Các nội dung sau **chưa được Plan 042.1/043 định nghĩa đủ để đóng thành baseline**:

1. **Bulk identifier semantics**
   - Với request hàng loạt có cả collection `studentIds` và `studentCodes`, cần chốt rõ semantics là union, positional pairing hay một cấu trúc item-level thống nhất.
   - Không suy diễn semantics mới chỉ từ việc cả hai collection cùng tồn tại.

2. **Blank credential semantics**
   - Cần chốt `username = ""` hoặc whitespace và `password = ""` có bị validation error hay được coi là missing/null.
   - Hiện tại chỉ `null` kích hoạt rule sinh giá trị mặc định.

3. **Default-password security lifecycle**
   - Cần cân nhắc one-time credential, forced password change hoặc random initial password trước khi dùng ở môi trường production thực tế.

4. **Username length source of truth**
   - Cần đồng bộ giới hạn giữa implementation hiện tại (20) và schema mục tiêu trong `DataStructure-v2` (100), sau đó rà soát lại validation và generation rule.

---

## 10. Traceability

| CR section | Source |
|---|---|
| Change A — ID/code resolution | Plan 042.1 + Dev Note 042.1 |
| Student lookup by code | Plan 042.1 |
| Bulk dedup + duplicate-before-mutation behavior | Dev Note 042.1 |
| Enrollment/Scorebook/Attendance by-code contracts | Plan 042.1 |
| Response `studentCode` + `studentName` | Plan 042.1 |
| Human-readable completeness notification | Plan 042.1 |
| `POST /api/v3/students` | Plan 043 + Dev Note 043 |
| `ADMIN`/`ACADEMIC_OFFICE` authorization cho Student v3 | Dev Note 043 |
| Username generation + initials fallback | Plan 043 |
| Default password `12345678` + encoding | Plan 043 |
| Duplicate username `409` | Plan 043 |
| Role `STUDENT` + atomic rollback | Plan 043 + Dev Note 043 |
| Safe account response | Plan 043 + Dev Note 043 |
| v1/v2 backward compatibility | Plan 042.1/043 + Dev Note 042.1/043 |
| No schema migration | Plan 042.1/043 + Dev Note 042.1/043 |

---

## 11. Recommended placement in repository

Nếu đưa CR này vào branch tài liệu, vị trí đề xuất:

```text
document/application-doc/v2/change-request/
└── CR-STUDENT-001-student-identifier-and-account-provisioning.md
```

Các module document nên liên kết tới CR này thay vì copy toàn bộ rule, đặc biệt:

```text
modules/StudentModule.md
modules/UserModule.md
modules/02-EnrollmentAndTeachingModule.md
modules/03-AttendanceAndSubjectModule.md
modules/04-AssessmentAndScoringModule.md
modules/05-ScoreChangeAndCalculationModule.md
```

CR này là source ghi nhận delta. Sau khi baseline/module docs được cập nhật chính thức, các Dev Plan tiếp theo nên tham chiếu requirement từ application-doc/CR thay vì chỉ tham chiếu ngược vào Plan 042.1/043.

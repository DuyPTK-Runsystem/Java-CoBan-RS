# Developer Plan 048: BE Demo Data Bootstrap

## 1. Trạng thái và phiên bản áp dụng

- **Status**: `Completed` — đã được người dùng phê duyệt bằng tin nhắn `approve` và triển khai xong.
- **Application-document version**: `v2`.
- **Ngày lập plan**: `2026-08-26`.
- **Module**: Backend bootstrap / identity / academic catalog / enrollment / teaching assignment.
- **Phụ thuộc**:
  - Flyway migrations `V1` đến `V18`;
  - Plan 025 (contract và migration scope freeze);
  - Plan 026 (enrollment và class placement);
  - Plan 027 (subject và teacher assignment);
  - Plan 034 (teacher account và role assignment);
  - Plan 043 (student account provisioning).

## 2. Mục tiêu

Chuẩn bị bộ dữ liệu synthetic, ổn định và có thể kiểm tra trên DB MySQL mới hoàn toàn để phục vụ phát triển, manual testing và các luồng backend tiếp theo của Plan 048.

Dữ liệu phải được tạo theo đúng schema v2, bảo đảm quan hệ khóa ngoại, unique constraint, role authorization và đăng nhập được bằng các account mẫu.

## 3. Quyết định dữ liệu đã chốt

### 3.1. Account và password

- Tài khoản quản trị bắt buộc khi khởi tạo DB:
  - username: `admin`;
  - password logic: `admin`;
  - role: `ADMIN`.
- Tài khoản `ACADEMIC_OFFICE`: 1 account, username `academic.office`.
- Tài khoản `TEACHER`: 20 account, username `teacher01` đến `teacher20`.
- Tài khoản `STUDENT`: 32 account, 4 học sinh cho mỗi 8 lớp.
- Mỗi account `ACADEMIC_OFFICE` phải được tạo trong `app_user` và được liên kết với role `ACADEMIC_OFFICE`.
- Mỗi hồ sơ `teacher` phải có `user_id` liên kết tới một `app_user` tương ứng và được gán role `TEACHER`.
- Không tạo teacher hoặc academic office dạng hồ sơ nghiệp vụ không có account đăng nhập.
- Các account ngoài `admin` dùng password logic `12345678`.
- Mọi password phải được mã hóa bằng BCrypt trước khi lưu; DB không lưu plaintext.
- Seed phải không tạo bản ghi trùng khi khởi động hoặc chạy lại fixture.

### 3.2. Năm học, học kỳ và lớp

- Năm học: `2026-2027`, trạng thái `ACTIVE`.
- Học kỳ 1:
  - tên: `HK1 2026 - 2027`;
  - thời gian: `2026-09-01` đến `2026-12-31`;
  - trạng thái: `ACTIVE`.
- Học kỳ 2:
  - tên: `HK2 2026 - 2027`;
  - thời gian: `2027-01-01` đến `2027-05-31`;
  - trạng thái: `DRAFT`.
- Khối: 6, 7, 8, 9.
- Lớp: `6A1`, `6A2`, `7A1`, `7A2`, `8A1`, `8A2`, `9A1`, `9A2`.
- Mỗi lớp có 4 học sinh và một enrollment trong năm học.

### 3.3. Môn học

Mười một môn phổ thông có loại `ACADEMIC`:

- Toán;
- Vật lí;
- Hóa học;
- Sinh học;
- Ngữ Văn;
- Ngoại ngữ;
- Lịch sử;
- Địa lí;
- Giáo dục công dân;
- Tin học;
- Công nghệ.

Hai môn nghề có loại `SKILL`:

- Nghề phổ thông - Điện dân dụng;
- Nghề phổ thông - Nông nghiệp.

Quy tắc áp dụng:

- Môn phổ thông áp dụng cho các khối phù hợp ở cả hai học kỳ.
- Hai môn nghề áp dụng cho khối 8 và 9, chỉ ở `HK2`.
- Công nghệ khối 9 chỉ áp dụng ở `HK1`.
- Các môn kỹ năng dùng trọng số `KTTT 25% - KTDK 35% - KTCK 40%`.
- Môn phổ thông dùng hệ số `KTTX 1 - KĐK 2 - KTCK 3` theo baseline v2.

## 4. Phạm vi

### 4.1. In-scope

1. Bảo đảm account `admin` tồn tại trên DB mới và có role `ADMIN`.
2. Seed account giáo vụ, 20 giáo viên và 32 học sinh.
3. Seed teacher profile, student profile và `student_info` cần thiết; mọi teacher và academic office đều có liên kết `app_user` hợp lệ.
4. Seed academic year, semester, grade level và school class.
5. Seed student enrollment cho 32 học sinh.
6. Seed 8 homeroom assignment với 8 giáo viên khác nhau; mỗi giáo viên chỉ làm GVCN một lớp và vẫn có thể đồng thời được phân công dạy môn.
7. Seed class subject và subject teaching assignment theo danh mục môn.
8. Cho phép giáo viên dạy nhiều môn, bao gồm các nhóm dữ liệu kiểm thử:
   - Nghề phổ thông - Điện dân dụng và Nghề phổ thông - Nông nghiệp;
   - Lịch sử và Địa lí.
9. Kiểm tra trên DB MySQL sạch, kiểm tra FK/unique constraint và đăng nhập account mẫu.

### 4.2. Out-of-scope của plan này

- Không tạo hoặc cấu hình `scorebook` và `assessment_column` ở giai đoạn này.
- Không seed `student_score`, score change request, calculation task hoặc transcript result.
- Không seed retake exam, attendance session/record hoặc notification.
- Không triển khai UI/frontend.
- Không thay đổi công thức tính điểm hay public API nghiệp vụ hiện có.

Scorebook và điểm mẫu sẽ là phạm vi tiếp theo sau khi bộ dữ liệu nền được xác nhận ổn định.

## 5. Thiết kế bootstrap đề xuất

### 5.1. Tách mandatory seed và demo fixture

- **Mandatory system seed**: tạo `admin` tự động sau khi schema và role seed hoàn tất.
- **Demo fixture**: tạo giáo vụ, giáo viên, học sinh, academic catalog, enrollment và assignment bằng bước bootstrap riêng, có thể chạy lại an toàn.
- Không tạo lại role đã có từ `V3__create_roles_and_assign_legacy_administrators.sql`.
- Không gán tất cả user mới thành `ADMIN`; mỗi user chỉ nhận role đúng loại.

### 5.2. Password handling

- Reuse `PasswordEncoder`/BCrypt configuration hiện có.
- Không hard-code plaintext password trong cột database.
- Không trả `password` hoặc password hash trong response DTO.
- Tài liệu test có thể ghi password logic mẫu để đăng nhập local, nhưng không dùng dữ liệu thật.

### 5.3. Naming và mapping ổn định

- Teacher username: `teacher01` … `teacher20`.
- Student username: `student.6a1.01` … `student.9a2.04`.
- Student code dùng format hợp lệ hiện tại và phải duy nhất.
- Teacher code và subject code dùng giá trị deterministic để các test/collection có thể tham chiếu ổn định.
- 8 giáo viên trong số 20 giáo viên được chọn làm GVCN cho 8 lớp; cả 8 giáo viên này vẫn được tham gia subject assignment như GVBM. 12 giáo viên còn lại cũng được phân công dạy môn.
- Mỗi teacher profile phải có đúng một liên kết `app_user`; role authorization được lấy từ account liên kết.

## 6. Khu vực/file dự kiến thay đổi

Phương án file cụ thể sẽ được chốt khi triển khai sau approval. Dự kiến ảnh hưởng:

- `BE/BaiTap-RS/src/main/resources/db/migration/`: migration mandatory admin nếu cần.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/`: bootstrap/seeder dùng lại repository và BCrypt hiện có nếu fixture được triển khai ở application layer.
- `BE/BaiTap-RS/src/test/`: test khởi tạo DB sạch, seed count, FK/unique và login.
- `scripts/` hoặc thư mục fixture tương ứng: demo data runner nếu chọn cơ chế script riêng.
- `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md` và `document/dev-impl-plan/be/BE_DEV_PLAN_SUMMARY.md`.

Không chỉnh sửa migration cũ `V1`–`V17`.

## 7. Kế hoạch kiểm tra

- Chạy toàn bộ Flyway migrations trên MySQL database mới.
- Xác nhận có đúng 4 role nền và đúng 1 account `admin`.
- Đăng nhập kiểm tra:
  - `admin` / `admin`;
  - `academic.office` / `12345678`;
  - một teacher account / `12345678`;
  - một student account / `12345678`.
- Kiểm tra số lượng: 1 academic office, 20 teachers, 32 students, 4 grade levels, 8 classes.
- Kiểm tra 1 academic office và 20 teacher profile đều có `app_user` liên kết hợp lệ, đúng role và đăng nhập được.
- Kiểm tra mỗi lớp có 4 enrollment và một GVCN duy nhất.
- Kiểm tra môn nghề chỉ xuất hiện ở khối 8/9 và `HK2`.
- Kiểm tra Công nghệ khối 9 chỉ xuất hiện ở `HK1`.
- Kiểm tra BCrypt bằng xác thực qua application, không so sánh plaintext trong DB.
- Nếu có thay đổi backend production/test: chạy test, Checkstyle, PMD và build theo workflow backend.

## 8. Rủi ro và điểm cần xác nhận

- Seed demo tự động trong mọi môi trường có thể làm production xuất hiện dữ liệu mẫu; vì vậy demo fixture được đề xuất tách khỏi mandatory admin seed.
- Trạng thái `ACTIVE` của năm học và `HK1`, `DRAFT` của `HK2` là giả định phục vụ dữ liệu khởi tạo, cần người dùng xác nhận nếu muốn khác.
- Cơ chế đã chốt: mandatory `admin` bằng Flyway migration V18; demo fixture bằng application seeder bật có điều kiện qua `app.seed.demo.enabled`.

## 9. Output dự kiến

1. DB mới có thể khởi tạo ổn định từ schema v2.
2. Account `admin` luôn tồn tại và đăng nhập được bằng `admin/admin`.
3. Có bộ dữ liệu nền gồm năm học, học kỳ, khối, lớp, môn, giáo viên, học sinh, enrollment và assignment.
4. Có fixture deterministic để các bước seed scorebook và điểm mẫu sau này tham chiếu được.
5. Có validation evidence và Dev Note ghi nhận kết quả thực tế.

## 10. Kết quả triển khai và validation

- Đã triển khai migration `V18__seed_default_admin.sql` để bảo đảm `admin/admin` và role `ADMIN` tồn tại sau migration sạch; password được lưu dưới dạng BCrypt.
- Đã triển khai application seeder có điều kiện tại `DemoDataSeeder`; mặc định tắt (`app.seed.demo.enabled=false`) để không tự đưa demo data vào môi trường ngoài ý muốn.
- Fixture chạy idempotent, gồm 1 giáo vụ, 20 giáo viên, 32 học sinh, 4 khối, 8 lớp, 32 enrollment, 8 GVCN, 91 applicability, 182 class subject và 182 subject teaching assignment.
- Teacher profile luôn có `user_id`; academic office là `app_user` có role `ACADEMIC_OFFICE`; teacher có role `TEACHER`; student có role `STUDENT`.
- Đã xác nhận bằng H2/Flyway clean migration và Spring integration test; kiểm thử MySQL container thực tế chưa chạy trong phiên này vì Docker service không khả dụng.
- Validation Result: `test PASS`, `Checkstyle PASS`, `PMD PASS`, `build PASS`.

## 11. Approval gate

Plan 048 đã được người dùng phê duyệt bằng tin nhắn `approve` trước khi triển khai.

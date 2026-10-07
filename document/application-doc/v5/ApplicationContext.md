# Application Context v5 — Library Management

## 1. Mục tiêu

v5 mở rộng Java-CoBan-RS từ school-management sang thêm vận hành thư viện trường học mà không tạo một application độc lập.

Library là bounded context mới nhưng chạy trong cùng:

- Spring Boot application;
- MySQL database;
- Flyway migration stream;
- Spring Security/JWT infrastructure;
- role model và `app_user`;
- audit/error conventions;
- notification/mail infrastructure;
- Spring Batch infrastructure;
- Spring AI abstraction;
- Vue 3/Vite/PrimeVue frontend;
- deployment pipeline hiện hành.

## 2. Kiến trúc mục tiêu

```text
Vue 3 / PrimeVue / existing router & service conventions
                         |
                   REST /api/v2
                         |
Spring Boot 4 / existing Security + audit + error handling
                         |
  ---------------------------------------------------------
  |          |          |          |          |           |
catalog    patron   circulation   fine/batch  code       library-ai
  |          |          |          |          |           |
  -------------------- MySQL / JPA ------------------------
                         |
                existing platform domains
        app_user | student | teacher | notification
```

Không có `library-service`, database riêng hoặc login riêng trong baseline.

## 3. Identity và Library Patron

`app_user` tiếp tục là identity đăng nhập.

`student` và `teacher` tiếp tục sở hữu hồ sơ học sinh/giáo viên.

Library thêm `library_patron` chỉ để lưu state thuộc thư viện:

```text
APP_USER 1 ---- 0..1 LIBRARY_PATRON
     |
     +---- 0..1 STUDENT
     |
     +---- 0..1 TEACHER
```

`library_patron` không duplicate:

- username/password;
- họ tên;
- email/phone nếu đã có nguồn canonical;
- student code;
- teacher code;
- application roles.

Library service resolve display data từ domain hiện hữu qua application boundary. Không tạo aggregate `MEMBER` độc lập chứa lại identity.

## 4. Role model

Roles hiện hữu tiếp tục có ý nghĩa platform:

- `ADMIN`
- `ACADEMIC_OFFICE`
- `TEACHER`
- `STUDENT`

v5 bổ sung:

- `LIBRARIAN`

`LIBRARIAN` là application role cho staff vận hành thư viện.

`MEMBER` của spec standalone **không** được thêm làm role. “Bạn đọc” được biểu diễn bằng `library_patron`.

Một `STUDENT` hoặc `TEACHER` có `library_patron` ACTIVE có thể sử dụng borrower capabilities.

## 5. Borrowing suspension không khóa application account

Nợ thư viện không được làm `app_user` bị disable/lock.

```text
LibraryPatronStatus:
ACTIVE
BORROWING_SUSPENDED
CLOSED
```

`BORROWING_SUSPENDED` chỉ chặn những operation Library được policy quy định, tối thiểu là tạo khoản mượn mới và gia hạn nếu business rule yêu cầu.

Học sinh/giáo viên vẫn phải đăng nhập và sử dụng các module:

- điểm;
- điểm danh;
- thời khóa biểu;
- thông báo;
- các chức năng học vụ khác.

## 6. Persistence baseline

v5 dùng MySQL và Flyway hiện hữu.

Tại thời điểm tạo baseline, migration stream hiện hữu đã tới `V29`; v5 implementation phải scan lại migration head trước khi tạo file mới và tiếp tục sequence, không giả định cứng `V30` nếu branch đã thay đổi.

Không copy các PostgreSQL-only artifacts từ source training:

- partial unique index;
- `varchar[]`;
- GIN;
- `to_tsvector`;
- `ON CONFLICT`;
- PostgreSQL Testcontainers.

## 7. API versioning

API mới mặc định ở `/api/v2`, đồng bộ convention đang tồn tại trong repo, nơi `/api/v1/students`, `/api/v2/students`, `/api/v3/students` có thể cùng hoạt động.

REST API version không phụ thuộc `v5` document version.

## 8. AI baseline

Library AI phải dùng Spring AI abstraction hiện hữu.

Không thêm direct Anthropic/OpenAI/vendor SDK chỉ vì source training nêu một provider cụ thể.

Quy tắc:

- model không có repository/SQL/direct database write;
- machine-readable output phải có structured contract;
- backend validate và thực hiện query/mutation;
- AI failure không được chặn luồng mượn/trả cốt lõi;
- provider/model/configurable;
- test không gọi Internet.

## 9. Frontend baseline

Library tích hợp vào FE hiện hữu:

- Vue 3 + Vite + TypeScript;
- PrimeVue;
- Vue Router;
- Storybook;
- Vitest;
- service boundary hiện hành.

Pinia/Axios không được thêm chỉ để khớp source training. Chỉ thêm dependency nếu Developer Plan chứng minh cần thiết.

## 10. Ngoài phạm vi v5 baseline

- thanh toán online thật;
- đa chi nhánh;
- microservices;
- Kubernetes;
- semantic vector search/pgvector;
- TOTP/dynamic QR;
- thay thế login system;
- parent-as-library-patron;
- tự động tạo borrower cho mọi account mà không có lifecycle rõ ràng.

Các mục này cần CR/amendment riêng.

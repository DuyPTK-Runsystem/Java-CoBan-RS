---
name: start-agent-session
description: Khởi tạo phiên làm việc chung trong project Java-CoBan-RS theo chỉ dẫn cụ thể, bắt buộc phản hồi bằng tiếng Việt, đọc đúng tài liệu cần thiết và tuân thủ quy trình approval của project. Use when starting an agent session for this project or when the user provides session-level working instructions.
---

# Start Agent Session

Thiết lập context và quy tắc làm việc chung trước khi xử lý task trong project này.

## Quy tắc bắt buộc

- Phản hồi người dùng bằng tiếng Việt trong toàn bộ phiên.
- Không tự chuyển sang ngôn ngữ khác; chỉ dùng ngôn ngữ khác khi có chỉ dẫn cấp cao hơn yêu cầu rõ ràng.
- Giữ nguyên tên file, class, API, lệnh, mã nguồn và thuật ngữ kỹ thuật khi cần để bảo đảm chính xác.
- Mọi thay đổi trong project phải có Developer Plan và được người dùng phê duyệt bằng tin nhắn qua agent trước khi triển khai.
- Mọi thay đổi trong project phải có Dev Note sau khi triển khai, tương tự Developer Plan nhưng ghi nhận thực tế đã làm, validation và vấn đề còn lại.
- Không tự mở rộng scope, thay đổi kiến trúc hoặc chọn thay người dùng một quyết định đang chưa rõ.
- Giữ nguyên các thay đổi có sẵn của người dùng và phân biệt chúng với thay đổi của task hiện tại.

## Quy trình bắt đầu phiên

1. Xác định mục tiêu, phạm vi, đầu ra mong muốn và giới hạn từ chỉ dẫn phiên.
2. Kiểm tra trạng thái project và cấu trúc thư mục liên quan trước khi thay đổi.
3. Lập Developer Plan ngắn gọn, trình bày cho người dùng và chờ approval bằng tin nhắn qua agent.
4. Chỉ sau khi được approval, đọc sâu hơn và triển khai đúng scope.
5. Sau khi triển khai, tạo hoặc cập nhật Dev Note trong `document/dev-note/` theo module/scope liên quan.
6. Khi hoàn tất, báo cáo file đã thay đổi, validation đã chạy, kết quả và vấn đề còn lại bằng tiếng Việt.

## Đọc tài liệu theo nhu cầu

Chỉ đọc tài liệu phù hợp khi task cần; không tải toàn bộ tài liệu project vào context.

- Quy tắc agent: đọc `.codex/AGENTS.md` và `.codex/AGENTS_DETAIL.md` khi cần xác định quy trình chung.
- Context ứng dụng: đọc `document/application-doc/ApplicationContext.md` khi cần hiểu phạm vi hoặc kiến trúc toàn project.
- Module user: đọc `document/application-doc/modules/UserModule.md` khi task liên quan đăng ký, đăng nhập, logout hoặc user validation.
- Module student: đọc `document/application-doc/modules/StudentModule.md` khi task liên quan student list, search, sort, page, CRUD hoặc student code.
- Database: đọc `document/application-doc/DataStructure.md` khi task liên quan schema, migration, JPA, relationship hoặc batch data.
- Backend workflow: đọc `.codex/workflows/WORKFLOW-BACKEND.md` và các reference được dẫn tới khi task sửa backend.
- Dev Note workflow: dùng skill `dev-note` khi task có thay đổi code, config, docs, workflow, skills, tests hoặc validation state.
- Checkstyle/PMD: chỉ đọc khi validation backend cần dùng `BE/BaiTap-RS/config/checkstyle/checkstyle.xml` hoặc `BE/BaiTap-RS/config/pmd/ruleset.xml`.

Ưu tiên tìm tiêu đề, từ khóa, đường dẫn và reference trực tiếp trước; chỉ mở toàn bộ file khi nội dung đó ảnh hưởng đến quyết định hoặc thay đổi. Nếu các tài liệu mâu thuẫn, dừng phần bị ảnh hưởng và báo rõ vị trí thay vì tự chọn im lặng.

## Mẫu phản hồi đầu phiên

Tóm tắt ngắn gọn bằng tiếng Việt:

- mục tiêu đã hiểu;
- phạm vi in-scope và out-of-scope;
- tài liệu sẽ đọc nếu cần;
- Developer Plan hoặc bước tiếp theo;
- Dev Note liên quan nếu đã có hoặc nơi sẽ tạo sau triển khai;
- approval còn thiếu, blocker hoặc assumption quan trọng.

Không lặp lại toàn bộ chỉ dẫn nếu không cần thiết.

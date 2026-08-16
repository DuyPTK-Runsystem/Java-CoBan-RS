# AGENTS_DETAIL.md

Tài liệu này mô tả quy trình tham chiếu cho các task triển khai không đơn giản.
Các `AGENTS.override.md` có thể bổ sung hoặc thu hẹp quy trình này cho từng khu vực.

## 1. Developer Plan tối thiểu

Plan nên bao gồm:
- Mục tiêu.
- Requirement liên quan.
- In-scope / out-of-scope.
- Kiến trúc hoặc luồng hiện tại.
- Phương án triển khai.
- Module và integration bị ảnh hưởng.
- File hoặc khu vực dự kiến thay đổi.
- API, schema hoặc dữ liệu thay đổi nếu có.
- Test và validation dự kiến.
- Rủi ro, assumption và output mong đợi.

Với thay đổi nhỏ, hiển nhiên và ít rủi ro, vẫn phải có plan ngắn và được người dùng phê duyệt bằng tin nhắn qua agent; không cần tạo plan dài.

## 2. Báo cáo trước khi code

Trước khi triển khai một thay đổi đã có plan, Agent nên tóm tắt:
- Kiến trúc hoặc flow hiện tại.
- Feature/hành vi sẽ thay đổi.
- Vị trí requirement liên quan.
- Module, file, interface, function hoặc component bị ảnh hưởng.
- Output dự kiến.
- Rủi ro hoặc compatibility concern quan trọng.
- Cách validate.

Giữ báo cáo ngắn và đúng trọng tâm.

## 3. Trong quá trình code

- Bám đúng plan và phạm vi.
- Không thêm thay đổi ngoài task nếu chưa được duyệt.
- Ưu tiên reuse code hiện có.
- Giữ thay đổi nhỏ, rõ ràng và dễ review.
- Không dùng trial-and-error mù quáng.
- Khi debug, ưu tiên log, test, stack trace và hành vi có thể tái hiện.
- Nếu một thay đổi thử nghiệm không giúp ích, không giữ lại nó.

## 4. Validation

Thực hiện các kiểm tra phù hợp với khu vực thay đổi, ví dụ:
- formatter;
- linter;
- static analysis;
- type checking;
- build;
- unit/integration test;
- UI test;
- contract/schema validation.

Tên command cụ thể phải được định nghĩa trong `AGENTS.override.md` hoặc tài liệu của module.

Nếu lỗi đến từ code cũ ngoài phạm vi:
- ghi nhận rõ;
- không tự ý refactor;
- không coi đó là lỗi do task hiện tại tạo ra.

## 5. Khi bị blocker

Nếu không thể tiếp tục một cách đáng tin cậy:
- nêu lỗi còn tồn tại;
- nguyên nhân đã xác định;
- các phương án quan trọng đã thử;
- phần đã hoàn thành;
- phần chưa hoàn thành;
- thông tin còn thiếu;
- bước tiếp theo được đề xuất.

Không tiếp tục thử ngẫu nhiên chỉ để tạo cảm giác có tiến triển.

## 6. Báo cáo sau khi code

Báo cáo cuối nên có:
- Tóm tắt thay đổi.
- File/khu vực đã chỉnh sửa.
- Requirement đã đáp ứng.
- Validation/test và kết quả.
- Deviations so với plan.
- Known issue hoặc limitation còn lại.

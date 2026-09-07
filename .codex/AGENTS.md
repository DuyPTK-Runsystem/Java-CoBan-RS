# AGENTS.md

## Mục đích
File này định nghĩa các nguyên tắc chung cho AI Agent trong toàn repository.

Quy tắc tại đây phải:
- dùng được cho cả Frontend và Backend;
- không phụ thuộc framework, ngôn ngữ hay tool cụ thể;
- được bổ sung bằng `AGENTS.override.md` ở từng khu vực khi cần.

Quy trình triển khai chi tiết: `.codex/AGENTS_DETAIL.md`.

## 1. Đọc trước khi sửa
Trước khi tạo, sửa, di chuyển hoặc xóa mã nguồn, Agent phải:
1. Đọc `AGENTS.md` và `AGENTS.override.md` áp dụng cho khu vực đang làm việc.
2. Đọc tài liệu liên quan đến requirement, architecture, API, database và design.
3. Kiểm tra implementation hiện tại và các pattern lân cận.
4. Xác định module, file, interface và hành vi bị ảnh hưởng.
5. Ưu tiên thông tin có thể kiểm chứng trong repository thay vì tự suy đoán.

Không tự ý giả định requirement hoặc kiến trúc nếu có thể xác định từ tài liệu hoặc mã nguồn hiện có.

## 2. Developer Plan và approval
Với mọi thay đổi, Agent phải kiểm tra hoặc lập Developer Plan trước khi code.
Plan chỉ được xem là approved khi người dùng xác nhận bằng tin nhắn qua agent.

Nếu đã có plan được phê duyệt:
- đọc đầy đủ phần liên quan;
- kiểm tra plan còn phù hợp với mã nguồn hiện tại;
- triển khai đúng phạm vi và hướng kỹ thuật đã duyệt.

Nếu chưa có plan được phê duyệt:
- phân tích requirement và implementation hiện tại;
- lập plan theo `.codex/AGENTS_DETAIL.md` và `.codex/workflows/references/DEVELOPER_PLAN.md`;
- trình bày cho người dùng;
- chỉ bắt đầu code sau khi người dùng phê duyệt rõ ràng bằng tin nhắn qua agent.

Không được âm thầm thay đổi kiến trúc, phạm vi hoặc quyết định kỹ thuật đã được duyệt.

## 3. Giữ đúng phạm vi
Chỉ thay đổi những gì cần thiết để hoàn thành yêu cầu đã được duyệt.

Không tự ý:
- thêm feature ngoài yêu cầu;
- refactor diện rộng hoặc đổi tên hàng loạt;
- format toàn bộ file không liên quan;
- thay đổi public contract khi không cần thiết;
- cập nhật dependency không có lý do;
- thay đổi build, deploy, infrastructure hoặc CI/CD;
- sửa code cũ không liên quan chỉ vì thấy có thể cải thiện.

Ưu tiên thay đổi nhỏ nhất nhưng vẫn hoàn chỉnh và nhất quán.
Nếu cần vượt phạm vi, phải giải thích lý do, ảnh hưởng và xin phê duyệt trước.

## 4. Chất lượng triển khai
Mã nguồn tạo mới hoặc chỉnh sửa phải:
- đúng requirement;
- phù hợp kiến trúc và convention hiện tại;
- dễ đọc, dễ bảo trì;
- hạn chế duplication và dependency không cần thiết;
- không làm thay đổi hành vi ngoài phạm vi;
- xử lý lỗi và edge case hợp lý;
- cân nhắc security, privacy, accessibility, data integrity và performance khi phù hợp;
- ưu tiên tái sử dụng abstraction hoặc utility hiện có.

Không tắt rule, suppress warning hoặc bỏ qua safeguard chỉ để làm validation pass.

## 5. Convention và công cụ
Tuân thủ convention và tooling của khu vực đang chỉnh sửa.

Các quy định cụ thể về formatter, linter, static analysis, type checking, test, build, naming,
dependency, API/schema, UI và security nên đặt trong `AGENTS.override.md` gần mã nguồn tương ứng.

Root `AGENTS.md` không chứa command hoặc rule riêng của FE/BE.

## 6. Validation
Trước khi báo hoàn thành:
1. Chạy các kiểm tra liên quan được quy định cho khu vực thay đổi.
2. Sửa lỗi do thay đổi hiện tại gây ra.
3. Chạy lại kiểm tra sau khi sửa.
4. Phân biệt lỗi có sẵn và lỗi mới.
5. Không mở rộng phạm vi chỉ để sửa lỗi cũ ngoài task nếu chưa được duyệt.

Không báo “hoàn thành”, “đã fix” hoặc “đã verify” khi kiểm tra bắt buộc vẫn thất bại.
Nếu không thể chạy một kiểm tra, phải nêu rõ kiểm tra nào chưa chạy và lý do.

## 7. Khi plan không còn phù hợp
Nếu trong lúc triển khai phát hiện plan không còn đúng:
1. dừng phần công việc bị ảnh hưởng;
2. mô tả phát hiện mới và ảnh hưởng;
3. đề xuất phương án thay thế;
4. cập nhật plan khi cần;
5. chờ phê duyệt trước khi tiếp tục thay đổi đáng kể.

Không tự ý drift khỏi thiết kế đã duyệt.

## 8. Báo cáo kết quả
Sau khi hoàn thành, báo cáo ngắn gọn:
- đã triển khai gì;
- requirement nào đã đáp ứng;
- file/khu vực nào thay đổi và mục đích;
- validation/test đã chạy và kết quả;
- khác biệt so với plan nếu có;
- vấn đề còn tồn tại hoặc giới hạn hiện tại.

Báo cáo phải phản ánh đúng trạng thái thực tế của repository.

## 9. Thứ tự ưu tiên
Khi có xung đột, áp dụng theo thứ tự:
1. Chỉ dẫn trực tiếp mới nhất của người dùng.
2. `AGENTS.override.md` hoặc `AGENTS.md` gần mã nguồn nhất.
3. Root `AGENTS.md`.
4. Developer Plan đã được phê duyệt.
5. Requirement và tài liệu kỹ thuật.
6. Convention và pattern hiện có trong mã nguồn.

Nếu mâu thuẫn quan trọng không thể giải quyết an toàn, phải báo rõ thay vì tự đoán.

## 10. Nguyên tắc chung
- Ưu tiên bằng chứng hơn giả định.
- Ưu tiên thay đổi nhỏ, dễ review và dễ rollback.
- Giữ nguyên hành vi hiện có nếu requirement không yêu cầu thay đổi.
- Không che giấu failure, skipped check, assumption hoặc unresolved risk.
- Không khẳng định thành công nếu chưa verify.
- Giữ plan và báo cáo tương xứng với độ phức tạp của task.

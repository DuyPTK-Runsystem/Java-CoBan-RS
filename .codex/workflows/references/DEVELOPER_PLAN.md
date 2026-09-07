# DEVELOPER_PLAN.md

## Mục đích
Reference này quy định nội dung tối thiểu của Developer Plan trước khi AI Agent triển khai code.

## Khi nào phải tạo plan
Tạo Developer Plan cho mọi thay đổi. Với thay đổi nhỏ, plan có thể ngắn gọn.

Plan cần được tạo lại hoặc cập nhật khi:
- feature/module chưa có plan;
- plan hiện tại chưa được phê duyệt;
- scope thay đổi đáng kể;
- implementation hiện tại khiến plan cũ không còn khả thi;
- requirement có nhiều phương án kỹ thuật cần người dùng quyết định.

## Nội dung tối thiểu
Developer Plan phải có:

### 1. Mục tiêu
- Vấn đề cần giải quyết.
- Kết quả mong muốn.

### 2. Requirement liên quan
- Requirement trực tiếp.
- Business rule hoặc constraint liên quan.

### 3. Phạm vi
- In-scope.
- Out-of-scope.
- Những phần không được thay đổi.

### 4. Kiến trúc hiện tại
- Module/service/component liên quan.
- Luồng xử lý hiện tại.
- Dependency và integration quan trọng.

### 5. Phương án triển khai
- Thiết kế đề xuất.
- Luồng xử lý sau thay đổi.
- Lý do chọn phương án.
- Trade-off quan trọng.

### 6. Phạm vi mã nguồn
Với mỗi file dự kiến:
- đường dẫn;
- tạo mới hay chỉnh sửa;
- class/interface/component;
- method/function hoặc khu vực thay đổi;
- mục đích thay đổi.

### 7. API / Database / Integration
Nếu có, nêu rõ:
- endpoint/request/response;
- schema/migration/query;
- message/event;
- external service;
- compatibility impact.

### 8. Test và validation
- Unit test.
- Integration test.
- Case thành công.
- Case lỗi.
- Checkstyle/PMD/build cần chạy.

### 9. Rủi ro
- Regression.
- Data integrity.
- Security.
- Performance.
- Backward compatibility.
- Phương án giảm thiểu.

### 10. Output dự kiến
Mô tả kết quả quan sát được sau khi hoàn thành.

## Approval gate
Plan chỉ được coi là hợp lệ sau khi người dùng đã phê duyệt rõ ràng bằng tin nhắn qua agent. Quy tắc này áp dụng cho mọi thay đổi, không phụ thuộc kích thước task.

Trước khi được phê duyệt:
- không sửa mã nguồn;
- không triển khai một phần feature;
- không tự chọn phương án thay người dùng.

Nếu plan cần thay đổi trong quá trình coding:
1. dừng phần bị ảnh hưởng;
2. báo nguyên nhân và tác động;
3. đề xuất plan cập nhật;
4. chờ phê duyệt lại trước khi tiếp tục.

---
name: before-backend-report
description: Kiểm tra Validation Result và Dev Note trước khi agent báo cáo kết quả code backend. Use after backend-validation and before reporting success or completion.
---

# Before Backend Report

Đóng vai trò quality gate cuối cùng trước báo cáo backend.

## Điều kiện bắt buộc

- Đã có Developer Plan và người dùng đã phê duyệt bằng tin nhắn qua agent.
- Đã nhận `Validation Result` từ `backend-validation`.
- Đã tạo hoặc cập nhật Dev Note liên quan trong `document/dev-note/`.
- Dev Note ghi đúng lệnh, trạng thái và kết quả thực tế từ `Validation Result`.

Không báo cáo backend là hoàn thành hoặc thành công nếu bất kỳ kiểm tra bắt buộc nào là `FAIL` hoặc `NOT RUN`.

## Quy trình

1. Xác nhận task đã ở trạng thái sau coding và không còn thay đổi chưa kiểm tra.
2. Đọc plan, diff liên quan và `Validation Result` gần nhất.
3. Xác nhận Dev Note đã ghi `Validation Result`, delta, blocker và số vòng debug.
4. Nếu có thay đổi backend mới hoặc kết quả không còn tin cậy, quay lại `backend-validation`.

## Quy tắc báo cáo

Chuyển tiếp nguyên trạng `Validation Result` từ `backend-validation`.

Báo cáo phải nêu rõ:

- file và hành vi đã thay đổi;
- `Validation Result` của test, Checkstyle, PMD và build;
- Dev Note đã cập nhật;
- lỗi hoặc giới hạn còn lại.

Nếu `Validation Result` có `FAIL` hoặc `NOT RUN`, không báo backend hoàn thành hoặc thành công.

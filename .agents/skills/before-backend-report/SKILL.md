---
name: before-backend-report
description: Kiểm tra điều kiện bắt buộc trước khi agent báo cáo kết quả code backend, gồm test đầy đủ, build, PMD, Checkstyle và lỗi còn lại. Use after backend implementation and before reporting success or completion.
---

# Before Backend Report

Đóng vai trò quality gate cuối cùng trước báo cáo backend.

## Điều kiện bắt buộc

- Đã có Developer Plan và người dùng đã phê duyệt bằng tin nhắn qua agent.
- Đã chạy test liên quan và test đầy đủ của backend.
- Đã chạy Checkstyle và PMD theo `backend-validation`.
- Đã kiểm tra build và các lỗi phát sinh từ thay đổi hiện tại.
- Đã tạo hoặc cập nhật Dev Note liên quan trong `document/dev-note/`.
- Mọi kết luận `PASS` đều phải có lệnh và kết quả thực tế làm bằng chứng.

Không báo cáo backend là hoàn thành hoặc thành công nếu bất kỳ kiểm tra bắt buộc nào là `FAIL` hoặc `NOT RUN`.

## Quy trình

1. Xác nhận task đã ở trạng thái sau coding và không còn thay đổi chưa kiểm tra.
2. Đọc plan, diff liên quan và validation output gần nhất.
3. Nếu output không còn tin cậy sau thay đổi mới, chạy lại:
   - từ `BE/BaiTap-RS`: `./gradlew.bat test`;
   - `./gradlew.bat checkstyleMain`;
   - `./gradlew.bat pmdMain`;
   - `./gradlew.bat build`.
4. Kiểm tra lỗi test, Checkstyle, PMD, build và report coverage nếu task yêu cầu JaCoCo.
5. Tạo hoặc cập nhật Dev Note bằng skill `dev-note`, bao gồm validation thực tế và delta so với Developer Plan.
6. Nếu lỗi do code hiện tại, sửa source hoặc test trong scope rồi quay lại bước validation.
7. Ghi số vòng `code → test → debug` đã dùng; tổng số vòng không được vượt quá 10.

## Quy tắc báo cáo

Chỉ dùng các trạng thái:

- `PASS`: đã chạy và thành công.
- `FAIL`: đã chạy nhưng thất bại.
- `NOT RUN`: chưa chạy hoặc không thể chạy.

Báo cáo phải nêu rõ:

- file và hành vi đã thay đổi;
- từng lệnh test, Checkstyle, PMD, build và JaCoCo nếu có;
- kết quả thực tế của từng lệnh;
- Dev Note đã cập nhật;
- số vòng debug;
- lỗi hoặc giới hạn còn lại.

Nếu đã dùng hết 10 vòng mà chưa đạt, dừng và báo cáo blocker; không gọi đó là `PASS`, không che giấu lỗi và không tiếp tục trial-and-error ngẫu nhiên.

# VALIDATION_AND_DEBUG.md

## 1. Mục đích
Reference này quy định cách AI Agent kiểm tra chất lượng và xử lý lỗi sau khi thay đổi mã nguồn.

## 2. Checkstyle
Trước khi hoàn tất:
- đọc `BE/BaiTap-RS/config/checkstyle/checkstyle.xml`;
- tuân thủ format, naming, import, whitespace, line length và convention liên quan;
- không tự ý disable rule;
- không dùng suppression nếu không có lý do kỹ thuật rõ ràng;
- sửa lỗi Checkstyle do code hiện tại gây ra.

## 3. PMD
Đọc cấu hình PMD tại `BE/BaiTap-RS/config/pmd/ruleset.xml` và phải chạy PMD.
Lỗi do thay đổi hiện tại phải được xử lý trước khi báo hoàn thành.

## 4. Build
Chạy build phù hợp với project sau khi thay đổi.
Không báo success nếu build bắt buộc đang fail.

## 5. Test
Ưu tiên theo thứ tự:
1. test trực tiếp cho phần thay đổi;
2. test của module;
3. test tích hợp liên quan;
4. test rộng hơn khi cần để phát hiện regression.

Nếu không thể chạy một loại test, phải nêu rõ lý do.

## 6. Lỗi cũ ngoài scope
Nếu validation fail vì code cũ:
- xác định bằng chứng cho thấy lỗi không do thay đổi hiện tại;
- không tự ý refactor ngoài scope;
- báo rõ lệnh đã chạy và lỗi còn tồn tại;
- không mô tả validation là pass nếu command tổng thể vẫn fail.

Có thể ghi rõ: phần thay đổi không tạo thêm lỗi mới, nhưng project còn lỗi baseline ngoài scope.

## 7. Giới hạn debug
Tối đa 10 vòng lặp:

`thay đổi → chạy kiểm tra → phân tích lỗi → sửa → chạy lại`

Một vòng được tính khi đã có thay đổi code và validation lại sau thay đổi đó.

## 8. Khi đạt giới hạn 10 vòng
Dừng trial-and-error và báo cáo:
- số vòng đã dùng;
- lỗi hiện tại;
- nguyên nhân đã xác định;
- giả thuyết còn lại;
- phương án đã thử;
- kết quả quan trọng;
- thay đổi hiện có;
- phần chưa hoàn thành;
- thông tin còn thiếu;
- bước tiếp theo đề xuất.

## 9. Nguyên tắc báo cáo
Không được:
- che giấu lỗi;
- nói “đã sửa” nếu chưa có validation xác nhận;
- nói “build/test pass” nếu chưa chạy;
- đổi scope chỉ để làm validation xanh;
- tiếp tục thử ngẫu nhiên sau giới hạn debug.

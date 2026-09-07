---
name: backend-validation
description: Chạy validation backend bằng test, Checkstyle, PMD và build; quản lý Validation Result, trạng thái PASS/FAIL/NOT RUN và tối đa 10 vòng debug. Use after backend implementation and before Dev Note or final reporting.
---

# Backend Validation

Thực hiện quality validation cho code backend trong `BE/BaiTap-RS`.

## Vai trò

Đây là skill duy nhất sở hữu logic validation backend. Workflow và
`before-backend-report` chỉ gọi skill này và sử dụng `Validation Result`.

## Quy tắc bắt buộc

- Đọc `BE/BaiTap-RS/config/checkstyle/checkstyle.xml` trước khi đánh giá Checkstyle.
- Đọc `BE/BaiTap-RS/config/pmd/ruleset.xml` trước khi đánh giá PMD.
- Chạy test liên quan và test backend đầy đủ sau mỗi thay đổi backend.
- Chạy cả Checkstyle và PMD sau mỗi vòng sửa code.
- Chỉ sửa source, test hoặc cấu hình đã nằm trong scope được phê duyệt; không sửa report để làm kết quả xanh.
- Report XML/HTML/console output chỉ được đọc. Không chỉnh sửa, xóa, làm giả hoặc suppress lỗi trong report.
- Không tắt rule, đổi `isIgnoreFailures`, thêm suppression hoặc đổi cấu hình kiểm tra chỉ để đạt `PASS` nếu chưa được phê duyệt.

## Gradle tasks chuẩn

Chạy từ thư mục `BE/BaiTap-RS` bằng Gradle wrapper phù hợp với môi trường:

```text
test
checkstyleMain
pmdMain
build
```

Đọc output console và các report do Gradle tạo ra; không sửa trực tiếp các file report.

## Vòng lặp sửa lỗi

1. Chạy test, Checkstyle, PMD và build theo thứ tự chuẩn.
2. Đọc lỗi và xác định lỗi thuộc thay đổi hiện tại hay baseline ngoài scope.
3. Sửa source/test trong scope nếu lỗi do task hiện tại.
4. Chạy lại toàn bộ validation.
5. Ghi nhận số vòng `code → test → debug`.

Tổng số vòng tối đa là 10. Nếu hết 10 vòng hoặc gặp blocker ngoài scope, dừng và báo cáo trung thực; không tuyên bố validation thành công khi lệnh tổng thể còn fail.

## Validation Result

Kết quả phải có đúng bốn mục sau:

- `test`: `PASS`, `FAIL` hoặc `NOT RUN`;
- `checkstyle`: `PASS`, `FAIL` hoặc `NOT RUN`;
- `PMD`: `PASS`, `FAIL` hoặc `NOT RUN`;
- `build`: `PASS`, `FAIL` hoặc `NOT RUN`.

`PASS` chỉ hợp lệ khi lệnh thực tế đã chạy thành công. `NOT RUN` phải
nêu lý do; không được suy ra trạng thái từ lệnh khác.

## Kết quả cần bàn giao

Ghi `Validation Result`, lệnh tương ứng, lỗi còn lại, số vòng đã dùng và
report đã đọc để Dev Note sử dụng trực tiếp.

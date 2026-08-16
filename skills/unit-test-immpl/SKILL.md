---
name: unit-test-immpl
description: Viết và chạy unit test sau khi agent code backend, sau đó dùng JaCoCo để đánh giá test coverage và báo cáo bằng chứng thực tế. Use after backend implementation when the approved unit-test plan must be implemented and verified.
---

# Unit Test Implementation

Thực hiện unit test theo plan đã được phê duyệt sau khi code backend.

## Quy trình

1. Đọc Developer Plan và `unit-test-plan` tương ứng.
2. Kiểm tra diff production code để bảo đảm test bao phủ behavior mới và regression risk.
3. Viết unit test cho case thành công, lỗi, boundary và interaction đã được plan xác định.
4. Chạy test từ `BE/BaiTap-RS`:

```text
./gradlew.bat test
```

5. Kiểm tra task JaCoCo khả dụng bằng `./gradlew.bat tasks --all`, sau đó chạy:

```text
./gradlew.bat jacocoTestReport
```

6. Đọc report coverage, tối thiểu kiểm tra class/method vừa thay đổi và ghi nhận instruction, branch hoặc line coverage nếu report cung cấp.
7. Nếu test fail hoặc coverage cho thấy case quan trọng chưa được bao phủ, sửa source/test trong scope và chạy lại.

## JaCoCo bắt buộc

Project hiện có dấu hiệu JaCoCo chưa được bật trong `BE/BaiTap-RS/build.gradle.kts`. JaCoCo là bắt buộc; không được kết thúc skill với `PASS` nếu task `jacocoTestReport` không tồn tại hoặc report chưa được tạo. Khi đó:

- ghi rõ `NOT RUN` và nguyên nhân;
- dừng ở trạng thái blocker, không báo hoàn tất;
- đề xuất cập nhật build plan để bật JaCoCo;
- chỉ thêm/cấu hình plugin khi đã nằm trong Developer Plan được phê duyệt.

Không tự đặt coverage threshold nếu project chưa quy định. Không sửa, xóa hoặc làm giả report JaCoCo; chỉ đọc report và sửa source/test khi cần.

## Giới hạn debug và kết quả

- Theo dõi số vòng `code → test → debug`.
- Không vượt quá 10 vòng.
- Không báo `PASS` nếu test hoặc JaCoCo chưa chạy thành công.
- Báo cáo lệnh đã chạy, kết quả test, report coverage, số vòng và các case còn thiếu.

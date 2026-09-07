---
name: unit-test-plan
description: Lập kế hoạch unit test trước khi agent code tính năng backend, bao gồm hành vi, case thành công/lỗi, boundary, mock, assertion, validation và JaCoCo coverage. Use before implementing a backend feature or changing backend behavior.
---

# Unit Test Plan

Tạo kế hoạch unit test trước khi viết code production hoặc test.

## Điều kiện bắt đầu

- Đọc requirement và tài liệu module liên quan khi cần.
- Xác định class, service, controller, repository hoặc component bị ảnh hưởng.
- Có Developer Plan cho feature; mọi plan phải được người dùng phê duyệt bằng tin nhắn qua agent trước khi code.

## Nội dung plan bắt buộc

Ghi trong Developer Plan hoặc phần test plan liên quan:

- class/method cần test;
- hành vi chính và business rule;
- case thành công;
- case lỗi, exception và validation;
- boundary, null, empty, duplicate, not-found và dữ liệu không hợp lệ nếu phù hợp;
- dependency cần mock hoặc fake;
- input, fixture và cách cô lập dữ liệu;
- assertion về output, state change, side effect và interaction;
- test regression cho behavior hiện có;
- lệnh chạy test;
- cách dùng JaCoCo để đọc coverage của phần thay đổi.

Không tự đặt coverage threshold hoặc business rule chưa có trong requirement. Nếu requirement chưa đủ để chọn case, đánh dấu `TBD` và hỏi người dùng thay vì tự suy đoán.

## Approval gate

Skill này chỉ lập plan; không triển khai production code hoặc unit test. Chỉ chuyển sang coding sau khi người dùng phê duyệt plan bằng tin nhắn qua agent.

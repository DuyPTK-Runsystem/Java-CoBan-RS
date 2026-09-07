# Developer Plan: Frontend API Catalog

## Mục tiêu

Tạo một tài liệu tập trung để FE tra cứu toàn bộ API v1 đang dùng: endpoint,
authentication, query parameters, request body, response data và lỗi.

## Phạm vi

- Tổng hợp User/Auth API và Student API.
- Ghi rõ khác biệt giữa tên field backend và model FE.
- Ghi rõ response envelope, `204 No Content`, CSV download và quy tắc ngày tháng.
- Liên kết tài liệu từ Application Documentation v1.

## Ngoài phạm vi

- Không thay đổi backend contract.
- Không thay đổi source code FE.
- Không thêm endpoint chưa có trong contract hoặc implementation hiện tại.

## Nguồn đối chiếu

- `document/application-doc/v1/ApplicationContext.md`.
- `document/application-doc/v1/modules/UserModule.md`.
- `document/application-doc/v1/modules/StudentModule.md`.
- `FE/src/services/userApi.ts` và `FE/src/services/studentApi.ts`.
- Các FE Dev Note 012, 015 và 021.

## Validation dự kiến

- `git diff --check`.
- Kiểm tra các link nội bộ trong tài liệu mới.

## Approval status

- Approved by user on 2026-08-26 through the request to aggregate APIs for FE.

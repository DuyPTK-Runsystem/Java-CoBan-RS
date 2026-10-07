# Application Documentation v5 — Library Management

## Trạng thái

- Ngày khởi tạo: 2026-10-07.
- Status: **DRAFT — requirement review; implementation approval pending**.
- Loại: feature baseline mới trên nền ứng dụng quản lý trường học hiện hữu.
- Nguồn nghiệp vụ ban đầu: đề training **“Thẻ Mượn Số”**; các giả định standalone đã được chuyển đổi để phù hợp với Java-CoBan-RS.
- v5 không thay thế các contract học vụ v3/v4. Identity, authentication, authorization infrastructure, audit, notification, database, deployment và frontend shell hiện hữu tiếp tục là platform baseline.

## Mục tiêu v5

v5 bổ sung bounded context **Library Management** cho hệ thống trường học hiện tại:

1. danh mục đầu sách và bản sao vật lý;
2. bạn đọc thư viện và thẻ QR;
3. mượn, trả, gia hạn, đặt giữ và xử lý mất sách;
4. phí trễ hạn bằng Spring Batch, có idempotency và restart;
5. barcode/QR và quét camera;
6. các tính năng AI hỗ trợ nghiệp vụ thư viện bằng Spring AI hiện hữu.

## Không phải mục tiêu

v5 **không**:

- tạo ứng dụng Spring Boot thứ hai;
- tạo frontend Vue thứ hai;
- tạo hệ thống đăng nhập/JWT/refresh-token riêng cho Library;
- tạo `MEMBER` như một application role song song với `STUDENT`/`TEACHER`;
- chuyển MySQL sang PostgreSQL;
- thay đổi hoặc xoá ngầm API `/api/v1`, `/api/v2`, `/api/v3` hiện có;
- bắt buộc provider AI hoặc SDK AI riêng;
- thực hiện thanh toán online, đa chi nhánh, microservices hoặc Kubernetes.

## Source order

Khi thực hiện task v5, đọc theo thứ tự:

1. [`ApplicationContext.md`](ApplicationContext.md)
2. [`RequirementBaseline.md`](RequirementBaseline.md)
3. CR v5 đã được duyệt
4. module/data-model/contract liên quan
5. Developer Plan tương ứng
6. code hiện tại khi cần xác nhận compatibility

Không suy diễn requirement từ đề standalone nếu requirement đó đã được adapt trong v5.

## Bản đồ tài liệu

| Area | Tài liệu |
|---|---|
| Context và boundary | [`ApplicationContext.md`](ApplicationContext.md) |
| Requirement baseline | [`RequirementBaseline.md`](RequirementBaseline.md) |
| Mapping spec standalone | [`requirement-adaptation/standalone-library-to-school-platform.md`](requirement-adaptation/standalone-library-to-school-platform.md) |
| Catalog | [`modules/01-LibraryCatalog.md`](modules/01-LibraryCatalog.md) |
| Patron và card | [`modules/02-LibraryPatronAndCard.md`](modules/02-LibraryPatronAndCard.md) |
| Circulation | [`modules/03-CirculationAndReservation.md`](modules/03-CirculationAndReservation.md) |
| Fine và Batch | [`modules/04-FineAndBatch.md`](modules/04-FineAndBatch.md) |
| Barcode/QR/camera | [`modules/05-BarcodeQrAndScanning.md`](modules/05-BarcodeQrAndScanning.md) |
| Library AI | [`modules/06-LibraryAI.md`](modules/06-LibraryAI.md) |
| Data ownership/schema | [`data-model/README.md`](data-model/README.md) |
| Migration/concurrency | [`data-model/MigrationAndConcurrency.md`](data-model/MigrationAndConcurrency.md) |
| Authorization | [`contract/Authorization.md`](contract/Authorization.md) |
| Error contract | [`contract/ErrorContract.md`](contract/ErrorContract.md) |
| Integration boundaries | [`contract/IntegrationBoundaries.md`](contract/IntegrationBoundaries.md) |
| FE/API contract | [`frontend-api/README.md`](frontend-api/README.md) |
| Main CR | [`change-request/CR-V5-001-library-management.md`](change-request/CR-V5-001-library-management.md) |

## API versioning rule

Document version, feature version và REST contract version là ba khái niệm độc lập.

- API Library mới mặc định dùng `/api/v2`.
- Ưu tiên `/api/v2/{resource}` nếu tên resource không conflict.
- Nếu `/api/v2/{resource}` đã có contract khác hoặc semantic ambiguity, dùng `/api/v2/library/{resource}`.
- Không sửa `/v1`, `/v2`, `/v3` cũ chỉ để “nhường chỗ” cho v5.
- Chỉ tạo Library `/v3` khi contract Library sau này có breaking change.

Baseline v5 hiện chọn:

```text
/api/v2/books
/api/v2/book-copies
/api/v2/library-patrons
/api/v2/library-cards
/api/v2/loans
/api/v2/returns
/api/v2/reservations
/api/v2/fines
/api/v2/library/batch-jobs
/api/v2/library/ai/*
```

Các path phải được scan lại trước implementation; nếu code phát sinh collision sau thời điểm baseline thì áp rule namespace ở trên.

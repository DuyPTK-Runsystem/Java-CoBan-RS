# Java CoBan

Môi trường Docker bên dưới chạy backend Spring Boot và MySQL. Frontend vẫn được quản lý độc lập trong thư mục `FE/`.

## Điều kiện cần

- Docker Engine và Docker Compose.
- Port `3307` còn trống trên máy host. API dùng `8081` mặc định và có thể đổi qua `API_HOST_PORT` trong `docker/.env` khi port này đang được dùng.

## Clone và chạy từ Git

Clone repository, rồi chuyển vào thư mục vừa clone:

```bash
git clone https://github.com/DuyPTK-Runsystem/Java-CoBan-RS.git
cd Java-CoBan-RS
```

## Khởi động bằng Docker

Tạo file cấu hình local, rồi thay password mẫu bằng một giá trị chỉ dùng tại máy của bạn:

```bash
cp docker/.env.example docker/.env
```

Khởi động backend và MySQL:

```bash
docker compose --env-file docker/.env up -d --build
```

Kiểm tra health của backend:

```bash
curl http://localhost:8081/actuator/health
```

Backend được mở tại `http://localhost:${API_HOST_PORT}` (`8081` mặc định). MySQL được map từ port host `3307` tới port `3306` trong container (`3307:3306`), nên kết nối từ máy host dùng:

```text
jdbc:mysql://localhost:3307/java_coban
```

Các container kết nối nội bộ qua `db:3306`; không dùng `localhost:3307` từ container backend.

Mỗi lần `api` khởi động, entrypoint script kiểm tra kết nối MySQL và chạy `CREATE DATABASE IF NOT EXISTS java_coban` trước khi chạy Spring Boot. Vì vậy Docker volume cũ chưa có database này vẫn có thể khởi động, miễn credential trong `docker/.env` hợp lệ.

## Nạp dữ liệu test batch

Script dưới đây chờ API healthy rồi chạy `document/postman/Java-CoBan-Batch-Test-Data.postman_collection.json` bằng Newman. Collection tạo một test user và 500 Student qua REST API, không insert SQL trực tiếp.

```bash
./scripts/load-batch-test-data.sh
```

Mỗi lần chạy thành công sẽ thêm 500 Student mới. Script không xoá hoặc reset database.

## Theo dõi và dừng môi trường

```bash
docker compose --env-file docker/.env logs -f api db
docker compose --env-file docker/.env down
```

`down` thông thường vẫn giữ named volume MySQL. Chỉ xoá volume khi bạn chủ động muốn xoá toàn bộ dữ liệu Docker của project:

```bash
docker compose --env-file docker/.env down -v
```

## Xử lý sự cố nhanh

- Nếu port `3307` đã được dùng, giải phóng port đó; Docker configuration cố ý không dùng `3306` trên host để tránh xung đột MySQL local.
- Nếu `8081` đã được dùng, đổi `API_HOST_PORT` trong `docker/.env` (ví dụ `8082`) rồi chạy lại Compose.
- Nếu API chưa healthy, xem log bằng lệnh `docker compose --env-file docker/.env logs api db`.
- Không commit `docker/.env`: file này chứa password local.

# Developer Plan 023: Docker Image, Test Data Bootstrap và README

## 1. Mục tiêu

- Đóng gói backend Spring Boot và MySQL thành môi trường Docker Compose có thể chạy được trên máy phát triển mà không xung đột MySQL local.
- Tạo image backend đa tầng, kiểm thử image cục bộ và push image đã xác định tag lên registry do người dùng cung cấp.
- Cung cấp script chạy chính collection `document/postman/Java-CoBan-Batch-Test-Data.postman_collection.json` bằng Newman để thêm dữ liệu test qua REST API.
- Tạo README gốc hướng dẫn chạy môi trường Docker, kiểm tra health và nạp test data. README không ghi quy trình/lệnh/credentials push image.

## 2. Requirement và hiện trạng

- Backend nằm tại `BE/BaiTap-RS`, dùng Gradle Wrapper, Java toolchain 21 và Spring Boot 4.0.7; hiện chưa có Dockerfile, Docker Compose, `.dockerignore` hoặc README gốc.
- Health endpoint hiện có: `GET /actuator/health`; ứng dụng dùng `SERVER_PORT` mặc định `8081`.
- MySQL container phải publish **cổng máy vật lý `3307` sang cổng container `3306`** (`3307:3306`) để không xung đột MySQL local.
- Application chạy trong Docker network phải kết nối database qua tên service và cổng nội bộ `db:3306`; port `3307` chỉ dùng khi host truy cập MySQL container.
- Collection test-data hiện có tạo một user theo timestamp, login, sau đó gọi API tạo 500 Student. Collection dựa vào Postman script (`postman.setNextRequest`), vì vậy seed phải chạy bằng Newman, không thay bằng SQL insert.
- Yêu cầu push image là bước triển khai/vận hành; không được ghi nội dung push image vào README hoặc tài liệu hướng dẫn mới.

## 3. Phạm vi

### In-scope

- Dockerfile multi-stage để build `bootJar` bằng Gradle và chạy JAR bằng Java runtime tối thiểu, non-root user, kèm `HEALTHCHECK` gọi Actuator.
- Docker Compose gồm `api` và `db`, DB volume có tên, healthcheck MySQL, dependency chờ DB healthy, môi trường cấu hình database và port host `3307:3306`.
- Cấu hình mẫu không chứa secrets; local `.env` hiện có không bị thay đổi và không dùng làm nguồn commit secrets mới.
- Script nạp data khởi động/kiểm tra API healthy rồi chạy collection qua container Newman trong cùng Docker network; có output và exit code phản ánh collection thất bại.
- README gốc chỉ dẫn prerequisite, cấu hình runtime, build/run/stop, endpoint health, nạp test-data, port mapping và cách dọn volume có chủ đích.
- Build, smoke test Docker Compose, seed test-data, kiểm tra DB/API, và push image với registry/repository/tag do người dùng cung cấp sau approval.
- Tạo Dev Note trong `document/dev-note/summary/` sau implementation, ghi image digest/tag đã push (nếu có), validation thực tế và không chứa password/token.

### Out-of-scope

- Dockerize frontend, Kubernetes, CI/CD, registry provisioning, chỉnh sửa source nghiệp vụ/API/schema, migration mới và thay collection Postman hiện có.
- Dùng port host `3306` cho MySQL container hoặc sửa cấu hình MySQL local của người dùng.
- Commit registry credentials, JWT secret, database password, token Newman, image push command hay hướng dẫn push vào README/tài liệu người dùng.
- Tự xoá volume/data hiện hữu khi chạy script seed; thao tác reset data phải tách riêng và yêu cầu người dùng xác nhận.

## 4. Kiến trúc và luồng đề xuất

```text
Host: localhost:3307
        |
        v
Docker Compose db:3306 <--- api:8081 (SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/java_coban)
                                |
                                v
                    /actuator/health và Student REST API
                                ^
                                |
        Newman one-off container -- chạy Postman collection trên Docker network
```

1. `api` build từ Dockerfile bằng context `BE/BaiTap-RS`; build stage dùng Gradle Wrapper/Java 21 để tạo boot JAR, runtime stage chỉ chứa JRE, JAR và user không phải root.
2. `db` dùng MySQL image có version pin, volume named để dữ liệu tồn tại qua `docker compose down`, và publish `${MYSQL_HOST_PORT:-3307}:3306`.
3. Compose truyền `SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/java_coban`; không dùng `localhost` trong container. Password/database name lấy từ file environment local bị ignore và có file example không chứa secret.
4. Script seed chỉ chạy sau khi `api` healthy. Nó gọi Newman với `--env-var baseUrl=http://api:8081` và mount collection read-only; Newman tạo user/login/500 Student đúng collection, nên dữ liệu đi qua validation, JWT và service hiện có.
5. Push chỉ dùng image đã build/verify cùng một repository/tag bất biến. Registry repository, tag và quyền đăng nhập là input bắt buộc trước execution; các thông tin này chỉ xuất hiện trong terminal/Dev Note nội bộ khi cần, không trong README.

## 5. Phạm vi file dự kiến

| Path/khu vực | Thao tác | Mục đích |
|---|---|---|
| `BE/BaiTap-RS/Dockerfile` | Tạo | Multi-stage Gradle build và Java 21 runtime cho backend. |
| `BE/BaiTap-RS/.dockerignore` | Tạo | Loại `build/`, `.gradle/`, IDE files, local env/secrets và artifact không cần khỏi build context. |
| `docker-compose.yml` | Tạo | Orchestrate `api`, `db` và service Newman one-off; khai báo network, named volume, healthcheck và map `3307:3306`. |
| `docker/.env.example` và `.gitignore` liên quan | Tạo/Sửa nếu cần | Chỉ cung cấp biến không bí mật/mẫu biến cần thiết; bảo đảm file runtime chứa password không được commit. |
| `scripts/load-batch-test-data.sh` | Tạo | Kiểm tra Docker Compose/API health, chạy collection qua Newman trong network và trả đúng exit status. |
| `README.md` | Tạo | Hướng dẫn Docker runtime, mapping port, healthcheck, seed test-data và dọn môi trường; tuyệt đối không mô tả push image. |
| `document/dev-note/summary/023-docker-image-readme-test-data-2026-08-19.md` | Tạo sau implementation | Ghi nhận implementation/validation thực tế, không ghi secret. |
| `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md` | Sửa | Index plan 023 và trạng thái approval/completion thực tế. |

`document/postman/Java-CoBan-Batch-Test-Data.postman_collection.json`, `.env` local và application source sẽ không bị sửa, trừ khi validation chứng minh image không thể khởi động với environment contract hiện có. Khi đó phải dừng và cập nhật plan trước.

## 6. Docker và integration contract

| Thành phần | Contract |
|---|---|
| Backend HTTP | Host `8081:8081` (hoặc biến host port được README nêu rõ); health tại `http://localhost:8081/actuator/health`. |
| MySQL | **Host `3307` -> container `3306`**. Host JDBC: `jdbc:mysql://localhost:3307/java_coban`; API JDBC trong Compose: `jdbc:mysql://db:3306/java_coban`. |
| Database | `java_coban`, MySQL credentials qua env runtime; không hard-code/commit password. |
| Persistence | Named volume giữ DB sau restart/down thông thường; chỉ hướng dẫn xoá volume như thao tác có chủ đích. |
| Test data | `scripts/load-batch-test-data.sh` chạy collection bằng Newman và sử dụng `http://api:8081`, không gọi SQL trực tiếp. Mỗi lần chạy có thể thêm 500 Student mới; script không tự reset DB. |
| Registry | Image repository/tag do người dùng cung cấp trước khi push. Chỉ push image sau khi local image, Compose startup và seed smoke test pass. |

## 7. Phương án triển khai chi tiết

1. Kiểm tra tên JAR thực tế từ `./gradlew bootJar`, sau đó viết Dockerfile multi-stage có cache layer hợp lý cho Gradle wrapper/build files; runtime chạy đúng user non-root và expose cổng 8081.
2. Tạo Compose với `db` start trước, healthcheck MySQL và `api` phụ thuộc health. Truyền toàn bộ datasource qua Compose environment để tách host port 3307 và container port 3306.
3. Tạo `.dockerignore` và runtime environment example; kiểm tra `git check-ignore` để bảo đảm file có password không thể bị add nhầm.
4. Tạo script seed dùng `docker compose up -d --build`, polling `api` health với timeout hữu hạn, sau đó `docker compose run --rm` Newman. Script mount đúng collection, dùng service DNS `api`, in hướng dẫn rằng mỗi run thêm 500 records, và không xoá dữ liệu khi failure.
5. Viết README từ góc nhìn người chạy: prerequisites, tạo file environment từ example, start/stop, `3307:3306`, health URL, seed command, logs/troubleshooting ngắn và cleanup có cảnh báo. Không đưa registry, `docker login`, `docker push`, repository hay tag push vào README.
6. Sau smoke tests pass, tag và push cùng image theo registry/repository/tag được người dùng cung cấp. Ghi digest/tag thật vào Dev Note nếu push thành công; không đưa credentials/token vào bất kỳ file nào.

## 8. Test và validation plan

### Static và build

Chạy từ `BE/BaiTap-RS/`:

```bash
./gradlew test
./gradlew checkstyleMain checkstyleTest
./gradlew pmdMain pmdTest
./gradlew build
```

- Xác nhận Docker build thành công và image runtime không chứa Gradle source/build cache không cần thiết.
- Kiểm tra `docker compose config` sau khi nạp environment; không in secret vào báo cáo.
- Chạy `git diff --check` và kiểm tra `.dockerignore`/`.gitignore` bảo vệ local env.

### Docker smoke test

- `docker compose up -d --build` khởi động `db` healthy trước; `api` đạt `GET /actuator/health` 200.
- Host kết nối DB qua cổng `3307`; cổng `3306` trên host không được Compose bind.
- Xác nhận API datasource chỉ kết nối `db:3306` nội bộ, không dùng `localhost:3307` từ container.
- Stop/start bình thường giữ named volume; không chạy lệnh xoá volume trong validation.

### Seed và regression

- Chạy `scripts/load-batch-test-data.sh`; Newman phải pass Register, Login và đủ 500 request Create Student.
- Kiểm tra số Student tăng đúng 500 so với mốc trước seed và gọi CSV export/Student list để xác nhận data được application đọc được.
- Chạy script lần hai chỉ khi cần kiểm tra repeatability; nếu chạy, xác nhận tăng thêm 500 và không overwrite record trước đó.
- Khi có registry input, xác nhận local image tag khớp image đã push; pull/run độc lập chỉ được thực hiện nếu registry/image public hoặc người dùng cấp quyền phù hợp.

## 9. Rủi ro, assumption và điểm cần xác nhận

| Hạng mục | Rủi ro / cách giảm thiểu |
|---|---|
| Port 3307 | Máy chủ có thể đã chiếm cổng 3307. Compose fail rõ ràng; không tự đổi sang port khác vì requirement đã chốt 3307. |
| Test data | Collection không idempotent theo nghĩa count: mỗi lần thành công thêm 500 Student. Script cảnh báo hành vi này và không tự xoá database. |
| Startup | App có thể mất thời gian khởi tạo Spring Batch schema. Script polling health với timeout và in logs cần xem khi timeout. |
| Secret | Password DB/JWT/registry credential có thể bị lộ qua env/log. Dùng env local bị ignore, masking và không copy giá trị vào README/Dev Note. |
| Image push | Chưa có registry host, namespace/repository, tag policy hay credentials. Đây là blocker duy nhất để thực hiện bước push; Dockerfile/Compose/README/seed vẫn có thể hoàn thành trước. |
| Platform | Image build trên máy phát triển có thể khác platform môi trường đích. Chỉ đặt `--platform` khi registry/target platform được xác nhận; không tự giả định. |

## 10. Output dự kiến

- Một lệnh Docker Compose chạy backend cùng MySQL, trong đó host dùng `3307:3306` và application vẫn kết nối `db:3306` nội bộ.
- Backend image Java 21 đã build/verify; image chỉ được push sau khi có registry coordinates và credentials từ người dùng.
- Một script tạo đúng batch test data thông qua Postman collection/Newman, không dùng SQL và không reset database.
- README gốc đủ để chạy/kiểm tra/nạp data tại local nhưng không chứa nội dung push image hoặc bí mật.
- Dev Note và summary được cập nhật bằng bằng chứng validation sau implementation.

## 11. Approval status

- Trạng thái: **Approved by user via agent on 2026-08-19.**
- Approval triển khai plan này đồng nghĩa cho phép tạo Docker/Compose/script/README và chạy validation cục bộ.
- Trước khi push image, cần người dùng cung cấp registry host, repository/namespace, tag mong muốn và quyền đăng nhập; không suy đoán hoặc commit các giá trị này.

## 12. Amendment 23.1: Database schema check before application start

- Yêu cầu bổ sung được user chấp thuận qua agent ngày 2026-08-19: Docker image phải có script kiểm tra MySQL và tạo database `java_coban` khi chưa tồn tại rồi mới chạy Spring Boot.
- Runtime stage dùng MySQL Connector/J đã có trong boot JAR để chạy bootstrap Java dưới user `spring`, retry kết nối MySQL hữu hạn và thực hiện `CREATE DATABASE IF NOT EXISTS` với `DATABASE_NAME=java_coban`. Cách này tương thích `caching_sha2_password` của MySQL 8.4, không phụ thuộc MariaDB client.
- `DATABASE_NAME` chỉ chấp nhận chữ cái, số và dấu gạch dưới để không tạo SQL identifier không an toàn. Script không tạo/sửa bảng; Hibernate `ddl-auto=update` tiếp tục chịu trách nhiệm schema bảng.
- Compose truyền host/port/database nội bộ `db:3306/java_coban`; host mapping MySQL vẫn không đổi `3307:3306`.

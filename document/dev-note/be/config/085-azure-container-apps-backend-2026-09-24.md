# Dev Note 085 BE — Docker image cho Azure Container Apps

## Developer Plan và approval

- Developer Plan ngắn trong hội thoại ngày 2026-09-24: viết lại Dockerfile Java 21, dùng database được cấp, cấu hình cổng/CORS và điều khiển seed bằng biến môi trường. Người dùng đã yêu cầu viết Dockerfile và deploy; đích cuối cùng hiện là Azure Container Apps Consumption cho BE, Vercel cho FE và Azure Database for MySQL Flexible Server. Không có file Developer Plan riêng.
- Phạm vi note này: BE Dockerfile và cấu hình BE. Coordinator phụ trách tạo tài nguyên, deploy và seed.

## Phạm vi thực tế và file thay đổi

- `BE/BaiTap-RS/.dockerignore`: loại `.env`, build output và file local khỏi cloud build context để không upload bí mật.
- `BE/BaiTap-RS/Dockerfile`: build Gradle `bootJar` bằng JDK 21; runtime JRE 21 với user không phải root; bỏ bước `DatabaseBootstrap` tạo database và healthcheck gắn cứng cổng; image mặc định `APP_SEED_DEMO_ENABLED=false`.
- `BE/BaiTap-RS/src/main/resources/application.properties`: nhận `PORT` nếu nền tảng cấp, fallback `SERVER_PORT` và cổng 8081. Azure Container Apps dùng ingress target port 8081 nếu không cấp `PORT`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/config/SecurityConfiguration.java`: nối thêm origin từ `APP_CORS_ALLOWED_ORIGINS` (danh sách phân tách bằng dấu phẩy), giữ localhost cho local FE.
- Không chỉnh sửa `.env`, database schema hay seed data. Dockerfile không cần cấu hình Azure riêng.

## Cấu hình Azure Container Apps

- Build context là `BE/BaiTap-RS`; image lưu ở Azure Container Registry. Từ repo root: `cd BE/BaiTap-RS && az acr build --registry <ACR_NAME> --image baitap-rs:<TAG> --file Dockerfile .`. Lệnh `az acr build` build và push image; không cần Docker daemon local.
- Azure Container App dùng image `<ACR_LOGIN_SERVER>/baitap-rs:<TAG>`, `--ingress external`, `--target-port 8081`, `--workload-profile-name Consumption`; đặt `--min-replicas 1 --max-replicas 1` trong lần seed đầu. Managed identity của app cần quyền `AcrPull` trên ACR; có thể dùng `--user-assigned <IDENTITY_ID> --registry-identity <IDENTITY_ID> --registry-server <ACR_LOGIN_SERVER>` khi tạo app.
- Mẫu lệnh tạo Container App sau khi đã có Resource Group, Container Apps Environment, ACR, Key Vault secrets và user-assigned identity có quyền pull ACR/đọc Key Vault (thay mọi placeholder trước khi chạy):

  ```bash
  az containerapp create \
    --resource-group <RG> --name <APP> --environment <ACA_ENV> \
    --image <ACR_LOGIN_SERVER>/baitap-rs:<TAG> \
    --user-assigned <IDENTITY_ID> \
    --registry-server <ACR_LOGIN_SERVER> --registry-identity <IDENTITY_ID> \
    --workload-profile-name Consumption \
    --ingress external --target-port 8081 \
    --min-replicas 1 --max-replicas 1 \
    --secrets \
      "db-password=keyvaultref:<DB_PASSWORD_SECRET_URI>,identityref:<IDENTITY_ID>" \
      "jwt-secret=keyvaultref:<JWT_SECRET_URI>,identityref:<IDENTITY_ID>" \
    --env-vars \
      "SPRING_DATASOURCE_URL=jdbc:mysql://<MYSQL_FQDN>:3306/<DATABASE>?sslMode=REQUIRED&serverTimezone=UTC" \
      "SPRING_DATASOURCE_USERNAME=<DB_USER>" \
      "SPRING_DATASOURCE_PASSWORD=secretref:db-password" \
      "JWT_SECRET=secretref:jwt-secret" \
      "APP_CORS_ALLOWED_ORIGINS=https://<VERCEL_DOMAIN>" \
      "SPRING_JPA_HIBERNATE_DDL_AUTO=validate" \
      "APP_SEED_DEMO_ENABLED=true"
  ```

- Runtime: `SPRING_DATASOURCE_URL=jdbc:mysql://<MYSQL_FQDN>:3306/<DATABASE>?sslMode=REQUIRED&serverTimezone=UTC`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` (secret reference), `JWT_SECRET` (secret reference), `APP_CORS_ALLOWED_ORIGINS=https://<VERCEL_DOMAIN>`, `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`. Không ghi giá trị bí mật vào repo; tạo secret trong Azure Portal hoặc Key Vault và tham chiếu bằng `secretref:`. MySQL database phải tồn tại trước khi app start; network/firewall/DNS phải cho app truy cập server.
- Lần deploy/khởi động đầu đặt `APP_SEED_DEMO_ENABLED=true`. Sau khi `/actuator/health` thành công và xác minh dữ liệu seed trên đúng database, chạy `az containerapp update --resource-group <RG> --name <APP> --set-env-vars APP_SEED_DEMO_ENABLED=false`, rồi kiểm tra revision mới và biến đã tắt. Đổi env var tạo revision mới; không tự động tắt sau lần chạy đầu. Đảm bảo revision cũ không còn nhận traffic. Không chạy nhiều replica cùng lúc khi seed.
- Tài liệu Microsoft: https://learn.microsoft.com/en-us/azure/container-registry/container-registry-quickstart-task-cli ; https://learn.microsoft.com/en-us/azure/container-apps/tutorial-deploy-from-code ; https://learn.microsoft.com/en-us/azure/container-apps/environment-variables ; https://learn.microsoft.com/en-us/azure/mysql/flexible-server/connect-java .

## Validation Result

- `git diff --check`: PASS (QA độc lập ở lượt trước; chưa chạy lại sau khi sửa note này).
- `GRADLE_USER_HOME=/tmp/java-coban-gradle ./gradlew --no-daemon --max-workers=1 bootJar`: PASS (QA độc lập ở lượt trước, Java 21; BE source chưa đổi từ đó).
- Docker image build/run và HTTP health: NOT RUN; Docker CLI không kết nối được daemon local.
- Backend test, Checkstyle, PMD: NOT RUN.
- Azure ACR build và Container Apps/MySQL runtime: NOT RUN; coordinator thực hiện khi có Azure access.

## Sai lệch và bước tiếp theo

- Đích lần lượt đổi từ Railway sang Render rồi Azure Container Apps; Dockerfile và code BE vẫn dùng được vì không hardcode nền tảng.
- Coordinator tạo MySQL database, ACR/Container Apps Environment/Container App và secrets, build image, deploy seed true, xác minh dữ liệu rồi chuyển false. QA độc lập xác minh image/runtime khi có môi trường thực tế.

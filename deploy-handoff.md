## Trạng thái hiện tại

Backend đang chạy trên Azure Container Apps chuẩn:

```text
App: aca-java-coban-rs-standard
Resource Group: rg-java-coban-demo
Environment: acae-java-coban-rs-standard-2026
URL: https://aca-java-coban-rs-standard.gentledesert-50ef7e69.eastasia.azurecontainerapps.io
Image: javacobanrsdemo26.azurecr.io/baitap-rs:b92feab
```

Container resource:

```text
CPU: 0.5
Memory: 1.0Gi
```

Ban đầu `0.5Gi` gây:

```text
java.lang.OutOfMemoryError: Java heap space
```

Sau khi tăng lên `1Gi`, Spring Boot startup thành công:

```text
Tomcat started on port 8081
Started BaiTapRsApplication in 53.822 seconds
```

Port backend:

```text
8081
```

Database:

```text
Server: java-coban-rs-demo-2026.mysql.database.azure.com
Database: java_coban
User: java_coban_app
```

DB/Flyway đã connect thành công.

Các env chính:

```text
SPRING_DATASOURCE_URL=jdbc:mysql://java-coban-rs-demo-2026.mysql.database.azure.com:3306/java_coban?sslMode=REQUIRED
SPRING_DATASOURCE_USERNAME=java_coban_app
SPRING_DATASOURCE_PASSWORD=secretref:db-password
JWT_SECRET=secretref:jwt-secret
APP_CORS_ALLOWED_ORIGINS=https://java-co-ban-rs.vercel.app
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
APP_SEED_DEMO_ENABLED=false
```

---

## Health hiện tại

```text
GET /actuator/health
=> HTTP 503
=> {"groups":["liveness","readiness"],"status":"DOWN"}
```

Nhưng:

```text
GET /actuator/health/liveness
=> UP

GET /actuator/health/readiness
=> UP
```

Tức là:

```text
Application/Tomcat: OK
Ingress: OK
Port 8081: OK
Liveness: UP
Readiness: UP
Overall health: DOWN
```

Có một health indicator khác đang làm overall health DOWN.

Đã thêm:

```text
MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=always
MANAGEMENT_ENDPOINT_HEALTH_SHOW_COMPONENTS=always
```

ACA revision config đã xác nhận cả hai đều có value `always`.

Nhưng `/actuator/health` vẫn không hiện `components`.

---

## Việc cần làm tiếp

### 1. Kiểm tra Java process có thực sự nhận MANAGEMENT_* không

Exec vào container:

```powershell
& $az containerapp exec `
  --name 'aca-java-coban-rs-standard' `
  --resource-group 'rg-java-coban-demo' `
  --revision 'aca-java-coban-rs-standard--latest' `
  --container 'aca-java-coban-rs-standard' `
  --command sh
```

Trong container:

```sh
JAVA_PID=$(ps | awk '/java -jar/ {print $1; exit}')

tr '\0' '\n' < /proc/$JAVA_PID/environ | grep '^MANAGEMENT_'
```

Mong đợi:

```text
MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=always
MANAGEMENT_ENDPOINT_HEALTH_SHOW_COMPONENTS=always
```

### 2. Nếu Java đã nhận đủ 2 env

Scan source project để tìm custom health behavior:

```text
HealthIndicator
HealthContributor
HealthEndpoint
HealthEndpointWebExtension
management.endpoint.health
management.health
/actuator/**
```

Đặc biệt kiểm tra `application.properties` và các class config/security/custom response.

Mục tiêu là tìm component nào khiến:

```text
overall health = DOWN
```

trong khi:

```text
liveness = UP
readiness = UP
```

### 3. Sau khi xử lý health

Gỡ:

```text
MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=always
MANAGEMENT_ENDPOINT_HEALTH_SHOW_COMPONENTS=always
```

nếu không muốn expose health details public.

### 4. Nối frontend Vercel

Frontend:

```text
https://java-co-ban-rs.vercel.app
```

Set:

```text
VITE_API_BASE_URL=https://aca-java-coban-rs-standard.gentledesert-50ef7e69.eastasia.azurecontainerapps.io
```

Sau đó redeploy Vercel và test FE → BE → MySQL.

---

## Lưu ý

`az containerapp logs show` hiện lỗi:

```text
KeyError: 'eventStreamEndpoint'
```

Nếu cần log, dùng `logStreamEndpoint` của replica thay vì command trên.

Không cần debug lại:

```text
Express ACA cũ
ACR
MySQL connectivity
DB username
port 8081
memory 0.5Gi
```

Các phần đó đã giải quyết xong.

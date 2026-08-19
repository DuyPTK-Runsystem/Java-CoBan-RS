# Dev Note 023: Docker Image, Test Data Bootstrap và README

## Related Developer Plan

- Plan: `document/dev-impl-plan/summary/023-docker-image-readme-test-data-2026-08-19.md`.
- Approval: user approved via agent on 2026-08-19.

## Actual scope completed

- Tạo Dockerfile multi-stage Java 21 cho backend; runtime dùng user `spring` không phải root, Actuator healthcheck và thư mục `build/logs` có quyền ghi.
- Tạo Docker Compose cho `api`, MySQL 8.4 và Newman one-off service. MySQL host mapping cố định là `3307:3306`; backend kết nối nội bộ `jdbc:mysql://db:3306/java_coban`.
- Tạo template environment Docker, ignore local Docker env, script nạp Postman collection qua Newman và README Docker runtime không có nội dung publish image; README cũng hướng dẫn clone repository từ Git trước khi chạy Docker.
- Thêm `API_HOST_PORT` (mặc định `8081`) để tránh collision với process local; validation dùng `8082`. Thay đổi này không ảnh hưởng mapping MySQL `3307:3306`.
- Amendment 23.1: thêm `BE/BaiTap-RS/docker-entrypoint.sh` và `BE/BaiTap-RS/docker/DatabaseBootstrap.java`. Runtime image dùng Connector/J để kiểm tra MySQL, tạo `java_coban` bằng `CREATE DATABASE IF NOT EXISTS` khi cần, rồi mới `exec` Spring Boot; bảng vẫn do Hibernate quản lý.

## Files changed

| Purpose | Files |
|---|---|
| Container backend | `BE/BaiTap-RS/Dockerfile`, `BE/BaiTap-RS/docker-entrypoint.sh`, `BE/BaiTap-RS/docker/DatabaseBootstrap.java`, `BE/BaiTap-RS/.dockerignore` |
| Docker runtime | `docker-compose.yml`, `docker/.env.example`, `.gitignore` |
| Test-data bootstrap | `scripts/load-batch-test-data.sh` |
| User guide | `README.md` |
| Plan/index | `document/dev-impl-plan/summary/023-docker-image-readme-test-data-2026-08-19.md`, `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md` |

## Validation

| Command/check | Result | Evidence |
|---|---|---|
| `./gradlew test` | PASS | Backend test suite completed successfully. |
| `./gradlew checkstyleMain checkstyleTest` | PASS | Both Checkstyle tasks successful. |
| `./gradlew pmdMain pmdTest` | PASS | Both PMD tasks successful. |
| `./gradlew build` | PASS | Build, checks and JaCoCo report task successful. |
| `bash -n scripts/load-batch-test-data.sh` | PASS | Shell syntax valid after dynamic API host-port support. |
| `MYSQL_ROOT_PASSWORD=validation-only docker compose config` | PASS | Compose resolves `3307:3306`, `db:3306` and service healthchecks. |
| Docker image build | PASS | Image built successfully; inspect confirmed runtime user `spring:spring` and Actuator healthcheck. |
| Docker Compose health smoke | PASS | MySQL healthy at host `3307`; API healthy at validation host port `8082`, using internal JDBC URL `jdbc:mysql://db:3306/java_coban`. |
| Amendment 23.1 Docker rebuild | PASS | Docker build compiled `DatabaseBootstrap.java`, extracted the existing MySQL Connector/J from boot JAR, and produced the updated entrypoint image. |
| Amendment 23.1 database-missing smoke | NOT RUN to completion | Validation project dropped only its temporary `java_coban` database and started updated API, but Docker daemon socket disappeared before API health and recreated-database query could return evidence. |
| Newman seed collection | NOT RUN to completion | Docker daemon socket disappeared before Newman result/count could be obtained. No assertion that 500 students were created. |
| Image publication | NOT RUN | Registry/repository/tag and authenticated publishing access were not supplied. |

## Deviation and fixes

- Initial container start failed because the existing Logback configuration writes to `build/logs`, which was absent in a non-root runtime image. Dockerfile now creates and owns `/app/build/logs` for `spring`.
- Host port `8081` was already occupied during validation. `API_HOST_PORT` was added as an opt-in override; Docker default remains `8081` and database mapping remains exactly `3307:3306`.
- MariaDB client in Alpine cannot load MySQL 8.4 `caching_sha2_password`; the first amendment implementation therefore retried without connecting. It was replaced with a small Java bootstrap compiled in the Docker build and the project's MySQL Connector/J, which supports the server authentication plugin.

## Remaining blockers and next steps

- Restart/restore Docker daemon, copy `docker/.env.example` to `docker/.env`, then run `./scripts/load-batch-test-data.sh` and verify Newman success plus the Student count increase of 500.
- After Docker daemon is stable, re-run the database-missing smoke test: drop only a disposable database, start API, then confirm entrypoint creates `java_coban` and health is `UP`.
- Supply approved registry host, repository/namespace and tag, together with authenticated Docker access, before the external image publication action can run.
- Validation temporary `docker/.env` was removed; no runtime password or token was committed.

## Validation iterations

- Code/config → validation → Docker runtime fix: 2 iterations.
- Amendment 23.1 Docker build/connection-client fix: 2 iterations; final runtime smoke remains blocked by Docker daemon availability.

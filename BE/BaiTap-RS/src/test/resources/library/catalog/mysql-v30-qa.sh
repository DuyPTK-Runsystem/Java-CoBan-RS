#!/usr/bin/env bash
set -euo pipefail

# Run only against the isolated, no-port QA container. This script drops and
# recreates only the plan096_qa schema inside that disposable container.
# Create the container first with:
# docker run -d --name plan096-mysql-qa --network none -e MYSQL_ALLOW_EMPTY_PASSWORD=yes mysql:8.4
# Remove it after validation with: docker rm -fv plan096-mysql-qa
container_name="plan096-mysql-qa"
database_name="plan096_qa"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
migration_file="$(cd "${script_dir}/../../../../main/resources/db/migration" && pwd)/V30__create_library_catalog.sql"

if ! docker inspect --format '{{.State.Running}}' "${container_name}" 2>/dev/null | grep -qx true; then
    printf 'Disposable QA container %s must already be running.\n' "${container_name}" >&2
    exit 2
fi

network_mode="$(docker inspect --format '{{.HostConfig.NetworkMode}}' "${container_name}")"
published_ports="$(docker inspect --format '{{json .NetworkSettings.Ports}}' "${container_name}")"
if [[ "${network_mode}" != "none" || "${published_ports}" != "{}" ]]; then
    printf 'Refusing to use %s: expected network=none and no published ports.\n' "${container_name}" >&2
    exit 2
fi

version="$(docker exec "${container_name}" mysql -uroot -Nse 'SELECT VERSION()')"
printf 'Disposable MySQL version: %s\n' "${version}"

docker exec "${container_name}" mysql -uroot -e \
    "DROP DATABASE IF EXISTS ${database_name}; CREATE DATABASE ${database_name};"
docker exec "${container_name}" mysql -uroot "${database_name}" -e \
    'CREATE TABLE app_user (user_id BIGINT NOT NULL AUTO_INCREMENT, user_name VARCHAR(20) NOT NULL, password VARCHAR(255) NOT NULL, created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY (user_id)); INSERT INTO app_user (user_name, password) VALUES ("qa-actor", "unused");'
docker exec -i "${container_name}" mysql -uroot "${database_name}" < "${migration_file}"

docker exec "${container_name}" mysql -uroot "${database_name}" -e \
    "INSERT INTO book (isbn, title, author, created_at, updated_at) VALUES ('9780306406157', 'Archived uniqueness check', 'QA', NOW(6), NOW(6)); UPDATE book SET archived_at = NOW(6) WHERE book_id = LAST_INSERT_ID();"

if docker exec "${container_name}" mysql -uroot "${database_name}" -e \
    "INSERT INTO book (isbn, title, author, created_at, updated_at) VALUES ('9780306406157', 'Duplicate ISBN', 'QA', NOW(6), NOW(6));" >/dev/null 2>&1; then
    printf 'FAIL: duplicate ISBN including archived row was accepted.\n' >&2
    exit 1
fi

docker exec "${container_name}" mysql -uroot "${database_name}" -e \
    "INSERT INTO book (title, author, created_at, updated_at) VALUES ('Copy parent', 'QA', NOW(6), NOW(6)); SET @book_id = LAST_INSERT_ID(); INSERT INTO book_copy (book_id, barcode, status, reference_only, created_at, updated_at) VALUES (@book_id, 'LIB-QA-UNIQUE', 'AVAILABLE', FALSE, NOW(6), NOW(6));"

if docker exec "${container_name}" mysql -uroot "${database_name}" -e \
    "INSERT INTO book_copy (book_id, barcode, status, reference_only, created_at, updated_at) VALUES (1, 'LIB-QA-UNIQUE', 'AVAILABLE', FALSE, NOW(6), NOW(6));" >/dev/null 2>&1; then
    printf 'FAIL: duplicate barcode was accepted.\n' >&2
    exit 1
fi

if docker exec "${container_name}" mysql -uroot "${database_name}" -e \
    "INSERT INTO book_copy (book_id, barcode, status, reference_only, created_at, updated_at) VALUES (999999, 'LIB-QA-ORPHAN', 'AVAILABLE', FALSE, NOW(6), NOW(6));" >/dev/null 2>&1; then
    printf 'FAIL: orphan copy row was accepted.\n' >&2
    exit 1
fi

docker exec "${container_name}" mysql -uroot "${database_name}" -e \
    "INSERT INTO book_copy_batch_request (actor_user_id, book_id, idempotency_key, payload_fingerprint, response_json, created_at) VALUES (1, 1, 'KeyCase', REPEAT('a', 64), JSON_OBJECT('result', 'first'), NOW(6)), (1, 1, 'keycase', REPEAT('b', 64), JSON_OBJECT('result', 'second'), NOW(6));"
case_count="$(docker exec "${container_name}" mysql -uroot "${database_name}" -Nse \
    "SELECT COUNT(*) FROM book_copy_batch_request WHERE actor_user_id = 1 AND book_id = 1 AND idempotency_key IN ('KeyCase', 'keycase')")"
if [[ "${case_count}" != "2" ]]; then
    printf 'FAIL: idempotency keys are not case-sensitive (count=%s).\n' "${case_count}" >&2
    exit 1
fi

json_type="$(docker exec "${container_name}" mysql -uroot "${database_name}" -Nse \
    "SELECT JSON_TYPE(response_json) FROM book_copy_batch_request WHERE idempotency_key = 'KeyCase'")"
if [[ "${json_type}" != "OBJECT" ]]; then
    printf 'FAIL: MySQL JSON response was not stored as an object (type=%s).\n' "${json_type}" >&2
    exit 1
fi

if docker exec "${container_name}" mysql -uroot "${database_name}" -e \
    "START TRANSACTION; INSERT INTO book_copy (book_id, barcode, status, reference_only, created_at, updated_at) VALUES (1, 'LIB-QA-ROLLBACK', 'AVAILABLE', FALSE, NOW(6), NOW(6)); INSERT INTO book_copy (book_id, barcode, status, reference_only, created_at, updated_at) VALUES (1, 'LIB-QA-UNIQUE', 'AVAILABLE', FALSE, NOW(6), NOW(6));" >/dev/null 2>&1; then
    printf 'FAIL: duplicate-barcode transaction unexpectedly succeeded.\n' >&2
    exit 1
fi
rollback_count="$(docker exec "${container_name}" mysql -uroot "${database_name}" -Nse \
    "SELECT COUNT(*) FROM book_copy WHERE barcode = 'LIB-QA-ROLLBACK'")"
if [[ "${rollback_count}" != "0" ]]; then
    printf 'FAIL: duplicate-barcode rollback left a copy row (count=%s).\n' "${rollback_count}" >&2
    exit 1
fi

printf 'PASS: V30 applied; archived ISBN and barcode uniques, copy FK, binary idempotency keys, native JSON object, and rollback after duplicate write verified.\n'

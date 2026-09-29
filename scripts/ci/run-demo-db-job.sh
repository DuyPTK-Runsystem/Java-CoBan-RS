#!/usr/bin/env bash
set -euo pipefail

for name in DB_JOB_TASK MYSQL_HOST MYSQL_DATABASE MYSQL_USER MYSQL_PWD MYSQL_PRIVATE_ENDPOINT_IP ACA_INFRA_SUBNET_CIDR SEED_TARGET_ID; do
  if [[ -z "${!name:-}" ]]; then
    printf 'Required ACA DB job setting is missing: %s\n' "$name" >&2
    exit 2
  fi
done

if [[ -z "${MYSQL_SSL_CA:-}" ]]; then
  for candidate in \
    /etc/pki/tls/certs/ca-bundle.crt \
    /etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem \
    /etc/ssl/certs/ca-certificates.crt
  do
    if [[ -r "$candidate" ]]; then
      MYSQL_SSL_CA="$candidate"
      break
    fi
  done
fi

if [[ -z "${MYSQL_SSL_CA:-}" || ! -r "$MYSQL_SSL_CA" ]]; then
  printf 'A readable MySQL CA bundle is required; set MYSQL_SSL_CA or install CA certificates.\n' >&2
  exit 2
fi
export MYSQL_SSL_CA

/opt/plan086/verify-demo-private-network.sh
case "$DB_JOB_TASK" in
  preflight)
    : "${EXPECTED_IMAGE:?EXPECTED_IMAGE is required for preflight}"
    : "${OPERATION:?OPERATION is required for preflight}"
    /opt/plan086/demo-seed-preflight.sh
    ;;
  postflight)
    : "${EXPECTED_IMAGE:?EXPECTED_IMAGE is required for postflight}"
    : "${OPERATION:?OPERATION is required for postflight}"
    /opt/plan086/demo-seed-postflight.sh
    ;;
  probe)
    mysql --connect-timeout=10 --ssl-mode=VERIFY_IDENTITY \
      --ssl-ca="$MYSQL_SSL_CA" \
      --host="$MYSQL_HOST" --user="$MYSQL_USER" --database="$MYSQL_DATABASE" \
      --batch --raw --skip-column-names --execute='SELECT 1' | grep -Fx 1 >/dev/null
    echo 'private_mysql_probe=PASS'
    ;;
  *)
    printf 'Unsupported DB_JOB_TASK: %s\n' "$DB_JOB_TASK" >&2
    exit 2
    ;;
esac

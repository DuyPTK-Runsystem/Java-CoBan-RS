#!/usr/bin/env bash
set -euo pipefail

for name in \
  MYSQL_HOST \
  MYSQL_DATABASE \
  MYSQL_USER \
  MYSQL_PWD \
  MYSQL_PRIVATE_ENDPOINT_IP \
  ACA_INFRA_SUBNET_CIDR
do
  if [[ -z "${!name:-}" ]]; then
    printf 'Required private-network setting is missing: %s\n' "$name" >&2
    exit 2
  fi
done

fail() {
  printf 'Private-network verification failed: %s\n' "$1" >&2
  exit 1
}

[[ -n "${MYSQL_SSL_CA:-}" ]] \
  || fail 'MYSQL_SSL_CA must be resolved by run-demo-db-job.sh before verification.'
[[ -r "$MYSQL_SSL_CA" ]] \
  || fail "Configured MySQL CA bundle is not readable: $MYSQL_SSL_CA"

# ---------------------------------------------------------------------------
# DNS verification
# ---------------------------------------------------------------------------
resolved_ips="$(
  getent ahostsv4 "$MYSQL_HOST" \
    | awk '{print $1}' \
    | sort -u
)"

[[ -n "$resolved_ips" ]] \
  || fail "MySQL hostname $MYSQL_HOST did not resolve to IPv4."

resolved_ip_count="$(
  printf '%s\n' "$resolved_ips" \
    | sed '/^[[:space:]]*$/d' \
    | wc -l \
    | tr -d ' '
)"

if [[ "$resolved_ip_count" != '1' || "$resolved_ips" != "$MYSQL_PRIVATE_ENDPOINT_IP" ]]; then
  printf \
    'Expected %s to resolve only to Private Endpoint address %s; got: %s\n' \
    "$MYSQL_HOST" \
    "$MYSQL_PRIVATE_ENDPOINT_IP" \
    "$resolved_ips" >&2
  exit 1
fi

# ---------------------------------------------------------------------------
# Route diagnostic
#
# ACA can expose an overlay/source address that does not belong directly to
# ACA_INFRA_SUBNET_CIDR. Therefore the source IP is diagnostic only and must
# not be used as proof that the Job is outside the configured VNet.
# ---------------------------------------------------------------------------
job_route="$(
  ip route get "$MYSQL_PRIVATE_ENDPOINT_IP" 2>/dev/null \
    | head -n 1 \
    || true
)"

if [[ -n "$job_route" ]]; then
  printf 'Private route diagnostic: %s\n' "$job_route"
fi

# ---------------------------------------------------------------------------
# Verify TLS + hostname identity + actual MySQL connectivity.
# ---------------------------------------------------------------------------
mysql_result="$(
  mysql \
    --connect-timeout=10 \
    --ssl-mode=VERIFY_IDENTITY \
    --ssl-ca="$MYSQL_SSL_CA" \
    --host="$MYSQL_HOST" \
    --user="$MYSQL_USER" \
    --database="$MYSQL_DATABASE" \
    --batch \
    --raw \
    --skip-column-names \
    --execute='SELECT 1'
)" || fail 'ACA Job cannot complete a TLS-verified MySQL query over the private route.'

[[ "$mysql_result" == '1' ]] \
  || fail "Unexpected MySQL connectivity probe result: $mysql_result"

# ---------------------------------------------------------------------------
# Verify the DB verifier identity is SELECT-only.
# ---------------------------------------------------------------------------
/opt/plan086/verify-demo-mysql-readonly.sh \
  || fail 'ACA DB verifier account is not SELECT-only on the configured database.'

printf \
  'private_network=PASS dns=%s endpoint_ip=%s ca=%s tls_mysql=PASS mysql_identity=SELECT_ONLY\n' \
  "$MYSQL_HOST" \
  "$MYSQL_PRIVATE_ENDPOINT_IP" \
  "$MYSQL_SSL_CA"
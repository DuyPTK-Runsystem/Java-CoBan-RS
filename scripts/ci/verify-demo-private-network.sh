#!/usr/bin/env bash
set -euo pipefail

for name in MYSQL_HOST MYSQL_DATABASE MYSQL_USER MYSQL_PWD MYSQL_PRIVATE_ENDPOINT_IP ACA_INFRA_SUBNET_CIDR; do
  if [[ -z "${!name:-}" ]]; then
    printf 'Required private-network setting is missing: %s\n' "$name" >&2
    exit 2
  fi
done

fail() {
  printf 'Private-network verification failed: %s\n' "$1" >&2
  exit 1
}

resolved_ips="$(getent ahostsv4 "$MYSQL_HOST" | awk '{print $1}' | sort -u)"
[[ -n "$resolved_ips" ]] || fail "MySQL hostname $MYSQL_HOST did not resolve to IPv4."
[[ "$(wc -l <<< "$resolved_ips" | tr -d ' ')" == '1' && "$resolved_ips" == "$MYSQL_PRIVATE_ENDPOINT_IP" ]] || {
  printf 'Expected %s to resolve only to Private Endpoint address %s; got: %s\n' "$MYSQL_HOST" "$MYSQL_PRIVATE_ENDPOINT_IP" "$resolved_ips" >&2
  exit 1
}

job_route="$(ip route get "$MYSQL_PRIVATE_ENDPOINT_IP" | head -n 1)"
job_source="$(awk '{ for (i=1; i<=NF; i++) if ($i == "src") { print $(i+1); exit } }' <<< "$job_route")"
[[ -n "$job_source" ]] || fail 'Could not determine the ACA Job source address for the MySQL private route.'
python3 - "$job_source" "$ACA_INFRA_SUBNET_CIDR" <<'PYNET' || fail 'ACA Job source address is outside the expected Container Apps VNet subnet.'
import ipaddress
import sys
if ipaddress.ip_address(sys.argv[1]) not in ipaddress.ip_network(sys.argv[2], strict=True):
    raise SystemExit(1)
PYNET

mysql --connect-timeout=10 --ssl-mode=VERIFY_IDENTITY \
  --host="$MYSQL_HOST" --user="$MYSQL_USER" --database="$MYSQL_DATABASE" \
  --batch --raw --skip-column-names --execute='SELECT 1' | grep -Fx 1 >/dev/null || fail 'ACA Job cannot complete a TLS-verified MySQL query over the private route.'
/opt/plan086/verify-demo-mysql-readonly.sh || fail 'ACA DB verifier account is not SELECT-only on the configured database.'

printf 'private_network=PASS dns=%s endpoint_ip=%s source_ip=%s tls_mysql=PASS mysql_identity=SELECT_ONLY\n' "$MYSQL_HOST" "$MYSQL_PRIVATE_ENDPOINT_IP" "$job_source"

resolved_ips="$(getent ahostsv4 "$MYSQL_HOST" | awk '{print $1}' | sort -u)"
[[ -n "$resolved_ips" ]] || fail "MySQL hostname $MYSQL_HOST did not resolve to IPv4."

[[ "$(wc -l <<< "$resolved_ips" | tr -d ' ')" == '1' \
   && "$resolved_ips" == "$MYSQL_PRIVATE_ENDPOINT_IP" ]] || {
  printf \
    'Expected %s to resolve only to Private Endpoint address %s; got: %s\n' \
    "$MYSQL_HOST" \
    "$MYSQL_PRIVATE_ENDPOINT_IP" \
    "$resolved_ips" >&2
  exit 1
}

# Diagnostic only. The source address visible inside an ACA container
# is not a reliable assertion for membership in the infrastructure subnet.
job_route="$(ip route get "$MYSQL_PRIVATE_ENDPOINT_IP" 2>/dev/null | head -n 1 || true)"
if [[ -n "$job_route" ]]; then
  printf 'Private route diagnostic: %s\n' "$job_route"
fi

mysql --connect-timeout=10 --ssl-mode=VERIFY_IDENTITY \
  --host="$MYSQL_HOST" \
  --user="$MYSQL_USER" \
  --database="$MYSQL_DATABASE" \
  --batch \
  --raw \
  --skip-column-names \
  --execute='SELECT 1' \
  | grep -Fx 1 >/dev/null \
  || fail 'ACA Job cannot complete a TLS-verified MySQL query over the private route.'

/opt/plan086/verify-demo-mysql-readonly.sh \
  || fail 'ACA DB verifier account is not SELECT-only on the configured database.'

printf \
  'private_network=PASS dns=%s endpoint_ip=%s tls_mysql=PASS mysql_identity=SELECT_ONLY\n' \
  "$MYSQL_HOST" \
  "$MYSQL_PRIVATE_ENDPOINT_IP"
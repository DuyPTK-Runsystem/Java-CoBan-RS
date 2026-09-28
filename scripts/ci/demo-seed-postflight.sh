#!/usr/bin/env bash
set -euo pipefail

for name in MYSQL_HOST MYSQL_DATABASE MYSQL_USER MYSQL_PWD SEED_TARGET_ID EXPECTED_IMAGE OPERATION; do
  if [[ -z "${!name:-}" ]]; then
    printf 'Required postflight setting is missing: %s\n' "$name" >&2
    exit 2
  fi
done

mysql_query() {
  mysql --connect-timeout=10 --ssl-mode=VERIFY_IDENTITY \
    --host="$MYSQL_HOST" --user="$MYSQL_USER" --database="$MYSQL_DATABASE" \
    --batch --raw --skip-column-names --execute="$1"
}
marker_rows="$(mysql_query "SELECT seed_key, fixture_version, target_id, deployment_ref FROM app_demo_seed_completion WHERE seed_key = 'DEMO_FIXTURE_PLAN_081'")"
[[ -n "$marker_rows" ]] || { echo 'Completion marker is absent; bootstrap is incomplete.' >&2; exit 1; }
IFS=$'\t' read -r marker_key fixture_version target_id deployment_ref <<< "$marker_rows"
[[ "$marker_key" == 'DEMO_FIXTURE_PLAN_081' && "$fixture_version" == 'PLAN_081_V1' && "$target_id" == "$SEED_TARGET_ID" && -n "$deployment_ref" ]] || {
  echo 'Completion marker does not match the expected fixture and DB target.' >&2
  exit 1
}
if [[ "$OPERATION" == 'bootstrap' && "$deployment_ref" != "$EXPECTED_IMAGE" ]]; then
  echo 'Completion marker deployment ref does not match this bootstrap image.' >&2
  exit 1
fi
if [[ "$OPERATION" == 'private-cutover' ]]; then
  expected_repository="${EXPECTED_IMAGE%:*}"
  marker_prefix="$expected_repository:"
  marker_tag="${deployment_ref#"$marker_prefix"}"
  [[ "$deployment_ref" == "$marker_prefix"* && "$marker_tag" =~ ^[0-9a-f]{40}$ ]] || {
    echo 'Completion marker does not reference an immutable image in the configured ACR repository.' >&2
    exit 1
  }
fi

echo 'Plan 081 completion marker verified from the private MySQL route.'

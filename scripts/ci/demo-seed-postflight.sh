#!/usr/bin/env bash
set -euo pipefail

for name in MYSQL_HOST MYSQL_DATABASE MYSQL_USER MYSQL_PWD SEED_TARGET_ID EXPECTED_IMAGE OPERATION; do
  if [[ -z "${!name:-}" ]]; then
    printf 'Required postflight setting is missing: %s\n' "$name" >&2
    exit 2
  fi
done
if [[ -z "${MYSQL_SSL_CA:-}" || ! -r "$MYSQL_SSL_CA" ]]; then
  echo 'MYSQL_SSL_CA must name a readable CA bundle.' >&2
  exit 2
fi

mysql_query() {
  mysql --connect-timeout=10 --ssl-mode=VERIFY_IDENTITY --ssl-ca="$MYSQL_SSL_CA" \
    --host="$MYSQL_HOST" --user="$MYSQL_USER" --database="$MYSQL_DATABASE" \
    --batch --raw --skip-column-names --execute="$1"
}
marker_query="SELECT seed_key, fixture_version, target_id, deployment_ref FROM app_demo_seed_completion WHERE seed_key = 'DEMO_FIXTURE_PLAN_081'"
marker_rows="$(mysql_query "$marker_query")"
if [[ -z "$marker_rows" && "$OPERATION" == 'bootstrap' ]]; then
  marker_timeout="${POSTFLIGHT_MARKER_TIMEOUT_SECONDS:-900}"
  marker_poll_interval="${POSTFLIGHT_MARKER_POLL_INTERVAL_SECONDS:-15}"
  [[ "$marker_timeout" =~ ^[1-9][0-9]*$ ]] || {
    echo 'POSTFLIGHT_MARKER_TIMEOUT_SECONDS must be a positive integer.' >&2
    exit 2
  }
  [[ "$marker_poll_interval" =~ ^[1-9][0-9]*$ ]] || {
    echo 'POSTFLIGHT_MARKER_POLL_INTERVAL_SECONDS must be a positive integer.' >&2
    exit 2
  }

  marker_started=$SECONDS
  marker_deadline=$((marker_started + marker_timeout))
  while [[ -z "$marker_rows" ]]; do
    marker_elapsed=$((SECONDS - marker_started))
    marker_remaining=$((marker_deadline - SECONDS))
    if (( marker_remaining <= 0 )); then
      echo "Timed out after ${marker_elapsed}s waiting for the Plan 081 completion marker." >&2
      echo 'Postflight timeout evidence: completion marker rows for DEMO_FIXTURE_PLAN_081 = 0.' >&2
      if seed_counts="$(mysql_query "SELECT 'academic_year', COUNT(*) FROM academic_year UNION ALL SELECT 'student', COUNT(*) FROM student UNION ALL SELECT 'teacher', COUNT(*) FROM teacher")"; then
        printf 'Postflight timeout evidence: seed table row counts (table, count):\n%s\n' "$seed_counts" >&2
      else
        echo 'Postflight timeout evidence: could not collect seed table row counts.' >&2
      fi
      exit 1
    fi

    marker_sleep="$marker_poll_interval"
    (( marker_sleep > marker_remaining )) && marker_sleep="$marker_remaining"
    echo "Bootstrap completion marker not present after ${marker_elapsed}s; retrying in ${marker_sleep}s."
    sleep "$marker_sleep"
    marker_rows="$(mysql_query "$marker_query")"
  done
  echo "Bootstrap completion marker appeared after $((SECONDS - marker_started))s."
fi
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

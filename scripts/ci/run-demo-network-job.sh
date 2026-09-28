#!/usr/bin/env bash
set -euo pipefail

for name in AZURE_RESOURCE_GROUP ACA_JOB_NAME DB_VERIFY_IMAGE DB_JOB_TASK MYSQL_HOST MYSQL_DATABASE MYSQL_USER MYSQL_PRIVATE_ENDPOINT_IP ACA_INFRA_SUBNET_CIDR SEED_TARGET_ID; do
  if [[ -z "${!name:-}" ]]; then
    printf 'Required ACA DB verifier setting is missing: %s\n' "$name" >&2
    exit 2
  fi
done

if [[ "$DB_JOB_TASK" != 'preflight' && "$DB_JOB_TASK" != 'postflight' && "$DB_JOB_TASK" != 'probe' ]]; then
  echo 'DB_JOB_TASK must be preflight, postflight or probe.' >&2
  exit 2
fi

for name in OPERATION EXPECTED_IMAGE; do
  if [[ "$DB_JOB_TASK" != 'probe' && -z "${!name:-}" ]]; then
    printf 'Required seed job setting is missing: %s\n' "$name" >&2
    exit 2
  fi
done

request_file="$(mktemp "${RUNNER_TEMP:-/tmp}/plan086-job.XXXXXX.json")"
trap 'rm -f "$request_file"' EXIT

env_json="$(jq -cn \
  --arg task "$DB_JOB_TASK" \
  --arg host "$MYSQL_HOST" \
  --arg database "$MYSQL_DATABASE" \
  --arg user "$MYSQL_USER" \
  --arg privateIp "$MYSQL_PRIVATE_ENDPOINT_IP" \
  --arg subnet "$ACA_INFRA_SUBNET_CIDR" \
  --arg target "$SEED_TARGET_ID" \
  --arg operation "${OPERATION:-}" \
  --arg expectedImage "${EXPECTED_IMAGE:-}" \
  '[
    {name:"DB_JOB_TASK",value:$task},
    {name:"MYSQL_HOST",value:$host},
    {name:"MYSQL_DATABASE",value:$database},
    {name:"MYSQL_USER",value:$user},
    {name:"MYSQL_PWD",secretRef:"mysql-password"},
    {name:"MYSQL_PRIVATE_ENDPOINT_IP",value:$privateIp},
    {name:"ACA_INFRA_SUBNET_CIDR",value:$subnet},
    {name:"SEED_TARGET_ID",value:$target}
  ] + (if $task == "probe" then [] else [{name:"OPERATION",value:$operation},{name:"EXPECTED_IMAGE",value:$expectedImage}] end)')"
jq -n --arg image "$DB_VERIFY_IMAGE" --argjson env "$env_json" '{containers:[{name:"db-verify",image:$image,command:["/bin/bash","/opt/plan086/run-demo-db-job.sh"],resources:{cpu:0.25,memory:"0.5Gi"},env:$env}]}' > "$request_file"

start_result="$(az containerapp job start \
  --name "$ACA_JOB_NAME" --resource-group "$AZURE_RESOURCE_GROUP" \
  --yaml "$request_file" --output json)"
execution="$(jq -r '.name // .properties.name // empty' <<< "$start_result")"
[[ -n "$execution" ]] || { echo 'Azure did not return an execution name for the DB verifier Job.' >&2; exit 1; }
printf 'Started ACA DB verifier execution: %s (%s).\n' "$execution" "$DB_JOB_TASK"

deadline=$((SECONDS + 1800))
while :; do
  execution_json="$(az containerapp job execution show \
    --name "$ACA_JOB_NAME" --resource-group "$AZURE_RESOURCE_GROUP" \
    --job-execution-name "$execution" --output json)"
  status="$(jq -r '.properties.status // .status // empty' <<< "$execution_json")"
  case "$status" in
    Succeeded)
      printf 'ACA DB verifier execution %s completed successfully.\n' "$execution"
      break
      ;;
    Failed|Stopped|Canceled|Cancelled)
      printf 'ACA DB verifier execution %s ended with status %s.\n' "$execution" "$status" >&2
      az containerapp job logs show --name "$ACA_JOB_NAME" --resource-group "$AZURE_RESOURCE_GROUP" \
        --execution "$execution" --container db-verify --tail 200 --format text || true
      exit 1
      ;;
    Running|Processing|Starting|'')
      if (( SECONDS >= deadline )); then
        printf 'Timed out waiting for ACA DB verifier execution %s (last status: %s).\n' "$execution" "${status:-unknown}" >&2
        az containerapp job logs show --name "$ACA_JOB_NAME" --resource-group "$AZURE_RESOURCE_GROUP" \
          --execution "$execution" --container db-verify --tail 200 --format text || true
        exit 1
      fi
      sleep 10
      ;;
    *)
      printf 'ACA DB verifier execution %s returned unknown status: %s.\n' "$execution" "$status" >&2
      az containerapp job logs show --name "$ACA_JOB_NAME" --resource-group "$AZURE_RESOURCE_GROUP" \
        --execution "$execution" --container db-verify --tail 200 --format text || true
      exit 1
      ;;
  esac
done

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  printf 'execution=%s\nstatus=%s\n' "$execution" "$status" >> "$GITHUB_OUTPUT"
fi

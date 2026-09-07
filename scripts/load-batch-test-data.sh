#!/usr/bin/env bash

set -euo pipefail

readonly project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly env_file="$project_dir/docker/.env"
readonly max_attempts=90

if [[ ! -f "$env_file" ]]; then
    echo "Missing docker/.env. Copy docker/.env.example to docker/.env and set MYSQL_ROOT_PASSWORD." >&2
    exit 1
fi

readonly api_host_port="$(sed -n 's/^API_HOST_PORT=//p' "$env_file" | tail -n 1)"

if [[ ! "$api_host_port" =~ ^[0-9]{1,5}$ ]] || ((api_host_port < 1 || api_host_port > 65535)); then
    echo "docker/.env must define API_HOST_PORT as a valid port number." >&2
    exit 1
fi

readonly health_url="http://localhost:$api_host_port/actuator/health"

if ! docker info >/dev/null 2>&1; then
    echo "Docker daemon is not available. Start Docker and try again." >&2
    exit 1
fi

cd "$project_dir"

compose=(docker compose --env-file docker/.env)

"${compose[@]}" up -d --build db api

for ((attempt = 1; attempt <= max_attempts; attempt++)); do
    if curl --fail --silent --show-error "$health_url" >/dev/null; then
        echo "API is healthy. Loading 500 batch test students through Newman..."
        "${compose[@]}" --profile tools run --rm test-data run \
            /etc/newman/Java-CoBan-Batch-Test-Data.postman_collection.json \
            --env-var baseUrl=http://api:8081 \
            --reporters cli
        echo "Test data loaded successfully. The collection added one test user and 500 students."
        exit 0
    fi

    sleep 2
done

echo "API did not become healthy within $((max_attempts * 2)) seconds." >&2
"${compose[@]}" logs --tail=100 api db >&2
exit 1

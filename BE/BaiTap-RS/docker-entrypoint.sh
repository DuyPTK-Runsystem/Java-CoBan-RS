#!/bin/sh

set -eu

database_host="${DATABASE_HOST:-db}"
database_port="${DATABASE_PORT:-3306}"
database_name="${DATABASE_NAME:-java_coban}"
export DATABASE_HOST="$database_host"
export DATABASE_PORT="$database_port"
export DATABASE_NAME="$database_name"
export DATABASE_BOOTSTRAP_USER="${SPRING_DATASOURCE_USERNAME:?SPRING_DATASOURCE_USERNAME must be set}"
export DATABASE_BOOTSTRAP_PASSWORD="${SPRING_DATASOURCE_PASSWORD:?SPRING_DATASOURCE_PASSWORD must be set}"

java -cp /app/db-bootstrap:/app/mysql-connector-j.jar DatabaseBootstrap

unset DATABASE_BOOTSTRAP_USER DATABASE_BOOTSTRAP_PASSWORD
exec java -jar /app/app.jar

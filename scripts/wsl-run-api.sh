#!/usr/bin/env bash
# Sobe a API Spring Boot na porta canónica (default 8084) após MySQL healthy.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

for i in $(seq 1 30); do
  status=$(docker inspect -f '{{.State.Health.Status}}' loja-revenda-mysql 2>/dev/null || echo starting)
  echo "mysql health: $status"
  if [ "$status" = "healthy" ]; then
    break
  fi
  sleep 2
done

set -a
# shellcheck disable=SC1091
source .env
set +a

PORT="${SERVER_PORT:-8084}"

cd backend
export DB_URL DB_USERNAME DB_PASSWORD JWT_SECRET
exec mvn -q spring-boot:run -Dspring-boot.run.arguments=--server.port="${PORT}"

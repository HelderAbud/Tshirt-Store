#!/usr/bin/env bash
set -euo pipefail

echo "==> Infra (MySQL)"
( cd infra && docker compose up -d >/dev/null && docker compose ps )

echo "==> Backend tests"
( cd backend && ( [ -f ./mvnw ] && ./mvnw test || mvn test ) )

echo "==> Frontend lint (se existir)"
( cd frontend && if npm run | grep -q "lint"; then npm run lint; else echo "No lint script found"; fi )

echo "==> OK"

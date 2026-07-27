#!/usr/bin/env bash
# Cria .env a partir do example (se faltar) e sobe MySQL no Compose (host 3308).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [ ! -f .env ]; then
  cp .env.example .env
  JWT=$(openssl rand -base64 32)
  sed -i "s|replace-with-base64-encoded-256-bit-secret|${JWT}|" .env
  echo "created .env"
else
  echo "env exists"
fi

echo "env keys:"
grep -E '^[A-Z_]+=' .env | cut -d= -f1

echo "ports:"
ss -ltn | grep -E ':8084|:3308' || true

cd infra
docker compose --env-file ../.env up -d
docker compose --env-file ../.env ps

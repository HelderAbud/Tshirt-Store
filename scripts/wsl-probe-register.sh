#!/usr/bin/env bash
# Smoke de auth na API canónica (8084).
# Nota: Tshirt-Store MVP ainda NÃO tem POST /register — só login.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

PORT="${SERVER_PORT:-8084}"
BASE="http://127.0.0.1:${PORT}"

echo "ports:"
ss -ltn | grep -E ":${PORT}|:8080|:8081" || true

echo "who:"
for p in "$PORT" 8080 8081; do
  code=$(curl -s -o "/tmp/h${p}.txt" -w "%{http_code}" "http://127.0.0.1:${p}/actuator/health" || true)
  echo "actuator ${p} -> ${code} $(head -c 80 "/tmp/h${p}.txt" 2>/dev/null || true)"
  code2=$(curl -s -o "/tmp/t${p}.txt" -w "%{http_code}" "http://127.0.0.1:${p}/api/health" || true)
  echo "api/health ${p} -> ${code2} $(head -c 80 "/tmp/t${p}.txt" 2>/dev/null || true)"
done

echo "login ${PORT} (esperado 401 com credenciais inválidas se API no ar):"
curl -s -D /tmp/login.h -o /tmp/login.b -w "%{http_code}\n" -X POST "${BASE}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@test.com","password":"wrong-password"}'
echo "headers:"; head -20 /tmp/login.h
echo "body:"; head -c 500 /tmp/login.b; echo

echo "register (não implementado — esperado 401/404/405):"
curl -s -o /tmp/reg.b -w "%{http_code}\n" -X POST "${BASE}/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@test.com","password":"Demo12345!"}' || true
head -c 200 /tmp/reg.b; echo

#!/usr/bin/env bash
# Diagnóstico rápido: OpenAPI auth paths + probes health/login.
# Register ainda não existe neste MVP.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

PORT="${SERVER_PORT:-8084}"
BASE="http://127.0.0.1:${PORT}"

echo "== ${PORT} OpenAPI =="
curl -s "${BASE}/v3/api-docs" -o /tmp/tshirt-openapi.json
python3 - <<'PY'
import json
d = json.load(open("/tmp/tshirt-openapi.json", encoding="utf-8"))
print("title:", d.get("info", {}).get("title"))
print("auth paths:")
for p in sorted(d.get("paths", {})):
    if "auth" in p:
        print(" ", p, list(d["paths"][p].keys()))
PY

echo "== probes =="
curl -s -o /dev/null -w "api_health:%{http_code}\n" "${BASE}/api/health"
curl -s -o /dev/null -w "actuator:%{http_code}\n" "${BASE}/actuator/health"
curl -s -o /dev/null -w "login_empty:%{http_code}\n" -X POST "${BASE}/api/auth/login" -H 'Content-Type: application/json' -d '{}'
curl -s -o /tmp/reg.out -w "register:%{http_code}\n" -X POST "${BASE}/api/auth/register" -H 'Content-Type: application/json' -d '{"email":"demo@test.com","password":"demo12345"}'
head -c 300 /tmp/reg.out; echo

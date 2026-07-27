#!/usr/bin/env bash
# Verifica .gitignore de .env + paths OpenAPI Product/catálogo na API 8084.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

PORT="${SERVER_PORT:-8084}"
BASE="http://127.0.0.1:${PORT}"

echo "gitignore .env:"
grep -n '\.env' .gitignore || echo "MISSING .env in gitignore"

echo "swagger:"
curl -s -o /dev/null -w "%{http_code}\n" "${BASE}/swagger-ui.html"
curl -s "${BASE}/v3/api-docs" | python3 -c "import sys,json; d=json.load(sys.stdin); print('paths', len(d.get('paths',{}))); print('has_admin', '/api/admin/products' in d.get('paths',{})); print('has_catalog', '/api/catalog/products' in d.get('paths',{}))"

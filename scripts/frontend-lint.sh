#!/usr/bin/env bash
set -euo pipefail
cd frontend
if npm run | grep -q "lint"; then
  npm run lint
else
  echo "No lint script found in package.json"
fi

#!/usr/bin/env bash
set -euo pipefail
cd backend
if [ -f "./mvnw" ]; then
  ./mvnw test
else
  mvn test
fi

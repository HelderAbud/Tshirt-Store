#!/usr/bin/env bash
set -euo pipefail
cd backend
if [ -f "./mvnw" ]; then
  ./mvnw spring-boot:run
else
  mvn spring-boot:run
fi

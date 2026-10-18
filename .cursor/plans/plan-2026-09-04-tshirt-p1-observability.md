# Plan — P1-02 Observabilidade mínima

**Trilha:** Normal · **Branch:** `feature/p1-problem-details` · **Data:** 2026-09-04

## Scope

- Expor Actuator: `health`, `info`, `metrics`
- Probes liveness/readiness habilitados
- `health` + `info` públicos; `metrics` só ADMIN
- `info.app` básico no YAML
- Teste MockMvc + nota no README + BACKLOG

## Out of scope

- Prometheus/Grafana, APM, tracing, P1-03

## Verification

`cd backend && mvn test`

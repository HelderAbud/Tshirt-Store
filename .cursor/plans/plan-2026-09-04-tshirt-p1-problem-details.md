# Plan — P1-01 Problem Details (RFC 7807)

**Trilha:** Normal · **Status:** aprovado (melhorias viáveis) · **Data:** 2026-09-04

## Problem

Erros da API usam JSON ad-hoc (`status` + `error`). P1-01 pede padronização com Problem Details.

## Scope

- Refatorar `GlobalExceptionHandler` para `ProblemDetail` (Spring Boot 3).
- Validação: `detail` = `Validation failed` + extensão `errors` (mapa campo → msg).
- `type` = `about:blank`; `title` padrão do status; `detail` = mensagem atual.
- Atualizar testes que assertam `$.error` → `$.detail`.
- Nota curta no README sobre formato de erro.

## Out of scope

- P1-02 observabilidade, P1-03 README “empresa”
- URIs custom por tipo de erro
- Body custom de 401/403 do Security

## Acceptance

- [x] Handlers devolvem `ProblemDetail` com `status` + `detail`
- [x] Validação inclui extensão `errors`
- [x] `mvn test` verde
- [x] BACKLOG P1-01 marcado

## Verification

```bash
cd backend && mvn test
```

## Fatias

1. Teste ProblemDetail (not found / conflict / validation) + handler
2. Migrar asserts `$.error` → `$.detail`
3. README + BACKLOG

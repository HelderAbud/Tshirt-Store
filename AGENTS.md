# AGENTS.md — Instruções para agentes de IA

Este arquivo orienta agentes de IA (Cursor, Copilot, etc.) ao trabalhar neste repositório.

## Projeto

- **Tshirt-Store** — Loja de revenda (MVP) em monorepo.
- Backend: Java 21, Spring Boot 3, Maven, MySQL, Flyway.
- Frontend: Next.js (planejado).
- CI: GitHub Actions (build, Spotless, testes unitários).

## Regras obrigatórias

1. **GitFlow** — O projeto usa GitFlow. Ver `docs/GITFLOW.md` e `.cursor/rules/gitflow.mdc`.
2. **Confirmação** — Nunca executar tarefas sem antes confirmar com o usuário (ver `.cursor/rules/java-spring-stack.mdc`).
3. **Convenções** — Seguir as regras em `.cursor/rules/` (backend, frontend, java-spring-stack, gitflow).

## Estrutura principal

```
backend/          # API Spring Boot
docs/             # Documentação (BACKLOG, GITFLOW, etc.)
.github/          # CI/CD, templates de PR/Issue
scripts/          # Scripts de dev (up.sh, backend-test.sh, etc.)
```

## Referências rápidas

- Backlog: `docs/BACKLOG.md`
- GitFlow: `docs/GITFLOW.md`
- Regras Cursor: `.cursor/rules/`

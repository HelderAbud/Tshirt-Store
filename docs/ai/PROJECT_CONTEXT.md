# Project Context — Loja Revenda (fonte de verdade)

## Stack fixa
Backend: Java 21 + Maven + Spring Boot 3 (Web, Validation, Security JWT, Data JPA/Hibernate, Actuator)  
DB: MySQL 8 + Flyway  
Docs: springdoc-openapi (Swagger)  
Testes: JUnit5, Mockito, Testcontainers (MySQL), MockMvc  
Qualidade/CI: Checkstyle, SpotBugs, PMD, JaCoCo (>= 60% inicial), OWASP Dependency-Check  
Frontend: Next.js (App Router) + React + TypeScript + Tailwind + ESLint/Prettier

## Estrutura
- `infra/` docker-compose (mysql)
- `backend/` API Spring
- `frontend/` Next.js
- `docs/BACKLOG.md` backlog principal
- `docs/WORKFLOW.md` fluxo de trabalho
- `docs/ai/*` prompts/checklists

## Regras obrigatórias
- Mudanças pequenas e incrementais
- Se tocar schema: **sempre** migration Flyway
- Se criar/alterar endpoint: **sempre** testes (unit + integração quando aplicável)
- Evitar N+1 (fetch join/EntityGraph quando necessário)
- Transações explícitas em fluxos críticos (pedido/pagamento/estoque)
- Não inventar requisito: se houver ambiguidade, faça 2–4 perguntas objetivas

## API
- DTOs (não expor Entity)
- Validação Bean Validation + regras no service
- Erros padronizados (Problem Details ou formato consistente)
- Status corretos: 200/201/204/400/401/403/404/409/422

## Segurança
- JWT HS256
- Front guarda em cookie httpOnly
- Roles: ADMIN e CUSTOMER
- `/admin/**` requer ADMIN

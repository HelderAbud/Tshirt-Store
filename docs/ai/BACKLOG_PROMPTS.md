# BACKLOG_PROMPTS — prompts por tarefa (copiar/colar no Cursor)

> Sempre comece com:
> “Use `docs/ai/PROJECT_CONTEXT.md` e siga `docs/ai/DEFINITION_OF_DONE.md`.”

---

## B0-01 — Estrutura do repo (monorepo)
**Prompt**
“Use `docs/ai/PROJECT_CONTEXT.md` e siga `docs/ai/DEFINITION_OF_DONE.md`.
Crie a estrutura do monorepo:
- backend/
- frontend/
- infra/
- docs/
Adicione README com como rodar (mínimo) e um `.gitignore` adequado.
Não implemente features; só base.”

## B0-02 — Docker Compose MySQL
**Prompt**
“Use o contexto do projeto.
Crie `infra/docker-compose.yml` com MySQL 8 (porta 3306) e volume persistente.
Inclua usuário/senha/db por env e um arquivo `.env.example`.
No final, explique como subir e como conectar.”

## B0-03 — Backend base + health
**Prompt**
“Crie um backend Spring Boot 3 (Java 21, Maven) em `backend/` com Actuator.
Configure conexão com MySQL do compose via `application-local.yml`.
Inclua `/actuator/health` funcionando.
Liste comandos para rodar.”

## B0-04 — Flyway + migration inicial
**Prompt**
“Habilite Flyway no backend.
Crie migration `V1__init.sql` com tabela `users` (id, name, email UNIQUE, password_hash, role, created_at).
Garanta que ao subir o app a migration roda.
Adicione teste simples (context loads + DB).”

## B0-05 — Swagger
**Prompt**
“Configure springdoc-openapi.
Garanta Swagger UI em `/swagger-ui/index.html`.
Inclua um endpoint simples de exemplo (ex.: GET /version) e apareça no Swagger.”

## B0-06 — Qualidade/CI
**Prompt**
“Configure no `backend/pom.xml`:
- checkstyle
- spotbugs
- pmd
- jacoco (mínimo 60%)
- owasp dependency-check
Crie GitHub Actions em `.github/workflows/ci.yml` para rodar `mvn -B verify` e falhar se gates falharem.
No final, liste como rodar localmente.”

---

## B1-01 — Auth (register/login) + JWT
**Prompt**
“Implemente auth com JWT HS256:
- POST /auth/register (CUSTOMER)
- POST /auth/login
Use BCrypt.
Retorne JWT (depois o front colocará em cookie httpOnly).
Inclua testes unit do service e 1 teste de integração (login OK e senha inválida).
Erros padronizados.”

## B1-02 — Roles
**Prompt**
“Implemente autorização por role:
- /admin/** exige ADMIN
- endpoints de carrinho/pedido exigem CUSTOMER
Crie teste de integração garantindo 401/403 corretos.”

---

## B2-01 — CRUD Product (ADMIN)
**Prompt**
“Implemente Product (ADMIN):
- POST /admin/products
- PUT /admin/products/{id}
- GET /admin/products/{id}
Com validação e erros padronizados.
Crie migrations Flyway e testes de integração (create + get).”

## B2-02 — Variants
**Prompt**
“Implemente ProductVariant:
- POST /admin/products/{id}/variants
Campos: color, size, sku UNIQUE, price, stockQty >= 0.
Crie migrations e índices (sku, product_id).
Testes cobrindo SKU duplicado (409) e validação (400).”

## B2-03 — Catálogo público
**Prompt**
“Implemente catálogo público:
- GET /catalog/products?page&size&q=
- GET /catalog/products/{id}
Com paginação e busca simples.
Evite N+1. Adicione teste de integração (list + detail).”

---

## B3-01 — Carrinho
**Prompt**
“Implemente carrinho persistido por usuário:
- POST /cart/items (variantId, qty)
- GET /cart
- PATCH/DELETE item
Regra: qty > 0 e <= stock disponível.
Inclua migrations e testes (unit + integração).”

## B3-02 — Criar pedido
**Prompt**
“Implemente criação de pedido:
- POST /orders (gera order + items do carrinho)
Calcula total corretamente, status CREATED e limpa carrinho.
Use transação.
Teste de integração do fluxo completo.”

## B3-03 — Status pedido (ADMIN)
**Prompt**
“Implemente atualização de status:
- PATCH /admin/orders/{id}/status
Transições válidas: CREATED->PAID->SHIPPED->DELIVERED, e CANCEL.
Valide transições (não pular).
Teste unitário das regras e integração do endpoint.”

## B3-04 — Histórico cliente
**Prompt**
“Implemente histórico:
- GET /orders (do usuário logado) com paginação.
Teste de integração.”

---

## B4-01 — Pagamento mock + idempotência
**Prompt**
“Implemente pagamento mock:
- POST /payments/mock/confirm?orderId=
Atualiza payment e order para PAID.
Idempotência: chamar 2x não duplica efeitos.
Teste de integração garantindo idempotência.”

## B4-02 — Baixa estoque
**Prompt**
“Implemente baixa de estoque ao pagar:
Ao confirmar pagamento, reduzir stockQty.
Se insuficiente, responder 409 e não confirmar.
Use transação e lock otimista/pessimista (escolha e justifique).
Teste cobrindo conflito.”

---

## F1-01..F1-04 — Front Next (MVP)
**Prompt base**
“Implemente no frontend Next.js (App Router, TS, Tailwind):
- tratar loading/empty/error
- fetch wrapper com tratamento de status
- integração com backend (Swagger como referência)
Faça em incrementos pequenos e liste como testar manualmente.”

---

## P1-01..P1-03 — Polimento
**Prompt base**
“Faça melhorias mínimas para profissionalizar:
- erros padronizados (Problem Details)
- observabilidade (request-id, métricas básicas, health db)
- README final (como rodar, como testar, trade-offs).”

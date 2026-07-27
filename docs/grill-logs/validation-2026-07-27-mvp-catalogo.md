# Validation — Tshirt-Store Opção B MVP (roles + Product + catálogo)

**Data:** 2026-07-27  
**Branch:** `feature/b102-admin-roles-product`  
**Trilha:** Helder Normal  
**Plano:** `.cursor/plans/plan-2026-07-27-tshirt-mvp-b.md`

## O que foi entregue

- B1-02: `SecurityFilterChain` — `/api/admin/**` exige ADMIN; `/api/catalog/**` público
- Flyway `V3__create_products_table.sql` + entity `Product`
- CRUD admin: `POST/GET/PUT/DELETE /api/admin/products`
- Catálogo: `GET /api/catalog/products` (paginado) e `GET /api/catalog/products/{id}`
- Bean Validation (`spring-boot-starter-validation`) + handler 400/404/409
- Docs: decisão B, TRILHA Dias 1–6, README, BACKLOG B1-02/B2-01/B2-03

## Verificação

```text
cd backend
mvn test            # 20+ testes, BUILD SUCCESS
mvn spotless:check  # BUILD SUCCESS
```

## Critérios de aceite

| Critério | Status |
|----------|--------|
| CUSTOMER → 403 em POST admin | OK (`AdminProductAuthorizationTest`) |
| Sem token → 401 | OK |
| ADMIN CRUD + SKU 409 + validação 400 | OK (`AdminProductControllerTest`) |
| Catálogo público + 404 | OK (`CatalogProductControllerTest`) |
| OpenAPI paths Product | OK (`OpenApiDocsTest`) |

## Fora de escopo (residual)

- B2-02 variantes, B3 carrinho/pedidos, F1 frontend, deploy, screenshot Swagger

## Riscos residuais

- Sem seed de ADMIN em runtime — login depende de users já na BD (como antes)
- Screenshot Swagger ainda pendente na TRILHA Dia 6
- Commit/push/PR só com pedido explícito

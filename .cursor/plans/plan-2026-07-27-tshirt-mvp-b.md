# Plano — Tshirt-Store Opção B (MVP auth + catálogo)

**Data:** 2026-07-27  
**Trilha:** Helder Normal · skills: to-spec → tdd → slice-verification  
**Branch:** `feature/b102-admin-roles-product`

## Decisão HITL

| Opção | Escolhida |
|-------|-----------|
| A — PAUSAR | Não (revogada em 2026-07-27) |
| **B — MVP mínimo** | **Sim** |
| C — Retomar completo | Não |

**Motivo:** entregar roles + Product/catálogo no portfólio sem carrinho/front (evitar overlap LojApp).

## Contrato mínimo de endpoints

### Admin (role ADMIN)

- `POST /api/admin/products` — criar
- `GET /api/admin/products/{id}` — detalhe admin
- `PUT /api/admin/products/{id}` — atualizar
- `DELETE /api/admin/products/{id}` — apagar

### Catálogo público (sem auth)

- `GET /api/catalog/products?page=&size=` — listagem paginada
- `GET /api/catalog/products/{id}` — detalhe

### Product (campos)

`id`, `name`, `sku` UNIQUE, `price`, `stock_qty` (>= 0), `created_at`, `updated_at`

## Fora de escopo

Variantes, carrinho, pedidos, pagamento, frontend Next.js, deploy.

## Fatias

1. Docs Dia 1 (este plano + TRILHA/README)
2. B1-02 SecurityFilterChain `/api/admin/**`
3. Flyway V3 `products`
4. B2-01 CRUD admin
5. B2-03 Catálogo público
6. OpenAPI + BACKLOG + grill-log

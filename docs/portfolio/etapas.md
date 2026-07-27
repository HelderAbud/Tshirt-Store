# Etapas do MVP — Tshirt-Store (Opção B)

Documento de portfólio com as **5 etapas honestas** do backend entregue até 2026-07-27.  
Não inclui carrinho, pedidos, pagamento nem frontend (adiados).

Plano: [`.cursor/plans/plan-2026-07-27-tshirt-mvp-b.md`](../../.cursor/plans/plan-2026-07-27-tshirt-mvp-b.md)  
Backlog: [`docs/BACKLOG.md`](../BACKLOG.md)

---

## 1. Setup + MySQL + Flyway

- Monorepo com `backend/` (Spring Boot 3, Java 21) e `infra/` (Docker Compose MySQL 8)
- Healthcheck, Actuator, Spotless e CI no GitHub Actions
- Flyway versiona o schema (`V1` placeholder, `V2` users, `V3` products)
- Testes usam H2 em memória (`MODE=MySQL`) com perfil `test`

**Prova:** `GET /api/health`, `mvn test`, workflow CI.

---

## 2. JWT login

- `POST /api/auth/login` → `{ token, email, role }`
- Filtro Bearer (`JwtAuthenticationFilter`) + claim `role` no token (HS256)
- `GET /api/me` para o utilizador autenticado
- Segredo via `JWT_SECRET` (Base64, ≥ 256 bits) — sem hardcode

**Prova:** `AuthControllerTest`, `JwtServiceTest`, `MeControllerTest`.

---

## 3. Roles ADMIN / CUSTOMER

- Enum `UserRole`: `ADMIN`, `CUSTOMER`
- `SecurityFilterChain`:
  - `/api/admin/**` → `hasRole("ADMIN")`
  - `/api/catalog/**` → `permitAll`
  - resto autenticado (exceto login/health/Swagger)

**Prova:** CUSTOMER em `POST /api/admin/products` → **403**; sem token → **401** (`AdminProductAuthorizationTest`).

---

## 4. CRUD Product (ADMIN)

- Tabela `products`: `name`, `sku` UNIQUE, `price`, `stock_qty`, timestamps
- Endpoints:
  - `POST /api/admin/products`
  - `GET /api/admin/products/{id}`
  - `PUT /api/admin/products/{id}`
  - `DELETE /api/admin/products/{id}`
- Bean Validation + erros 400 / 404 / 409 (SKU duplicado)

**Prova:** `AdminProductControllerTest`; migration `V3__create_products_table.sql`.

---

## 5. Catálogo público GET

- Sem autenticação:
  - `GET /api/catalog/products?page=&size=` (paginado)
  - `GET /api/catalog/products/{id}`
- Id inexistente → **404**
- OpenAPI/Swagger documenta admin e catálogo

**Prova:** `CatalogProductControllerTest`, `OpenApiDocsTest`; screenshot [`docs/screenshots/swagger-products.png`](../screenshots/swagger-products.png) (contrato em `docs/screenshots/openapi-products.json`; captura live recomendada quando MySQL estiver disponível).

---

## Fora deste MVP

| Item | Estado |
|------|--------|
| B2-02 Variantes | Pendente |
| B3 Carrinho / pedidos | Pendente (evitar overlap LojApp) |
| F1 Frontend Next.js | Explicitamente adiado |
| Deploy público | Opcional (Dia 10) |

---

## Pitch (uma linha)

API e-commerce de camisetas — auth JWT com roles, CRUD Product admin e catálogo público (MVP backend, sem carrinho).

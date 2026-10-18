# Validation — A1 backoffice fase 1

**Data:** 2026-09-04  
**Branch:** `feature/admin-backoffice`

## Verificado

- [x] `mvn test` verde (inclui `AdminInventoryControllerTest`, `AdminSaleControllerTest`, Actuator/Problem Details)
- [x] Migration Flyway `V9__inventory_movements_and_sales.sql`
- [x] Endpoints admin inventory + sales
- [x] SPA `frontend/` (HTML/JS) documentada em `frontend/README.md`
- [x] CORS `http://localhost:3000`

## Smoke manual (quando MySQL/API no ar)

1. `cd infra && docker compose --env-file ../.env up -d`
2. `cd backend && mvn spring-boot:run`
3. `cd frontend && python -m http.server 3000`
4. Login `admin@demo.local` / `Demo12345!`
5. Criar produto+variante no Swagger se estoque vazio
6. Ajustar estoque → registrar venda → ver barra no dashboard

## Riscos residuais

- `npm install` / Next.js não validado neste ambiente (rede/TLS); fontes Next em `frontend/src` + `package.json` são opcionais (A1-04).
- Dois fluxos de baixa de estoque: e-commerce (`orders` pay) vs balcão (`sales`).

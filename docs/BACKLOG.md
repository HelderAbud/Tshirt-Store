# BACKLOG — Loja Revenda (MVP)

> Ordem sugerida: **B0 → B1 → B2 → B3 → B4 → P1 → A1 (backoffice) → F2 vitrine → F3 NF-e**

Marque [x] quando concluir.  
Dica: sempre execute em PR pequeno.

---

## Marco B0 — Setup e fundação
- [x] **B0-01** Criar repositório e estrutura (monorepo)
- [x] **B0-02** Docker Compose com MySQL
- [x] **B0-03** Backend Spring Boot base + healthcheck
- [x] **B0-04** Flyway habilitado + primeira migration
- [x] **B0-05** Swagger (OpenAPI) ligado
- [x] **B0-06** Qualidade/CI (gates obrigatórios)

## Marco B1 — Auth + usuários
- [x] **B1-01** Cadastro/Login (JWT) — `POST /api/auth/register` (CUSTOMER) + `POST /api/auth/login`
- [x] **B1-02** Autorização por role (ADMIN/CUSTOMER)

## Marco B2 — Catálogo
- [x] **B2-01** CRUD de Product (ADMIN)
- [x] **B2-02** Variações (size/color/sku/price/stock)
- [x] **B2-03** Catálogo público (listagem + detalhe)

## Marco B3 — Carrinho e pedidos
- [x] **B3-01** Carrinho persistido por usuário
- [x] **B3-02** Criar pedido a partir do carrinho
- [x] **B3-03** Atualizar status do pedido (ADMIN)
- [x] **B3-04** Histórico do cliente

## Marco B4 — Pagamento mock + estoque
- [x] **B4-01** Pagamento mock (simulado) + idempotência
- [x] **B4-02** Baixa de estoque ao pagar

## Marco P1 — Polimento profissional
- [x] **P1-01** Padronizar erros (Problem Details)
- [x] **P1-02** Observabilidade mínima
- [x] **P1-03** Documentação final (README “de empresa”)

## Marco A1 — Backoffice loja única (fase 1)
- [x] **A1-01** Ajuste de estoque + low-stock + listagem (`/api/admin/inventory/*`)
- [x] **A1-02** Venda de balcão + série diária (`/api/admin/sales`, `/daily`)
- [x] **A1-03** Front admin (login → estoque → venda → dashboard) em `frontend/`
- [x] **A1-04** (opcional) Migrar SPA para Next.js quando `npm install` estiver ok

## Marco F2 — Vitrine cliente (fase 2)
- [x] **F2-01** UI catálogo/carrinho/checkout sobre `/api/catalog` e `/api/cart`

## Marco F3 — NF-e (fase 3)
- [x] **F3-01** Import/emissão mínima + impacto em estoque

## Marco F1 — Frontend Next.js (legado / adiado)
- [ ] **F1-01** Setup Next + páginas base — *substituído parcialmente por A1-03 SPA; Next opcional em A1-04*
- [ ] **F1-02** Carrinho no front (integração com API) — *fase 2*
- [ ] **F1-03** Auth no front (cookie httpOnly) — *fase 2*
- [ ] **F1-04** Admin mínimo (produtos e pedidos) — *coberto por A1*

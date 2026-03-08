# BACKLOG — Loja Revenda (MVP)

> Ordem sugerida para acelerar: **B0 → B1 → B2 → B3 → B4 → F1 → P1**

Marque [x] quando concluir.  
Dica: sempre execute em PR pequeno.

---

## Marco B0 — Setup e fundação
- [x] **B0-01** Criar repositório e estrutura (monorepo)
- [x] **B0-02** Docker Compose com MySQL
- [x] **B0-03** Backend Spring Boot base + healthcheck
- [x] **B0-04** Flyway habilitado + primeira migration
- [ ] **B0-05** Swagger (OpenAPI) ligado
- [ ] **B0-06** Qualidade/CI (gates obrigatórios)

## Marco B1 — Auth + usuários
- [ ] **B1-01** Cadastro/Login (JWT)
- [ ] **B1-02** Autorização por role (ADMIN/CUSTOMER)

## Marco B2 — Catálogo
- [ ] **B2-01** CRUD de Product (ADMIN)
- [ ] **B2-02** Variações (size/color/sku/price/stock)
- [ ] **B2-03** Catálogo público (listagem + detalhe)

## Marco B3 — Carrinho e pedidos
- [ ] **B3-01** Carrinho persistido por usuário
- [ ] **B3-02** Criar pedido a partir do carrinho
- [ ] **B3-03** Atualizar status do pedido (ADMIN)
- [ ] **B3-04** Histórico do cliente

## Marco B4 — Pagamento mock + estoque
- [ ] **B4-01** Pagamento mock (simulado) + idempotência
- [ ] **B4-02** Baixa de estoque ao pagar

## Marco F1 — Frontend Next.js (MVP)
- [ ] **F1-01** Setup Next + páginas base
- [ ] **F1-02** Carrinho no front (integração com API)
- [ ] **F1-03** Auth no front (cookie httpOnly)
- [ ] **F1-04** Admin mínimo (produtos e pedidos)

## Marco P1 — Polimento profissional
- [ ] **P1-01** Padronizar erros (Problem Details)
- [ ] **P1-02** Observabilidade mínima
- [ ] **P1-03** Documentação final (README “de empresa”)

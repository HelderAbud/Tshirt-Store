# Trilha dia a dia — T-Shirt Store

> **Metodologia:** [Helder Method v1.2](../Agentes/helder-method-v1.2-resumo-compartilhavel.md) + [skills-pessoal](../Agentes/skills-pessoal/skills-pessoal/README-pt_br.md) ([WORKFLOW](../Agentes/skills-pessoal/skills-pessoal/WORKFLOW.md))  
> **Iniciativa:** **MVP mínimo** (auth + catálogo) — Opção B (2026-07-27)  
> **Triagem Helder:** **Normal** (`tdd` + `mvn test`)  
> **Estado:** B0 ✅ · B1 ✅ · B2-01/B2-03 ✅ · B2-02/B3+/F1 pendentes ([`docs/BACKLOG.md`](docs/BACKLOG.md))  
> **Custo:** R$ 0  
> **Plano ativo:** [`.cursor/plans/plan-2026-07-27-tshirt-mvp-b.md`](.cursor/plans/plan-2026-07-27-tshirt-mvp-b.md)

---

## Decisão no Dia 1 (obrigatória)

| Opção | Descrição | Próximo passo |
|-------|-----------|---------------|
| A — PAUSAR | Foco em LojApp + HH Financeiro | Dias 1–2 Opção A |
| **B — MVP mínimo** (**escolhida 2026-07-27**) | B1-02 roles + B2 Product/catálogo | Dias 1–14 abaixo |
| C — Retomar completo | Só após LojApp deploy + HH deploy | Reabrir trilha |

**HITL Dia 1:** decisão registada em `.cursor/plans/plan-2026-07-09-tshirt-decisao.md` e plano B em `plan-2026-07-27-tshirt-mvp-b.md`.

---

## Como usar

- Trilha **enxuta** — não competir com LojApp (estoque/vendas/NFe).
- Planos: `.cursor/plans/plan-YYYY-MM-DD-tshirt-*.md`.
- Se **PAUSAR:** README honesto + arquivar expectativas.
- **Opção A** = Fast path (Simple). **Opção B** = Core Workflow completo (`tdd` + `mvn test`).
- Commit/push/PR só com pedido explícito (HITL).

### Helder → skills-pessoal

| Trilha Helder | Caminho |
|---------------|---------|
| **Simple** (Opção A) | Fast path: fazer → verificar → resumir |
| **Normal** (Opção B) | `to-spec` → `to-issues` → `tdd` → `slice-verification` → `code-review` |
| **Complex** | igual Normal + HITL entre fases |
| **Hotfix** | `diagnose` → patch mínimo → regressão → só então retomar |

### Core Workflow (mapa) — Opção B

| Fase | Skill |
|------|-------|
| Spec | `to-spec` |
| Plan | `to-issues` |
| Branch | `git-workflow-and-versioning` |
| Build | `tdd` |
| Verify | `slice-verification` (`mvn test` / Spotless) |
| Review | `code-review` |
| Simplify | `code-simplification` |
| Ship | `finishing-a-development-branch` |

---

## Opção A — PAUSAR (2 dias)

### Dia 1 — Honestidade no README 📋

**Trilha:** Simple

**Tarefas**
- [x] Banner no README: *"Desenvolvimento pausado — prioridade LojApp e HH Financeiro"*
- [x] Atualizar tabela portfólio: estado real (auth + health; catálogo pendente)
- [x] Decisão registrada no plano

**Validação:** README não promete e-commerce completo.

**Evidência:** `.cursor/plans/plan-2026-07-09-tshirt-decisao.md`

---

### Dia 2 — Arquivo backlog + link

- [ ] Data "pausado em" em [`docs/BACKLOG.md`](docs/BACKLOG.md)
- [ ] Remover de destaque LinkedIn até retomar
- [ ] `docs/grill-logs/validation-YYYY-MM-DD-pausa.md`

**FIM trilha Opção A.**

---

## Opção B — MVP mínimo (14 dias)

> Só execute se escolheu **B** no Dia 1.

### Dia 1 — Decisão + plano B2 escopo 📋

| Trilha | Normal |

- [x] Plano: roles + Product CRUD apenas (sem carrinho/front)
- [x] Contrato mínimo: endpoints Product em bullet no plano
- [x] **Não** iniciar F1 frontend nesta trilha

---

### Dia 2 — B1-02 Roles ADMIN/CUSTOMER

**Fatia vertical:** `@PreAuthorize` ou equivalente em rotas admin.

- [x] TDD: teste que CUSTOMER não cria produto
- [x] Atualizar README autenticação
- [x] `mvn test` verde

**Skills:** `to-spec` → `to-issues` → `tdd` → `slice-verification`.

---

### Dia 3 — Migration Product (se não existir)

- [x] Flyway migration tabela `product`
- [x] Campos mínimos: name, price, stock, sku
- [x] **HITL:** revisar migration antes de merge

---

### Dia 4 — B2-01 CRUD Product (ADMIN)

- [x] POST/GET/PUT/DELETE `/api/products` (ou path padrão do projeto)
- [x] DTOs + validação Bean Validation
- [x] Testes MockMvc

---

### Dia 5 — B2-03 Catálogo público (GET list/detail)

**Fatia:** leitura pública sem auth vs escrita admin.

- [x] GET listagem paginada
- [x] GET by id
- [x] Testes de autorização

---

### Dia 6 — OpenAPI + README

- [x] Swagger documenta Product
- [x] Screenshot `docs/screenshots/swagger-products.png`
- [x] Badge CI (já existe workflow)

---

### Dia 7 — `docs/portfolio/etapas.md`

**5 etapas (honestas)**
1. Setup + MySQL + Flyway  
2. JWT login  
3. Roles ADMIN/CUSTOMER  
4. CRUD Product  
5. Catálogo público GET  

- [x] `docs/portfolio/etapas.md` criado
---

### Dia 8 — Spotless + CI verde

- [ ] `mvn test` + Spotless check
- [ ] Corrigir CI se falhar (Hotfix)

---

### Dia 9 — Diagrama + pitch

- [ ] Mermaid: Client → API → MySQL
- [ ] Pitch: *API e-commerce camisetas — auth JWT + catálogo (MVP)*

---

### Dia 10 — Deploy opcional (Render)

- [ ] Só API + MySQL free tier
- [ ] Swagger URL ou "local only"

---

### Dias 11–12 — Validation + ENTREVISTAS

- [ ] Criar `docs/ENTREVISTAS.md` (3 Q&A JWT, roles, Product)
- [ ] `docs/grill-logs/validation-YYYY-MM-DD-mvp-catalogo.md`

---

### Dias 13–14 — LinkedIn + PAUSAR front

- [ ] Post curto: MVP backend catálogo
- [ ] Registrar no BACKLOG: F1 frontend **explicitamente adiado**
- [ ] **Não** iniciar Next.js até LojApp estável

**DoD Opção B:** CRUD Product + roles + CI + docs — sem carrinho.

---

## Opção C — Retomar depois

**Pré-requisitos**
- [ ] LojApp com URL demo
- [ ] HH Financeiro com URL demo
- [ ] Revisar overlap: carrinho/pedidos fazem sentido aqui vs LojApp?

**Então:** nova trilha `TRILHA-DIA-A-DIA-v2.md` para B3 carrinho + F1 front.

---

## Prompt base Cursor

```text
T-Shirt Store — Dia N do TRILHA-DIA-A-DIA.md.
Opção [A/B/C] escolhida no plano YYYY-MM-DD-tshirt-decisao.md.
Helder Simple (A) ou Normal (B) + skills-pessoal.
Opção A: fast path. Opção B: to-spec → to-issues → tdd → slice-verification (mvn test).
Não expandir para frontend sem aprovação explícita.
Overlap LojApp: preferir pausar se escopo duplicar estoque/vendas.
```

---

## Calendário resumido

| Opção | Dias | Resultado |
|-------|------|-----------|
| A Pausar | 1–2 | README honesto |
| B MVP | 1–14 | Auth + Product + docs |
| C Retomar | TBD | Após outros carros-chefe |

---

*Trilha v1.1 — 2026-07-09 — Helder v1.2 + skills-pessoal*

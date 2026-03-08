# Agentes, Skills e Rules no Cursor — Visão detalhada

Este documento explica em profundidade como o projeto está organizado para orientar o Cursor (e você) na implementação da Loja de Revenda.

---

## 1. Conceitos: Rules, Skills e Agent docs

### O que é cada coisa?

| Conceito | Arquivo | O que faz | Quando entra em jogo |
|----------|---------|-----------|----------------------|
| **Rule** | `.mdc` em `.cursor/rules/` | Define padrões, stack e convenções. O Cursor aplica automaticamente. | **Automática**: por glob (arquivos abertos) ou sempre (alwaysApply) |
| **Skill** | `SKILL.md` em `.cursor/skills/` | Ensina um fluxo passo a passo para uma tarefa específica. | **Por contexto**: quando a descrição bate com a tarefa, ou via @mention |
| **Agent doc** | `.md` em `docs/` | Descreve o papel do “agente” e mapeia tarefas → skills. | **Manual**: só via @mention no chat |

### Ordem de “força”

1. **Rules** — Sempre respeitadas quando aplicam ao contexto.
2. **Skills** — Usadas quando a tarefa pede (criar endpoint, etc.).
3. **Agent docs** — Referência para você saber qual skill usar e como pedir.

### Fluxo simplificado

```
Você pede algo
    → Cursor carrega Rules (por glob ou sempre)
    → Cursor verifica se alguma Skill se aplica (ou você @mention)
    → Cursor usa Agent doc se você @mencionar (para saber mapeamento tarefa→skill)
    → Cursor responde seguindo tudo isso
```

---

## 2. Rules — Regras persistentes

As rules ficam em `.cursor/rules/` e têm frontmatter YAML: `description`, `globs` (opcional) e `alwaysApply`.

### 2.1 `java-spring-stack.mdc` — Regra global (sempre ativa)

| Campo | Valor |
|-------|-------|
| `alwaysApply` | `true` |
| `globs` | — (não usa) |
| Escopo | Todas as conversas |

**O que define:**

- **Comportamento:** Nunca executar sem antes perguntar ao usuário.
- **Stack:** Java 21, Spring Boot 3, Maven, PostgreSQL, Flyway, JWT, Testcontainers.
- **Arquitetura:** Monólito modular, camadas controller → service → repository → domain.
- **Banco:** Migration Flyway sempre que mudar schema; nunca alterar migrations antigas.
- **API:** DTOs separados das entidades JPA; Swagger obrigatório.
- **Validação:** Bean Validation nos DTOs.
- **Segurança:** Spring Security, JWT, roles (ADMIN, SELLER, CUSTOMER).
- **Testes:** Unitários (JUnit + Mockito) e integração (Testcontainers).

**Quando vale:** Em qualquer interação no projeto.

---

### 2.2 `backend.mdc` — Regra específica de backend

| Campo | Valor |
|-------|-------|
| `alwaysApply` | `false` |
| `globs` | `backend/**` |
| Escopo | Arquivos dentro de `backend/` |

**O que define:**

- DTOs + validação
- Erros padronizados
- Flyway para schema
- Evitar N+1
- Transações em pedido/pagamento/estoque
- Testcontainers (MySQL) em fluxos críticos

**Quando vale:** Quando você tem aberto ou está editando algo em `backend/` (ex.: `backend/src/...`).

---

### 2.3 `frontend.mdc` — Regra específica de frontend

| Campo | Valor |
|-------|-------|
| `alwaysApply` | `false` |
| `globs` | `frontend/**` |
| Escopo | Arquivos dentro de `frontend/` |

**O que define:**

- Next App Router + TypeScript
- Estados loading/empty/error
- fetch wrapper
- Auth em cookie httpOnly (sem token em localStorage)
- ESLint/Prettier passando

**Quando vale:** Quando você está em `frontend/`.

---

### 2.4 `index.mdc` — Regra de fluxo e backlog

| Campo | Valor |
|-------|-------|
| `alwaysApply` | `true` |
| Escopo | Todas as conversas |

**O que define:**

- Seguir `@docs/ai/PROJECT_CONTEXT.md`, `DEFINITION_OF_DONE`, `CODE_REVIEW_CHECKLIST`, `BACKLOG`, `WORKFLOW`.
- Fluxo obrigatório: identificar item do backlog → plano curto (3–6 passos) → incrementos pequenos → migrations se schema → testes se endpoint → comandos de verificação ao final.
- Guardrails: máximo 1–3 arquivos por resposta, listar edge cases e testes antes de codar.

---

## 3. Skills — Conhecimento acionável

Skills são pastas em `.cursor/skills/` com arquivo `SKILL.md` e frontmatter `name` + `description`. O Cursor usa a `description` para decidir quando carregar a skill.

### 3.1 `spring-endpoint`

| Campo | Valor |
|-------|-------|
| `name` | `spring-endpoint` |
| `description` | Cria ou modifica endpoints REST em Java Spring Boot seguindo boas práticas do projeto. Use ao criar ou alterar endpoints REST, operações CRUD, controllers Spring ou novas rotas de API. |
| Caminho | `.cursor/skills/spring-endpoint/SKILL.md` |

**O que ensina — Fluxo em 9 passos:**

1. **Caso de uso** — Qual recurso e qual ação (ex.: criar produto, listar, atualizar estoque).
2. **Contrato da API** — Rota, método HTTP, DTOs request/response.
3. **Domínio** — Entidade JPA (Produto, Pedido, Cliente).
4. **Persistência** — Repository Spring Data JPA.
5. **Camadas** — Controller (recebe, valida, chama service) → Service (lógica de negócio) → Repository (acesso ao banco).
6. **Banco** — Nova migration Flyway (nunca alterar antigas).
7. **Validação** — Bean Validation nos DTOs.
8. **Segurança** — Autenticação/autorização com Spring Security + JWT.
9. **Testes** — Unitários (JUnit + Mockito) e integração (Testcontainers).

**Checklist rápido (skill):**

- Contrato da API definido
- Entidade JPA e Repository criados ou atualizados
- Migration Flyway (se schema mudou)
- Controller → Service → Repository implementados
- Bean Validation nos DTOs
- Segurança verificada (JWT, roles)
- Testes unitários e de integração

**Como acionar:** `@spring-endpoint` ou `@.cursor/skills/spring-endpoint/SKILL.md` no chat.

---

## 4. Agent docs — Referência e mapeamento

São documentos em `docs/` que descrevem o papel do “agente” e o mapeamento tarefa → skill. Só entram em contexto via @mention.

### 4.1 `docs/backend-agent.md`

**Objetivo:** Definir o papel do agente backend (Java/Spring) e o fluxo de decisão.

**Principais seções:**

- **Responsabilidades:** Entender o pedido, classificar tarefa, escolher skill, gerar código padronizado.
- **Tipos de tarefa:** endpoint REST, banco, testes, segurança, refatoração, arquitetura.
- **Mapeamento tarefa → skill:**
  - Criação de endpoint REST → `spring-endpoint`
  - Alteração de banco → fluxo Flyway (sem skill dedicada)
  - Demais → seguir padrões do projeto
- **Padrões de arquitetura:** Controller → Service → Repository → Database.
- **Boas práticas:** DTOs, Bean Validation, Flyway, testes, Swagger.

**Como acionar:** `@docs/backend-agent.md` no chat.

---

### 4.2 `docs/architecture.md`

**Objetivo:** Resumir a arquitetura do projeto.

**Conteúdo:**

- Monólito modular
- Spring Boot, PostgreSQL, Flyway, Testcontainers
- Estrutura: controller / service / repository / domain / dto

**Como acionar:** `@docs/architecture.md` no chat.

---

## 5. Fluxo de uso prático

### 5.1 Criar novo endpoint REST (ex.: POST /api/produtos)

```
1. @docs/backend-agent.md @spring-endpoint
2. "Crie o endpoint POST /api/produtos para cadastrar produto com nome e preço"
3. O agente segue:
   - java-spring-stack (sempre)
   - index.mdc (fluxo e guardrails)
   - backend.mdc (se estiver em backend/)
   - spring-endpoint (fluxo em 9 passos)
   - backend-agent (mapeamento e boas práticas)
```

### 5.2 Alterar schema do banco

```
1. @docs/backend-agent.md
2. "Preciso adicionar coluna stock na tabela produto"
3. O agente segue:
   - java-spring-stack (migrations Flyway)
   - index.mdc (flyway quando schema muda)
   - backend-agent (fluxo Flyway)
```

### 5.3 Implementar frontend

```
1. Abra um arquivo em frontend/
2. O Cursor já aplica frontend.mdc (Next, loading/empty/error, etc.)
3. java-spring-stack e index.mdc continuam ativas
```

### 5.4 Ver arquitetura geral

```
1. @docs/architecture.md
2. "Resuma a arquitetura do projeto"
```

---

## 6. Tabela de referência rápida

| Quero | O que usar | Observação |
|-------|------------|------------|
| Regras globais | — | Aplicadas sempre (java-spring-stack, index) |
| Regras backend | Abrir arquivo em `backend/` | backend.mdc entra automaticamente |
| Regras frontend | Abrir arquivo em `frontend/` | frontend.mdc entra automaticamente |
| Criar endpoint REST | `@docs/backend-agent.md` + `@spring-endpoint` | Skill guia o fluxo |
| Alterar banco | `@docs/backend-agent.md` | Fluxo Flyway |
| Ver mapeamento tarefa→skill | `@docs/backend-agent.md` | Seção "Mapeamento tarefa → skill" |
| Ver arquitetura | `@docs/architecture.md` | Visão resumida |

---

## 7. Estrutura de arquivos relevante

```
.cursor/
├── rules/
│   ├── index.mdc           # Sempre: fluxo backlog + guardrails
│   ├── java-spring-stack.mdc  # Sempre: stack + convenções backend
│   ├── backend.mdc         # backend/** — regras específicas
│   └── frontend.mdc        # frontend/** — regras específicas
└── skills/
    └── spring-endpoint/
        └── SKILL.md        # Fluxo de criação de endpoints REST

docs/
├── backend-agent.md        # Papel do agente backend + mapeamento
├── architecture.md         # Visão de arquitetura
├── AGENTS_AND_SKILLS.md    # Este documento
├── BACKLOG.md
├── WORKFLOW.md
└── ai/
    ├── PROJECT_CONTEXT.md
    ├── DEFINITION_OF_DONE.md
    ├── CODE_REVIEW_CHECKLIST.md
    └── BACKLOG_PROMPTS.md
```

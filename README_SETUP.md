# Setup do Cursor + IA (minimalista, funcional e “anti-muleta”)

Este repositório inclui um setup para usar o Cursor com IA de forma **profissional**, mas sem “fazer tudo por você”.
A ideia é acelerar **sem perder aprendizado**, forçando:
- planejamento curto
- incrementos pequenos
- testes e migrations
- validação e comunicação (PR)

---

## O que foi adicionado

### Regras do Cursor
- `.cursor/index.mdc` (regras globais — sempre aplicadas)
- `.cursor/rules/backend.mdc` (regras para `backend/**`)
- `.cursor/rules/frontend.mdc` (regras para `frontend/**`)
- `.cursorrules` (legacy — útil em algumas versões)

Essas regras impõem **guardrails**:
- sempre propor plano (3–6 passos)
- executar 1 passo por vez
- limitar número de arquivos por resposta
- listar edge cases + testes antes do código
- migrations Flyway obrigatórias ao tocar schema
- finalizar com comandos de verificação

### Documentação operacional
- `docs/START_HERE.md` — como começar em 3 comandos
- `docs/BACKLOG.md` — backlog com ordem sugerida
- `docs/WORKFLOW.md` — fluxo de branch/PR/validação
- `docs/RUN_CURSOR.md` — como executar no Cursor

### IA (prompts e checklists)
- `docs/ai/PROJECT_CONTEXT.md` — contexto/fonte de verdade
- `docs/ai/DEFINITION_OF_DONE.md` — DoD (quando está pronto)
- `docs/ai/CODE_REVIEW_CHECKLIST.md` — checklist de review
- `docs/ai/BACKLOG_PROMPTS.md` — prompts por item (use como referência, não como “piloto automático”)

### Templates e scripts
- `.github/PULL_REQUEST_TEMPLATE.md` — treina comunicação profissional (inclui decisões e escopo)
- `.github/ISSUE_TEMPLATE/backlog_item.md` — se quiser transformar backlog em issues
- `scripts/*.sh` — atalhos de execução (inclui `scripts/verify.sh`)

---

## Como instalar no seu projeto
1) Extraia o zip na raiz do seu repo `loja-revenda/`
2) Confirme que existem:
   - `.cursor/`
   - `docs/`
   - `scripts/`

> Se você já tinha arquivos com o mesmo nome, faça merge manual (para não sobrescrever sem querer).

---

## Como trabalhar (o jeito certo)
### Passo 1 — escolha um item do backlog
Abra `docs/BACKLOG.md` e pegue o próximo item (ex.: **B2-02**).

### Passo 2 — faça o DoR (5 linhas)
Antes de codar, preencha `docs/DEFINITION_OF_READY.md` (pode ser no PR ou numa issue).  
Isso evita “codar no escuro”.

### Passo 3 — peça para o Cursor ajudar (com guardrails)
Use `docs/ai/BACKLOG_PROMPTS.md` como base, mas peça **apenas o próximo incremento**.

Exemplo de pedido “bom”:
> “Implemente só o passo 1 do plano para B2-02: migrations + entidades + repository.  
> Liste edge cases e os testes que faltam. Não crie controller ainda.”

### Passo 4 — valide localmente
Use:
```bash
./scripts/verify.sh
```
Ou, separadamente:
```bash
./scripts/up.sh
./scripts/backend-test.sh
./scripts/frontend-lint.sh
```

### Passo 5 — PR pequeno e bem explicado
Abra PR e preencha o template:
- o que foi feito
- como testar
- **decisões/trade-offs**
- **escopo (o que ficou de fora)**

---

## Como NÃO usar (pra não travar o aprendizado)
- Não peça “faça tudo do B2-01 ao B2-03” de uma vez.
- Não aceite código que você não consegue explicar.
- Não pule testes/migrations “só pra andar”.
- Se o Cursor errar, use isso como aprendizado: ajuste e valide.

---

## Ordem recomendada (pra acelerar)
1) B0 (setup) + B1 (auth)
2) B2 (catálogo)
3) B3 (carrinho/pedido)
4) B4 (pagamento mock + estoque)
5) Front (F1)
6) Polimento (P1)

---

## Dicas para virar material de entrevista
Crie 5 ADRs curtas (em `docs/adr/`) durante o projeto:
- JWT em cookie httpOnly
- formato de erros (Problem Details)
- estratégia de transação em pedido/pagamento
- lock para estoque (otimista/pessimista)
- como evitar N+1 no catálogo

Cada ADR com 5–10 linhas já vira história boa para entrevista.

---

## Arquivos principais (atalhos)
- Backlog: `docs/BACKLOG.md`
- Fluxo: `docs/WORKFLOW.md`
- Rodar no Cursor: `docs/RUN_CURSOR.md`
- Regras IA: `docs/ai/PROJECT_CONTEXT.md`
- DoD: `docs/ai/DEFINITION_OF_DONE.md`
- Verify: `scripts/verify.sh`

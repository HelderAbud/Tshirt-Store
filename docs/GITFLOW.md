# GitFlow — Resumo (com explicações para júnior)

Este projeto usa **GitFlow** como modelo de branches. O fluxo é obrigatório.

---

## O que é GitFlow e por que usar?

**GitFlow** é um jeito combinado de organizar as branches do Git. Em vez de todo mundo commitar no mesmo lugar, cada tipo de mudança tem um tipo de branch e um destino certo.

**Por que isso ajuda?**
- **main** fica estável (só o que já foi “publicado”).
- **develop** concentra o que está em desenvolvimento.
- Features novas não quebram o código de quem está fazendo outra coisa.
- Fica claro de onde criar uma branch e para onde abrir o PR.

---

## Branches principais

| Branch | O que é | Em português |
|--------|--------|--------------|
| **main** | Código em produção. Só recebe merge de `release/*` ou `hotfix/*`. | O que está no ar para o usuário. Ninguém commita direto aqui. |
| **develop** | Integração. Próximo release. Recebe merge de `feature/*` e `release/*`. | O “cadinho” onde juntamos todas as features novas antes de virar release. |

**Dica:** Pense em `main` = “já saiu” e `develop` = “próxima versão em construção”.

---

## Branches de suporte

São branches temporárias. Você cria, trabalha, abre PR e depois a branch pode ser apagada.

| Padrão | De onde você cria | Para onde abre PR | Quando usar |
|--------|-------------------|--------------------|-------------|
| **feature/\*** | develop | develop | Nova funcionalidade (ex.: tela de produto, carrinho). |
| **release/\*** | develop | main **e** develop | Quando develop está pronta para virar versão (ex.: 1.0.0). |
| **hotfix/\*** | main | main **e** develop | Bug crítico em produção que não pode esperar o próximo release. |

**Resumindo:**
- **feature** = “estou desenvolvendo algo novo” → volta para `develop`.
- **release** = “vamos publicar essa versão” → vai para `main` e atualiza `develop`.
- **hotfix** = “algo quebrou em produção” → corrige em `main` e replica em `develop`.

---

## Fluxo resumido (diagrama)

```
main ─────●─────────────────────●──────────────●
           \                   / \            /
            \   release/1.0   /   \ hotfix/   /
             \               /     \         /
develop ──────●─────●───────●───────●───────●
               \   /         \
                \ /  feature/ \
                 ●             ●
```

Cada ● é um merge. As linhas mostram de onde vem o código (feature → develop, release → main, etc.).

---

## Passo a passo no dia a dia (júnior)

### Fazer uma feature nova

1. Atualize e crie a branch a partir de **develop**:
   ```bash
   git checkout develop
   git pull
   git checkout -b feature/B2-01-crud-produto
   ```
2. Desenvolva, commite na `feature/...`.
3. Abra um **Pull Request** da sua `feature/...` **para** `develop`.
4. CI precisa passar (build + testes). Depois alguém aprova e dá merge.
5. Após o merge, pode apagar a branch `feature/...` no remoto.

### Publicar uma versão (release)

1. Criar branch a partir de **develop**:
   ```bash
   git checkout develop
   git pull
   git checkout -b release/1.0.0
   ```
2. Ajustes finais (versão no pom, changelog, etc.).
3. Abrir **dois** PRs: um de `release/1.0.0` → **main**, outro → **develop**. Ou fazer merge em main e depois em develop, conforme combinado com o time.

### Corrigir bug urgente em produção (hotfix)

1. Criar branch a partir de **main**:
   ```bash
   git checkout main
   git pull
   git checkout -b hotfix/correcao-estoque
   ```
2. Fazer a correção, commitar.
3. Abrir PR de `hotfix/...` → **main**. Após merge em main, abrir PR de `hotfix/...` → **develop** (ou merge direto) para develop não ficar desatualizada.

---

## Regras práticas

1. **Nunca** commitar direto em `main` ou `develop` — sempre via PR.
2. **feature** → PR para `develop`.
3. **release** → PR para `main` e para `develop`.
4. **hotfix** → PR para `main` e para `develop`.
5. CI deve passar antes de merge (GitHub Actions).

---

## Nomenclatura sugerida

- `feature/B2-01-crud-produto` — ID do backlog + descrição curta.
- `release/1.0.0` — número da versão.
- `hotfix/correcao-estoque` — descrição curta do que foi corrigido.

---

## Erros comuns (evitar)

| Erro | Certo |
|------|--------|
| Criar `feature/...` a partir de `main` | Criar sempre a partir de `develop`. |
| Abrir PR de feature para `main` | Abrir PR de feature para `develop`. |
| Commitar direto em `develop` ou `main` | Sempre usar branch (feature/release/hotfix) e PR. |
| Esquecer de atualizar `develop` após hotfix | Fazer merge do hotfix em **main** e em **develop**. |

Se tiver dúvida em um caso específico, pergunte ao time ou consulte este doc antes de criar a branch ou abrir o PR.

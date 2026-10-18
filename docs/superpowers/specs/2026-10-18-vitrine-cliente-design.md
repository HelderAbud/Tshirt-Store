# Spec — Vitrine do cliente (F2-01)

**Data:** 2026-10-18  
**Status:** aprovada  
**Trilha:** Normal

## Problem

O cliente não tem tela para ver o catálogo, montar o carrinho e pagar o pedido mock. A API já faz isso. O painel admin em `frontend/index.html` é só da loja.

## Scope

Segunda página estática na mesma pasta e na mesma porta do painel:

- `frontend/store.html`
- `frontend/store.js`
- reuso de `frontend/styles.css`
- nota em `frontend/README.md` com a URL `http://localhost:3000/store.html`
- checkbox F2-01 em `docs/BACKLOG.md` só quando o fluxo for verificado

Telas, trocadas dentro de `store.html`:

1. **Catálogo** — `GET /api/catalog/products?page=&size=20`. Lista `content[]` com nome, SKU e preço. Se `totalPages` for maior que 1, botões anterior e próxima.
2. **Detalhe** — `GET /api/catalog/products/{id}`. Mostra variantes (cor, tamanho, SKU, preço, estoque). Quantidade mínima 1. Botão adicionar ao carrinho chama `PUT /api/cart/items` com `{ variantId, qty }`.
3. **Entrar / criar conta** — `POST /api/auth/login` e `POST /api/auth/register` com `{ email, password }`. Senha de cadastro com no mínimo 8 caracteres. Campos de exemplo: `customer@demo.local` e `Demo12345!`.
4. **Carrinho** — `GET /api/cart`. Alterar quantidade com `PUT /api/cart/items`. Remover com `DELETE /api/cart/items/{variantId}`.
5. **Pagar** — `POST /api/orders` e, em seguida, `POST /api/orders/{id}/pay` com header `Idempotency-Key` (UUID, no máximo 128 caracteres). A confirmação mostra id, status e total.

Um link de texto no topo da loja aponta para `index.html` (painel). O painel não ganha comportamento novo.

## Out of scope

- Endpoint, migration, CORS ou regra nova no backend
- Next.js e `npm install`
- Cookie httpOnly
- NF-e
- Histórico de pedidos na vitrine (`GET /api/orders` fica sem tela)
- Alterar `index.html` ou `app.js`
- Regra CSS nova que mude o layout do painel. Classe nova, se precisar, é usada só em `store.html`

## Acceptance criteria

- Sem token, o catálogo e o detalhe abrem.
- Sem token, adicionar ao carrinho, abrir o carrinho ou pagar leva à tela de entrar. Depois do login ou do cadastro, a loja volta para a tela de onde a pessoa saiu. O clique de adicionar não é repetido sozinho.
- O token do cliente fica em `localStorage` na chave `tshirt_customer_token`. A chave `tshirt_admin_token` não é lida nem apagada.
- Resposta 401 em carrinho, pedido ou pagamento limpa só o token do cliente e abre a tela de entrar.
- Login 401 sem corpo mostra o texto fixo "E-mail ou senha inválidos." Nos demais erros, a tela mostra `detail` do Problem Details.
- Enquanto o pagamento está em curso, o botão fica desabilitado e usa uma única `Idempotency-Key`.
- Catálogo vazio explica que o produto se cria no painel ou no Swagger.
- `index.html` e `app.js` do painel continuam no fluxo admin.

## Decisions

- Uma página (`store.html` + `store.js`), no mesmo padrão do painel.
- JWT no `localStorage`, chave separada da do admin.
- A API atual é o contrato. A listagem usa o `Page` do Spring (`content`, `totalPages`, `number`).
- Voltar depois do login restaura a rota (catálogo, detalhe com id, ou carrinho). Não reenvia o `PUT` do carrinho.

## Risks

- O CSS compartilhado pode alterar o visual do painel se uma regra nova for global. Ajuste novo fica em classes usadas só pela loja, ou o painel é conferido no mesmo smoke.
- Dois fluxos de baixa de estoque já existem (pagamento do pedido e venda de balcão). Esta fatia só chama o pagamento do pedido.
- Não há teste automatizado de browser neste repositório.

## Verification

Com a API em `http://localhost:8084` e `python -m http.server 3000` em `frontend/`:

1. Abrir `http://localhost:3000/store.html` sem token e ver o catálogo.
2. Entrar com `customer@demo.local` / `Demo12345!`.
3. Abrir um produto, adicionar uma variante, ver o carrinho, fechar o pedido e pagar.
4. Confirmar id, status e total na tela.
5. Abrir `http://localhost:3000/` e entrar como admin. O painel segue em estoque, venda e dashboard.
6. `mvn test` no backend permanece verde porque o backend não muda.

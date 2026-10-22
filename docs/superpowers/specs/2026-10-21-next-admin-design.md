# Spec — Painel Next.js (A1-04)

**Data:** 2026-10-21  
**Status:** aprovada  
**Trilha:** Normal

## Problem

O painel que funciona é o HTML em `frontend/index.html` e `frontend/app.js`. O Next em `frontend/src` já tem login, estoque, venda e dashboard, mas não instala, não tem a tela de NF-e e não é o jeito de abrir a loja.

## Scope

- A porta do front continua **3000**. O comando passa a ser `npm run dev` (`next dev -p 3000`).
- `next`, `react`, `react-dom`, `recharts`, `tailwindcss` e `autoprefixer` viram dependências de instalação, não opcionais.
- O painel Next fica igual ao HTML atual: login admin, estoque, venda, dashboard e NF-e.
- A vitrine continua em `http://localhost:3000/store.html`, servida pelo Next, sem reescrever o fluxo em React.
- `index.html` e `app.js` saem só depois da conferência do Next. Se `npm install` falhar, eles ficam e **A1-04** não é marcado.
- Checkbox **A1-04** em `docs/BACKLOG.md` só depois dessa conferência.

### Painel

- Login com `admin@demo.local` / `Demo12345!` preenchidos. Role diferente de `ADMIN` não entra.
- Token do admin: `localStorage`, chave `tshirt_admin_token`. Sem cookie httpOnly.
- Sem token, estoque, venda, dashboard e NF-e abrem o login.
- Navegação: Dashboard, Estoque, Venda, NF-e e Sair.
- Estoque: listar variantes, ajustar `delta` e motivo, mostrar o `detail` do erro.
- Venda: registrar um item com variante, quantidade e preço.
- Dashboard: receita dos últimos 14 dias.
- NF-e: escolher um XML e enviar `POST /api/admin/nfe/import` com `FormData` no campo `file`. O `Content-Type` não é forçado para JSON. Sucesso mostra `{accessKey} · {direction}`. Erro mostra o `detail`.

### Vitrine

- `store.html`, `store.js`, `store-logic.mjs` e `styles.css` ficam em `frontend/public/`.
- O link **Painel** da loja aponta para `/`, não para `index.html`.
- O restante da loja não muda: token `tshirt_customer_token`, catálogo, carrinho e pagamento mock.
- Os testes Node que leem esses arquivos passam a usar o caminho novo.

## Out of scope

- Reescrever a vitrine em React
- Cookie httpOnly (F1-03)
- Mudar a API, o CORS ou a porta 8084
- Emissão de NF-e, SEFAZ ou certificado
- Marcar F1-01, F1-02, F1-03 ou F1-04

## Acceptance criteria

- `npm install` no `frontend/` conclui e `npm run dev` abre o painel em `http://localhost:3000`.
- Login admin chega ao dashboard. Sem token, uma tela interna volta ao login.
- Ajuste de estoque, venda e dashboard usam a API que já existe.
- A tela NF-e envia o XML e mostra a chave e a direção, ou o `detail`.
- `http://localhost:3000/store.html` abre a loja. O link Painel cai no Next.
- A loja não envia `POST /api/admin/nfe/import` e não apaga `tshirt_admin_token`.
- Com a conferência feita, `index.html` e `app.js` não existem mais e **A1-04** está marcado.
- O README deixa de dizer que o painel sobe com `python -m http.server`.

## Decisions

- O esboço em `frontend/src` é a base. Não nasce um segundo app.
- A vitrine permanece HTML, servida como arquivo estático na mesma origem.
- O token do admin continua no `localStorage`.
- O HTML do painel só sai depois que o Next passa no mesmo fluxo.

## Risks

- `npm install` já falhou neste ambiente por rede/TLS. Sem ele, a fatia não avança.
- `index.html` dentro de `public/` esconderia a home do Next. Esse arquivo não entra em `public/`.
- O `python -m http.server` na porta 3000 impede o Next de subir.

## Verification

- `node --test` no `frontend/` continua passando, com os caminhos novos da vitrine e a checagem da tela NF-e no código do Next.
- Com a API em `http://localhost:8084`: login admin, ajuste de estoque, venda, dashboard e um XML de entrada na tela NF-e.
- Abrir `http://localhost:3000/store.html` e o link Painel.
- Se a API ou o `npm install` não estiverem disponíveis, **A1-04** fica aberto.

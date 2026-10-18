# Vitrine do cliente Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Abrir uma loja estática em `frontend/store.html` para catálogo, carrinho e pagamento mock, sem mudar a API nem o painel.

**Architecture:** Funções puras em `frontend/store-logic.mjs` (testadas com `node --test`) montam o HTML e as regras de token, erro, quantidade e rota de retorno. `frontend/store.js` só fala com a API e com o DOM. O painel continua em `index.html` e `app.js`.

**Tech Stack:** HTML, CSS já existente, JavaScript de browser (`type="module"`), Node.js `node:test` para a lógica pura. API Spring já em `http://localhost:8084`.

## Global Constraints

- Página em `frontend/store.html` + `frontend/store.js`, reuso de `frontend/styles.css`, porta 3000.
- Catálogo: `GET /api/catalog/products?page=&size=20`, campos `content`, `totalPages`, `number`.
- Detalhe: `GET /api/catalog/products/{id}` com variantes (cor, tamanho, SKU, preço, estoque).
- Auth: `POST /api/auth/login` e `POST /api/auth/register` com `{ email, password }`. Senha de cadastro com no mínimo 8 caracteres. Exemplo `customer@demo.local` / `Demo12345!`.
- Carrinho: `GET /api/cart`, `PUT /api/cart/items` com `{ variantId, qty }`, `DELETE /api/cart/items/{variantId}`.
- Pagar: `POST /api/orders` e `POST /api/orders/{id}/pay` com header `Idempotency-Key` (UUID, no máximo 128 caracteres). Confirmação mostra id, status e total.
- Token do cliente: `localStorage` chave `tshirt_customer_token`. Não ler nem apagar `tshirt_admin_token`.
- Sem token, catálogo e detalhe abrem. Adicionar, carrinho ou pagar levam ao login e, depois, voltam à rota salva. O `PUT` não é reenviado sozinho.
- Login 401 sem corpo: texto `E-mail ou senha inválidos.` Demais erros: `detail` do Problem Details.
- 401 em carrinho, pedido ou pagamento limpa só o token do cliente e abre o login.
- Botão de pagar desabilitado durante o envio, com uma única chave.
- Catálogo vazio: `Nenhum produto no catálogo. Crie um produto no painel ou no Swagger.`
- Não alterar `index.html`, `app.js`, backend, CORS, Next.js. Classe CSS nova só se a loja precisar, sem mudar o layout do painel.
- `docs/BACKLOG.md` marca F2-01 só depois do smoke manual.
- Commit, push e PR só se o usuário pedir nesta sessão. Os comandos abaixo ficam prontos para esse momento.

**Spec:** `docs/superpowers/specs/2026-10-18-vitrine-cliente-design.md`

---

### Task 1: Regras puras da loja

**Files:**
- Create: `frontend/store-logic.mjs`
- Test: `frontend/store-logic.test.mjs`

**Interfaces:**
- Consumes: nada
- Produces:
  - `CUSTOMER_TOKEN_KEY` = `"tshirt_customer_token"`
  - `ADMIN_TOKEN_KEY` = `"tshirt_admin_token"`
  - `EMPTY_CATALOG_TEXT`, `LOGIN_401_TEXT`
  - `escapeHtml(value) -> string`
  - `loginErrorMessage(status, body) -> string`
  - `apiErrorMessage(status, body) -> string`
  - `clampQty(value) -> number`
  - `createIdempotencyKey(randomUUID) -> string`
  - `saveReturnRoute(route) -> { name, productId }`
  - `pagerState(page) -> { page, totalPages, hasPrev, hasNext }`
  - `catalogItemsHtml(products) -> string`
  - `variantRowsHtml(variants) -> string`
  - `cartRowsHtml(items) -> string`
  - `orderConfirmHtml(order) -> string`

- [ ] **Step 1: Write the failing test**

Create `frontend/store-logic.test.mjs`:

```javascript
import assert from "node:assert/strict";
import test from "node:test";
import {
  ADMIN_TOKEN_KEY,
  CUSTOMER_TOKEN_KEY,
  EMPTY_CATALOG_TEXT,
  LOGIN_401_TEXT,
  apiErrorMessage,
  cartRowsHtml,
  catalogItemsHtml,
  clampQty,
  createIdempotencyKey,
  escapeHtml,
  loginErrorMessage,
  orderConfirmHtml,
  pagerState,
  saveReturnRoute,
  variantRowsHtml,
} from "./store-logic.mjs";

test("customer token key is separate from the admin key", () => {
  assert.equal(CUSTOMER_TOKEN_KEY, "tshirt_customer_token");
  assert.equal(ADMIN_TOKEN_KEY, "tshirt_admin_token");
  assert.notEqual(CUSTOMER_TOKEN_KEY, ADMIN_TOKEN_KEY);
});

test("login 401 without a body uses the fixed sentence", () => {
  assert.equal(loginErrorMessage(401, null), LOGIN_401_TEXT);
  assert.equal(LOGIN_401_TEXT, "E-mail ou senha inválidos.");
});

test("other errors use problem detail", () => {
  assert.equal(loginErrorMessage(409, { detail: "Email already registered" }), "Email already registered");
  assert.equal(apiErrorMessage(400, { detail: "Validation failed" }), "Validation failed");
  assert.equal(apiErrorMessage(401, null), "HTTP 401");
});

test("quantity below 1 becomes 1", () => {
  assert.equal(clampQty(0), 1);
  assert.equal(clampQty(-3), 1);
  assert.equal(clampQty("2"), 2);
  assert.equal(clampQty("nope"), 1);
});

test("idempotency key stays within 128 characters and is not reused by the factory", () => {
  const key = createIdempotencyKey(() => "abc-123");
  assert.equal(key, "abc-123");
  assert.ok(key.length <= 128);
  assert.throws(() => createIdempotencyKey(() => ""), /Idempotency-Key invalid/);
  assert.throws(() => createIdempotencyKey(() => "x".repeat(129)), /Idempotency-Key invalid/);
});

test("return route restores catalog, detail or cart and does not keep a stale product id", () => {
  assert.deepEqual(saveReturnRoute({ name: "detail", productId: 7 }), { name: "detail", productId: 7 });
  assert.deepEqual(saveReturnRoute({ name: "cart", productId: 7 }), { name: "cart", productId: null });
  assert.deepEqual(saveReturnRoute({ name: "pay" }), { name: "catalog", productId: null });
});

test("empty catalog explains how to create a product", () => {
  const html = catalogItemsHtml([]);
  assert.match(html, new RegExp(EMPTY_CATALOG_TEXT));
});

test("catalog, variants, cart and order escape names", () => {
  assert.equal(escapeHtml(`<b>"&`), "&lt;b&gt;&quot;&amp;");
  const catalog = catalogItemsHtml([{ id: 1, name: "<Camisa>", sku: "A&B", price: 10 }]);
  assert.match(catalog, /&lt;Camisa&gt;/);
  assert.match(catalog, /data-open="1"/);
  const variants = variantRowsHtml([{ id: 4, color: "<azul>", size: "M", sku: "S1", price: 10, stockQty: 2 }]);
  assert.match(variants, /&lt;azul&gt;/);
  assert.match(variants, /data-add="4"/);
  const cart = cartRowsHtml([{ variantId: 4, productName: "<Polo>", color: "azul", size: "M", price: 10, qty: 2 }]);
  assert.match(cart, /&lt;Polo&gt;/);
  assert.match(cart, /data-qty="4"/);
  assert.match(cart, /data-remove="4"/);
  const order = orderConfirmHtml({ id: 9, status: "PAID", total: 20 });
  assert.match(order, /id="order-id"/);
  assert.match(order, /Pedido 9/);
  assert.match(order, /id="order-status"/);
  assert.match(order, /PAID/);
  assert.match(order, /id="order-total"/);
});

test("pager exposes previous and next pages", () => {
  assert.deepEqual(pagerState({ number: 0, totalPages: 1 }), {
    page: 0,
    totalPages: 1,
    hasPrev: false,
    hasNext: false,
  });
  assert.deepEqual(pagerState({ number: 1, totalPages: 3 }), {
    page: 1,
    totalPages: 3,
    hasPrev: true,
    hasNext: true,
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run from the repo root: `node --test frontend/store-logic.test.mjs`

Expected: FAIL because `frontend/store-logic.mjs` does not exist.

- [ ] **Step 3: Write minimal implementation**

Create `frontend/store-logic.mjs`:

```javascript
export const CUSTOMER_TOKEN_KEY = "tshirt_customer_token";
export const ADMIN_TOKEN_KEY = "tshirt_admin_token";
export const EMPTY_CATALOG_TEXT =
  "Nenhum produto no catálogo. Crie um produto no painel ou no Swagger.";
export const LOGIN_401_TEXT = "E-mail ou senha inválidos.";

export function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

export function loginErrorMessage(status, body) {
  if (status === 401 && (body == null || body.detail == null || body.detail === "")) {
    return LOGIN_401_TEXT;
  }
  if (body && body.detail) return String(body.detail);
  return `HTTP ${status}`;
}

export function apiErrorMessage(status, body) {
  if (body && body.detail) return String(body.detail);
  return `HTTP ${status}`;
}

export function clampQty(value) {
  const n = Number(value);
  if (!Number.isInteger(n) || n < 1) return 1;
  return n;
}

export function createIdempotencyKey(randomUUID) {
  const key = randomUUID();
  if (typeof key !== "string" || key.length < 1 || key.length > 128) {
    throw new Error("Idempotency-Key invalid");
  }
  return key;
}

export function saveReturnRoute(route) {
  const name = route && route.name;
  if (name !== "catalog" && name !== "detail" && name !== "cart") {
    return { name: "catalog", productId: null };
  }
  if (name === "detail") {
    return { name: "detail", productId: Number(route.productId) };
  }
  return { name, productId: null };
}

export function pagerState(page) {
  const number = Number(page && page.number) || 0;
  const totalPages = Number(page && page.totalPages) || 0;
  return {
    page: number,
    totalPages,
    hasPrev: number > 0,
    hasNext: totalPages > number + 1,
  };
}

export function catalogItemsHtml(products) {
  const items = Array.isArray(products) ? products : [];
  if (items.length === 0) {
    return `<p class="muted" id="empty-catalog">${escapeHtml(EMPTY_CATALOG_TEXT)}</p>`;
  }
  return items
    .map(
      (product) => `<article class="panel" data-product-id="${escapeHtml(product.id)}">
        <h2>${escapeHtml(product.name)}</h2>
        <p class="muted">${escapeHtml(product.sku)}</p>
        <p>${escapeHtml(product.price)}</p>
        <button type="button" data-open="${escapeHtml(product.id)}">Ver</button>
      </article>`,
    )
    .join("");
}

export function variantRowsHtml(variants) {
  const items = Array.isArray(variants) ? variants : [];
  if (items.length === 0) {
    return `<tr><td colspan="6" class="muted">Sem variantes.</td></tr>`;
  }
  return items
    .map(
      (variant) => `<tr>
        <td>${escapeHtml(variant.color)}</td>
        <td>${escapeHtml(variant.size)}</td>
        <td>${escapeHtml(variant.sku)}</td>
        <td>${escapeHtml(variant.price)}</td>
        <td>${escapeHtml(variant.stockQty)}</td>
        <td><button type="button" data-add="${escapeHtml(variant.id)}">Adicionar</button></td>
      </tr>`,
    )
    .join("");
}

export function cartRowsHtml(items) {
  const rows = Array.isArray(items) ? items : [];
  if (rows.length === 0) {
    return `<tr><td colspan="5" class="muted">Carrinho vazio.</td></tr>`;
  }
  return rows
    .map(
      (item) => `<tr>
        <td>${escapeHtml(item.productName)}</td>
        <td>${escapeHtml(item.color)} / ${escapeHtml(item.size)}</td>
        <td>${escapeHtml(item.price)}</td>
        <td><input data-qty="${escapeHtml(item.variantId)}" type="number" min="1" value="${escapeHtml(item.qty)}" /></td>
        <td>
          <button type="button" data-update="${escapeHtml(item.variantId)}">Atualizar</button>
          <button type="button" data-remove="${escapeHtml(item.variantId)}">Remover</button>
        </td>
      </tr>`,
    )
    .join("");
}

export function orderConfirmHtml(order) {
  return `<p id="order-id">Pedido ${escapeHtml(order.id)}</p>
    <p id="order-status">${escapeHtml(order.status)}</p>
    <p id="order-total">${escapeHtml(order.total)}</p>`;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `node --test frontend/store-logic.test.mjs`

Expected: PASS, 8 tests.

- [ ] **Step 5: Commit**

Só se o usuário pedir commit nesta sessão:

```bash
git checkout main
git pull
git checkout -b feature/storefront-customer
git add frontend/store-logic.mjs frontend/store-logic.test.mjs
git commit -m "test(storefront): cover customer store rules"
```

A branch nasce de `main` porque este repositório não tem `develop`.

---

### Task 2: Página da loja

**Files:**
- Create: `frontend/store.html`
- Create: `frontend/store.js`
- Test: `frontend/store-page.test.mjs`
- Modify: `frontend/README.md`

**Interfaces:**
- Consumes: todas as exportações da Task 1
- Produces: `http://localhost:3000/store.html` com as rotas internas `catalog`, `detail`, `login` e `cart`. A confirmação do pagamento aparece no carrinho, nos elementos `order-id`, `order-status` e `order-total`.

- [ ] **Step 1: Write the failing test**

Create `frontend/store-page.test.mjs`:

```javascript
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

test("store page is a module and does not boot the admin app", () => {
  const html = readFileSync(new URL("./store.html", import.meta.url), "utf8");
  assert.match(html, /styles\.css/);
  assert.match(html, /type="module"/);
  assert.match(html, /store\.js/);
  assert.doesNotMatch(html, /app\.js/);
});

test("store script keeps the customer token helper and the pay header", () => {
  const js = readFileSync(new URL("./store.js", import.meta.url), "utf8");
  assert.match(js, /CUSTOMER_TOKEN_KEY/);
  assert.match(js, /loginErrorMessage/);
  assert.match(js, /createIdempotencyKey/);
  assert.match(js, /saveReturnRoute/);
  assert.match(js, /Idempotency-Key/);
  assert.doesNotMatch(js, /tshirt_admin_token/);
  assert.doesNotMatch(js, /localStorage\.removeItem\("tshirt_admin_token"\)/);
});

test("frontend readme documents the store url", () => {
  const readme = readFileSync(new URL("./README.md", import.meta.url), "utf8");
  assert.match(readme, /http:\/\/localhost:3000\/store\.html/);
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node --test frontend/store-page.test.mjs`

Expected: FAIL because `store.html` does not exist.

- [ ] **Step 3: Write minimal implementation**

Create `frontend/store.html`:

```html
<!DOCTYPE html>
<html lang="pt-BR">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>Tshirt Store — Loja</title>
    <link rel="stylesheet" href="./styles.css" />
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="./store.js"></script>
  </body>
</html>
```

Create `frontend/store.js`:

```javascript
import {
  CUSTOMER_TOKEN_KEY,
  apiErrorMessage,
  cartRowsHtml,
  catalogItemsHtml,
  clampQty,
  createIdempotencyKey,
  escapeHtml,
  loginErrorMessage,
  orderConfirmHtml,
  pagerState,
  saveReturnRoute,
  variantRowsHtml,
} from "./store-logic.mjs";

const API_URL = localStorage.getItem("tshirt_api_url") || "http://localhost:8084";
const app = document.getElementById("app");
let returnRoute = { name: "catalog", productId: null };
let paying = false;

function getToken() {
  return localStorage.getItem(CUSTOMER_TOKEN_KEY);
}

function setToken(token) {
  localStorage.setItem(CUSTOMER_TOKEN_KEY, token);
}

function clearToken() {
  localStorage.removeItem(CUSTOMER_TOKEN_KEY);
}

function requireCustomer(route) {
  if (getToken()) return true;
  returnRoute = saveReturnRoute(route);
  routeTo("login");
  return false;
}

async function api(path, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (options.body && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  const res = await fetch(`${API_URL}${path}`, { ...options, headers });
  if (res.status === 401 && !path.startsWith("/api/auth/")) {
    clearToken();
    routeTo("login");
    const error = new Error(apiErrorMessage(401, null));
    error.redirected = true;
    throw error;
  }
  if (!res.ok) {
    let body = null;
    try {
      body = await res.json();
    } catch (_) {
      body = null;
    }
    const message = path.startsWith("/api/auth/")
      ? loginErrorMessage(res.status, body)
      : apiErrorMessage(res.status, body);
    throw new Error(message);
  }
  if (res.status === 204) return null;
  return res.json();
}

function shell(active, bodyHtml) {
  return `
    <header>
      <p class="eyebrow">Tshirt Store</p>
      <h1>Loja</h1>
      <p class="muted"><a href="./index.html">Painel</a></p>
    </header>
    <nav class="row">
      <button type="button" data-go="catalog" class="${active === "catalog" ? "nav-btn active" : "nav-btn"}">Catálogo</button>
      <button type="button" data-go="cart" class="${active === "cart" ? "nav-btn active" : "nav-btn"}">Carrinho</button>
      <button type="button" class="ghost" id="logout">Sair</button>
    </nav>
    <div>${bodyHtml}</div>
  `;
}

function showError(err) {
  if (err && err.redirected) return;
  const msg = document.getElementById("msg");
  if (!msg) return;
  msg.hidden = false;
  msg.textContent = err.message;
}

function bindChrome() {
  document.querySelectorAll("[data-go]").forEach((button) => {
    button.addEventListener("click", () => routeTo(button.getAttribute("data-go")));
  });
  const logout = document.getElementById("logout");
  if (logout) {
    logout.addEventListener("click", () => {
      clearToken();
      returnRoute = { name: "catalog", productId: null };
      routeTo("catalog");
    });
  }
}

function routeTo(name, productId) {
  if (name === "login") return renderLogin();
  if (name === "detail") return renderDetail(productId);
  if (name === "cart") {
    if (!requireCustomer({ name: "cart" })) return;
    return renderCart();
  }
  return renderCatalog(0);
}

async function renderCatalog(pageNumber) {
  let page = { content: [], number: pageNumber, totalPages: 0 };
  let error = "";
  try {
    page = await api(`/api/catalog/products?page=${pageNumber}&size=20`);
  } catch (err) {
    error = err.message;
  }
  const pager = pagerState(page);
  app.innerHTML = shell(
    "catalog",
    `
    <section class="panel">
      <h2>Catálogo</h2>
      <p class="err" id="msg" ${error ? "" : "hidden"}>${escapeHtml(error)}</p>
      ${catalogItemsHtml(page.content)}
      <div class="row">
        <button type="button" id="prev" ${pager.hasPrev ? "" : "disabled"}>Anterior</button>
        <button type="button" id="next" ${pager.hasNext ? "" : "disabled"}>Próxima</button>
      </div>
    </section>
  `,
  );
  bindChrome();
  document.querySelectorAll("[data-open]").forEach((button) => {
    button.addEventListener("click", () => routeTo("detail", Number(button.getAttribute("data-open"))));
  });
  document.getElementById("prev").addEventListener("click", () => renderCatalog(pager.page - 1));
  document.getElementById("next").addEventListener("click", () => renderCatalog(pager.page + 1));
}

async function renderDetail(productId) {
  let product = null;
  let error = "";
  try {
    product = await api(`/api/catalog/products/${productId}`);
  } catch (err) {
    error = err.message;
  }
  const body = product
    ? `
      <h2>${escapeHtml(product.name)}</h2>
      <p class="muted">${escapeHtml(product.sku)}</p>
      <label>Quantidade <input id="qty" type="number" min="1" value="1" /></label>
      <table>
        <thead><tr><th>Cor</th><th>Tam.</th><th>SKU</th><th>Preço</th><th>Estoque</th><th></th></tr></thead>
        <tbody>${variantRowsHtml(product.variants)}</tbody>
      </table>`
    : "";
  app.innerHTML = shell(
    "catalog",
    `<section class="panel">${body}<p class="err" id="msg" ${error ? "" : "hidden"}>${escapeHtml(error)}</p></section>`,
  );
  bindChrome();
  document.querySelectorAll("[data-add]").forEach((button) => {
    button.addEventListener("click", () => addVariant(productId, Number(button.getAttribute("data-add"))));
  });
}

async function addVariant(productId, variantId) {
  if (!requireCustomer({ name: "detail", productId })) return;
  const qty = clampQty(document.getElementById("qty").value);
  try {
    await api("/api/cart/items", {
      method: "PUT",
      body: JSON.stringify({ variantId, qty }),
    });
    routeTo("cart");
  } catch (err) {
    showError(err);
  }
}

function renderLogin() {
  app.innerHTML = `
    <section class="panel" style="max-width:420px;margin:4rem auto;">
      <p class="eyebrow">Tshirt Store</p>
      <h1>Entrar</h1>
      <p class="muted"><a href="./index.html">Painel</a></p>
      <form id="login-form" style="display:grid;gap:0.75rem;margin-top:1rem;">
        <input name="email" type="email" value="customer@demo.local" required />
        <input name="password" type="password" value="Demo12345!" required />
        <button type="submit">Entrar</button>
      </form>
      <form id="register-form" style="display:grid;gap:0.75rem;margin-top:1rem;">
        <h2>Criar conta</h2>
        <input name="email" type="email" required />
        <input name="password" type="password" minlength="8" required />
        <button type="submit">Cadastrar</button>
      </form>
      <button type="button" class="ghost" id="back-catalog">Voltar ao catálogo</button>
      <p class="err" id="msg" hidden></p>
    </section>
  `;
  document.getElementById("back-catalog").addEventListener("click", () => {
    returnRoute = { name: "catalog", productId: null };
    routeTo("catalog");
  });
  document.getElementById("login-form").addEventListener("submit", (event) => submitAuth(event, "/api/auth/login"));
  document.getElementById("register-form").addEventListener("submit", (event) => submitAuth(event, "/api/auth/register"));
}

async function submitAuth(event, path) {
  event.preventDefault();
  const data = new FormData(event.target);
  const msg = document.getElementById("msg");
  msg.hidden = true;
  try {
    const session = await api(path, {
      method: "POST",
      body: JSON.stringify({
        email: data.get("email"),
        password: data.get("password"),
      }),
    });
    setToken(session.token);
    const next = returnRoute;
    returnRoute = { name: "catalog", productId: null };
    routeTo(next.name, next.productId);
  } catch (err) {
    msg.hidden = false;
    msg.textContent = err.message;
  }
}

async function renderCart() {
  let cart = { items: [] };
  let error = "";
  try {
    cart = await api("/api/cart");
  } catch (err) {
    if (err.redirected) return;
    error = err.message;
  }
  app.innerHTML = shell(
    "cart",
    `
    <section class="panel">
      <h2>Carrinho</h2>
      <p class="err" id="msg" ${error ? "" : "hidden"}>${escapeHtml(error)}</p>
      <table>
        <thead><tr><th>Produto</th><th>Variante</th><th>Preço</th><th>Qtd</th><th></th></tr></thead>
        <tbody>${cartRowsHtml(cart.items)}</tbody>
      </table>
      <button type="button" id="pay" ${paying ? "disabled" : ""}>Pagar</button>
      <div id="confirm"></div>
    </section>
  `,
  );
  bindChrome();
  document.querySelectorAll("[data-update]").forEach((button) => {
    button.addEventListener("click", () => updateQty(Number(button.getAttribute("data-update"))));
  });
  document.querySelectorAll("[data-remove]").forEach((button) => {
    button.addEventListener("click", () => removeItem(Number(button.getAttribute("data-remove"))));
  });
  document.getElementById("pay").addEventListener("click", pay);
}

async function updateQty(variantId) {
  const input = document.querySelector(`[data-qty="${variantId}"]`);
  try {
    await api("/api/cart/items", {
      method: "PUT",
      body: JSON.stringify({ variantId, qty: clampQty(input.value) }),
    });
    routeTo("cart");
  } catch (err) {
    showError(err);
  }
}

async function removeItem(variantId) {
  try {
    await api(`/api/cart/items/${variantId}`, { method: "DELETE" });
    routeTo("cart");
  } catch (err) {
    showError(err);
  }
}

async function pay() {
  if (paying) return;
  if (!requireCustomer({ name: "cart" })) return;
  paying = true;
  const button = document.getElementById("pay");
  button.disabled = true;
  const idempotencyKey = createIdempotencyKey(() => crypto.randomUUID());
  try {
    const order = await api("/api/orders", { method: "POST" });
    const paid = await api(`/api/orders/${order.id}/pay`, {
      method: "POST",
      headers: { "Idempotency-Key": idempotencyKey },
    });
    document.getElementById("confirm").innerHTML = orderConfirmHtml(paid);
  } catch (err) {
    showError(err);
  } finally {
    paying = false;
    const current = document.getElementById("pay");
    if (current) current.disabled = false;
  }
}

routeTo("catalog");
```

Add this paragraph at the end of `frontend/README.md`:

```markdown
## Vitrine do cliente

Com a API em `http://localhost:8084` e o servidor estático desta pasta na porta 3000, abra http://localhost:3000/store.html

O painel admin continua em http://localhost:3000/
```

- [ ] **Step 4: Run test to verify it passes**

Run: `node --test frontend/store-logic.test.mjs frontend/store-page.test.mjs`

Expected: PASS. `index.html` and `app.js` stay unmodified (`git diff -- index.html frontend/app.js` prints nothing).

- [ ] **Step 5: Commit**

Só se o usuário pedir commit nesta sessão:

```bash
git add frontend/store.html frontend/store.js frontend/store-page.test.mjs frontend/README.md
git commit -m "feat(storefront): add customer catalog cart and mock checkout"
```

---

### Task 3: Smoke e backlog

**Files:**
- Modify: `docs/BACKLOG.md` (somente o checkbox F2-01, e só depois do smoke passar)
- Modify: nenhum arquivo de backend

**Interfaces:**
- Consumes: a página da Task 2
- Produces: F2-01 marcado se, e somente se, o smoke abaixo passar

- [ ] **Step 1: Confirm backend tests still pass**

Run: `mvn test` in `backend/`

Expected: BUILD SUCCESS. Nenhum arquivo em `backend/` entra no diff.

- [ ] **Step 2: Manual store smoke**

API em `http://localhost:8084`. Em `frontend/`: `python -m http.server 3000`.

1. Abrir http://localhost:3000/store.html numa janela anônima. O catálogo aparece sem login. Se não houver produto, a frase exata é `Nenhum produto no catálogo. Crie um produto no painel ou no Swagger.`
2. Abrir um produto. Clicar em Adicionar sem token. A tela de entrar abre. O carrinho não recebe o item ainda.
3. Entrar com `customer@demo.local` / `Demo12345!`. A loja volta ao detalhe desse produto.
4. Adicionar a variante, abrir o carrinho, clicar em Pagar uma vez. A confirmação mostra `Pedido`, o status e o total. O botão fica desabilitado enquanto a chamada corre.
5. Abrir http://localhost:3000/ e entrar como `admin@demo.local` / `Demo12345!`. Estoque, venda e dashboard seguem abrindo.

- [ ] **Step 3: Mark the backlog item**

Em `docs/BACKLOG.md`, trocar a linha F2-01 para:

```markdown
- [x] **F2-01** UI catálogo/carrinho/checkout sobre `/api/catalog` e `/api/cart`
```

Se o smoke não passou, deixar `[ ]` e registrar o que falhou. Não marcar o item no escuro.

- [ ] **Step 4: Commit**

Só se o usuário pedir commit nesta sessão, e só com o smoke verde:

```bash
git add docs/BACKLOG.md
git commit -m "docs(backlog): mark customer storefront done"
```

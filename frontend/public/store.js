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
      <p class="muted"><a href="/">Painel</a></p>
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
      <p class="muted"><a href="/">Painel</a></p>
      <form id="login-form" style="display:grid;gap:0.75rem;margin-top:1rem;">
        <input name="email" type="email" required />
        <input name="password" type="password" required />
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

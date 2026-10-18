const API_URL = localStorage.getItem("tshirt_api_url") || "http://localhost:8084";
const TOKEN_KEY = "tshirt_admin_token";

const app = document.getElementById("app");

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}

function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

async function api(path, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (options.body && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  const res = await fetch(`${API_URL}${path}`, { ...options, headers });
  if (!res.ok) {
    let detail = `HTTP ${res.status}`;
    try {
      const body = await res.json();
      detail = body.detail || body.error || detail;
    } catch (_) {}
    throw new Error(detail);
  }
  if (res.status === 204) return null;
  return res.json();
}

function isoDate(d) {
  return d.toISOString().slice(0, 10);
}

function shell(active, bodyHtml) {
  return `
    <header>
      <p class="eyebrow">Tshirt Store</p>
      <h1>Backoffice</h1>
      <p class="muted">Estoque · venda de balcão · dashboard</p>
    </header>
    <nav class="row">
      <button class="nav-btn ${active === "dashboard" ? "active" : ""}" data-go="dashboard">Dashboard</button>
      <button class="nav-btn ${active === "inventory" ? "active" : ""}" data-go="inventory">Estoque</button>
      <button class="nav-btn ${active === "sales" ? "active" : ""}" data-go="sales">Venda</button>
      <button class="ghost" id="logout">Sair</button>
    </nav>
    <section class="panel">${bodyHtml}</section>
  `;
}

function bindNav() {
  document.querySelectorAll("[data-go]").forEach((btn) => {
    btn.addEventListener("click", () => route(btn.getAttribute("data-go")));
  });
  const logout = document.getElementById("logout");
  if (logout) {
    logout.addEventListener("click", () => {
      clearToken();
      route("login");
    });
  }
}

async function renderLogin() {
  app.innerHTML = `
    <section class="panel" style="max-width:420px;margin:4rem auto;">
      <p class="eyebrow">Tshirt Store</p>
      <h1>Login admin</h1>
      <p class="muted">API: ${API_URL}</p>
      <form id="login-form" style="display:grid;gap:0.75rem;margin-top:1rem;">
        <input name="email" type="email" value="admin@demo.local" required />
        <input name="password" type="password" value="Demo12345!" required />
        <button type="submit">Entrar</button>
      </form>
      <p class="err" id="msg" hidden></p>
    </section>
  `;
  document.getElementById("login-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const msg = document.getElementById("msg");
    msg.hidden = true;
    try {
      const data = await api("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({
          email: fd.get("email"),
          password: fd.get("password"),
        }),
      });
      if (data.role !== "ADMIN") throw new Error("Backoffice exige role ADMIN");
      setToken(data.token);
      route("dashboard");
    } catch (err) {
      msg.textContent = err.message;
      msg.hidden = false;
    }
  });
}

async function renderInventory() {
  let stock = [];
  let error = "";
  try {
    stock = await api("/api/admin/inventory/stock");
  } catch (err) {
    error = err.message;
  }

  const options = stock
    .map(
      (s) =>
        `<option value="${s.variantId}">${s.sku} (${s.color}/${s.size}) — ${s.stockQty}</option>`,
    )
    .join("");

  const rows = stock.length
    ? stock
        .map(
          (s) =>
            `<tr><td>${s.sku}</td><td>${s.color}</td><td>${s.size}</td><td>${s.stockQty}</td></tr>`,
        )
        .join("")
    : `<tr><td colspan="4" class="muted">Sem variantes. Crie via Swagger/API.</td></tr>`;

  app.innerHTML = shell(
    "inventory",
    `
    <h2>Estoque</h2>
    <p class="muted">Ajuste por variante com motivo</p>
    <form id="adjust-form" class="row" style="margin-top:1rem;">
      <select name="variantId" required><option value="">Variante</option>${options}</select>
      <input name="delta" type="number" value="1" required />
      <input name="reason" value="acerto de estoque" required />
      <button type="submit">Ajustar</button>
    </form>
    <p class="err" id="msg" ${error ? "" : "hidden"}>${error}</p>
    <p class="ok" id="ok" hidden></p>
    <table>
      <thead><tr><th>SKU</th><th>Cor</th><th>Tam.</th><th>Qtd</th></tr></thead>
      <tbody>${rows}</tbody>
    </table>
  `,
  );
  bindNav();
  document.getElementById("adjust-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const msg = document.getElementById("msg");
    const ok = document.getElementById("ok");
    msg.hidden = true;
    ok.hidden = true;
    try {
      const res = await api("/api/admin/inventory/adjust", {
        method: "POST",
        body: JSON.stringify({
          variantId: Number(fd.get("variantId")),
          delta: Number(fd.get("delta")),
          reason: fd.get("reason"),
        }),
      });
      ok.textContent = `OK — ${res.sku} agora com ${res.stockQty}`;
      ok.hidden = false;
      route("inventory");
    } catch (err) {
      msg.textContent = err.message;
      msg.hidden = false;
    }
  });
}

async function renderSales() {
  let stock = [];
  let error = "";
  try {
    stock = await api("/api/admin/inventory/stock");
  } catch (err) {
    error = err.message;
  }
  const options = stock
    .map(
      (s) =>
        `<option value="${s.variantId}" data-price="${s.price}">${s.sku} — estoque ${s.stockQty}</option>`,
    )
    .join("");

  app.innerHTML = shell(
    "sales",
    `
    <h2>Venda de balcão</h2>
    <p class="muted">Registra venda e baixa estoque</p>
    <form id="sale-form" class="row" style="margin-top:1rem;">
      <select name="variantId" id="variant" required><option value="">Variante</option>${options}</select>
      <input name="qty" type="number" min="1" value="1" required />
      <input name="unitPrice" id="price" type="number" min="0" step="0.01" required placeholder="Preço" />
      <button type="submit">Registrar venda</button>
    </form>
    <p class="err" id="msg" ${error ? "" : "hidden"}>${error}</p>
    <p class="ok" id="ok" hidden></p>
  `,
  );
  bindNav();
  const variant = document.getElementById("variant");
  const price = document.getElementById("price");
  variant.addEventListener("change", () => {
    const opt = variant.selectedOptions[0];
    if (opt && opt.dataset.price) price.value = opt.dataset.price;
  });
  document.getElementById("sale-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const msg = document.getElementById("msg");
    const ok = document.getElementById("ok");
    msg.hidden = true;
    ok.hidden = true;
    try {
      const sale = await api("/api/admin/sales", {
        method: "POST",
        body: JSON.stringify({
          items: [
            {
              variantId: Number(fd.get("variantId")),
              qty: Number(fd.get("qty")),
              unitPrice: Number(fd.get("unitPrice")),
            },
          ],
        }),
      });
      ok.textContent = `Venda #${sale.id} — total R$ ${Number(sale.totalAmount).toFixed(2)}`;
      ok.hidden = false;
    } catch (err) {
      msg.textContent = err.message;
      msg.hidden = false;
    }
  });
}

async function renderDashboard() {
  const to = new Date();
  const from = new Date();
  from.setUTCDate(to.getUTCDate() - 13);
  let points = [];
  let error = "";
  try {
    points = await api(
      `/api/admin/sales/daily?from=${isoDate(from)}&to=${isoDate(to)}`,
    );
  } catch (err) {
    error = err.message;
  }

  const revenue = points.reduce((s, p) => s + Number(p.revenue), 0);
  const count = points.reduce((s, p) => s + Number(p.count), 0);
  const max = Math.max(...points.map((p) => Number(p.revenue)), 1);
  const bars = points.length
    ? points
        .map((p) => {
          const h = Math.max(2, Math.round((Number(p.revenue) / max) * 150));
          return `<div class="bar-wrap"><div class="bar" style="height:${h}px" title="R$ ${Number(p.revenue).toFixed(2)}"></div><div class="bar-label">${String(p.date).slice(5)}</div></div>`;
        })
        .join("")
    : `<p class="muted">Sem vendas no período.</p>`;

  app.innerHTML = shell(
    "dashboard",
    `
    <h2>Dashboard</h2>
    <p class="muted">Receita diária (UTC) — últimos 14 dias</p>
    <div class="kpis">
      <div><p class="muted">Receita</p><strong>R$ ${revenue.toFixed(2)}</strong></div>
      <div><p class="muted">Vendas</p><strong>${count}</strong></div>
    </div>
    <p class="err" id="msg" ${error ? "" : "hidden"}>${error}</p>
    <div class="chart">${bars}</div>
  `,
  );
  bindNav();
}

function route(name) {
  if (name !== "login" && !getToken()) {
    renderLogin();
    return;
  }
  if (name === "login") return renderLogin();
  if (name === "inventory") return renderInventory();
  if (name === "sales") return renderSales();
  return renderDashboard();
}

route(getToken() ? "dashboard" : "login");

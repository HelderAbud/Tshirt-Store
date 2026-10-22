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

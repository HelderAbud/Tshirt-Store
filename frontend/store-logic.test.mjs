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

import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

test("store page is a module and does not boot the admin app", () => {
  const html = readFileSync(new URL("./public/store.html", import.meta.url), "utf8");
  assert.match(html, /styles\.css/);
  assert.match(html, /type="module"/);
  assert.match(html, /store\.js/);
  assert.doesNotMatch(html, /app\.js/);
});

test("store script keeps the customer token helper and the pay header", () => {
  const js = readFileSync(new URL("./public/store.js", import.meta.url), "utf8");
  assert.match(js, /CUSTOMER_TOKEN_KEY/);
  assert.match(js, /loginErrorMessage/);
  assert.match(js, /createIdempotencyKey/);
  assert.match(js, /saveReturnRoute/);
  assert.match(js, /Idempotency-Key/);
  assert.match(js, /href="\/"/);
  assert.doesNotMatch(js, /index\.html/);
  assert.doesNotMatch(js, /tshirt_admin_token/);
  assert.doesNotMatch(js, /localStorage\.removeItem\("tshirt_admin_token"\)/);
});

test("frontend readme documents the store url", () => {
  const readme = readFileSync(new URL("./README.md", import.meta.url), "utf8");
  assert.match(readme, /http:\/\/localhost:3000\/store\.html/);
});

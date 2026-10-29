import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const page = readFileSync(new URL("./src/app/nfe/page.tsx", import.meta.url), "utf8");
const api = readFileSync(new URL("./src/api/nfe.ts", import.meta.url), "utf8");
const client = readFileSync(new URL("./src/api/client.ts", import.meta.url), "utf8");
const shell = readFileSync(new URL("./src/components/AdminShell.tsx", import.meta.url), "utf8");
const store = readFileSync(new URL("./public/store.js", import.meta.url), "utf8");

test("admin nav has an NF-e screen that posts the xml", () => {
  assert.match(shell, /\/nfe/);
  assert.match(shell, /NF-e/);
  assert.match(page, /importNfe/);
  assert.match(api, /\/api\/admin\/nfe\/import/);
  assert.match(api, /FormData/);
  assert.match(api, /\/api\/admin\/nfe\/supplier-codes/);
  assert.match(page, /supplierCnpj/);
  assert.match(page, /createSupplierCode/);
  assert.match(client, /instanceof FormData/);
});

test("storefront stays untouched", () => {
  assert.equal(store.includes("/api/admin/nfe/import"), false);
});

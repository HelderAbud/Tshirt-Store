import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const app = readFileSync(new URL("./app.js", import.meta.url), "utf8");
const store = readFileSync(new URL("./store.js", import.meta.url), "utf8");

test("admin nav has an NF-e screen that posts the xml", () => {
  assert.match(app, /data-go="nfe"/);
  assert.match(app, /NF-e/);
  assert.match(app, /\/api\/admin\/nfe\/import/);
  assert.match(app, /FormData/);
  assert.match(app, /instanceof FormData/);
});

test("storefront stays untouched", () => {
  assert.equal(store.includes("/api/admin/nfe/import"), false);
});

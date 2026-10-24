import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

test("admin and store login forms start empty", () => {
  const admin = readFileSync(new URL("./src/app/login/page.tsx", import.meta.url), "utf8");
  const store = readFileSync(new URL("./public/store.js", import.meta.url), "utf8");
  assert.equal(admin.includes("Demo12345!"), false);
  assert.equal(admin.includes("admin@demo.local"), false);
  assert.equal(store.includes("Demo12345!"), false);
  assert.equal(store.includes("customer@demo.local"), false);
});

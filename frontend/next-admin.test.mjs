import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const pkg = JSON.parse(readFileSync(new URL("./package.json", import.meta.url), "utf8"));

test("next is a required dependency and dev serves port 3000", () => {
  assert.equal(pkg.dependencies.next, "14.2.5");
  assert.equal(pkg.dependencies["react-dom"] !== undefined, true);
  assert.equal(pkg.dependencies.recharts !== undefined, true);
  assert.equal(pkg.dependencies.tailwindcss !== undefined, true);
  assert.equal(pkg.dependencies.autoprefixer !== undefined, true);
  assert.equal(pkg.optionalDependencies, undefined);
  assert.equal(pkg.scripts.dev, "next dev -p 3000");
});

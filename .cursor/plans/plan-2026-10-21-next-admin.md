# Painel Next.js Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fazer o Next da pasta `frontend/` ser o painel na porta 3000, com a tela de NF-e, e continuar servindo a vitrine em `/store.html`.

**Architecture:** O App Router que já está em `frontend/src` permanece. A tela NF-e entra nesse app e o cliente HTTP deixa de forçar JSON quando o corpo é `FormData`. A vitrine muda de pasta para `frontend/public/`, então o Next a entrega na mesma origem. `index.html` e `app.js` só saem depois da conferência manual.

**Tech Stack:** Next.js 14.2.5, React 18, Tailwind CSS 3, Recharts 2, Node.js `node:test`. API Spring já em `http://localhost:8084`.

## Global Constraints

- Porta do front: **3000**. Comando: `npm run dev`, que executa `next dev -p 3000`.
- Dependências de instalação, não opcionais: `next` 14.2.5, `react` ^18.3.1, `react-dom` ^18.3.1, `recharts` ^2.12.7, `tailwindcss` ^3.4.17, `autoprefixer` ^10.4.20. Tailwind 4 não serve: o CSS atual usa `@tailwind`.
- Login preenchido com `admin@demo.local` / `Demo12345!`. Role diferente de `ADMIN` não entra.
- Token do admin: `localStorage`, chave `tshirt_admin_token`. Sem cookie httpOnly.
- Sem token, estoque, venda, dashboard e NF-e abrem o login.
- Navegação: Dashboard, Estoque, Venda, NF-e e Sair.
- NF-e: `POST /api/admin/nfe/import`, `FormData` campo `file`, sem forçar `Content-Type: application/json`. Sucesso: `{accessKey} · {direction}`. Erro: `detail`.
- Vitrine em `frontend/public/`: `store.html`, `store.js`, `store-logic.mjs`, `styles.css`. URL `http://localhost:3000/store.html`.
- O link Painel da loja aponta para `/`, não para `index.html`. O resto da loja não muda.
- `index.html` não entra em `public/`.
- `index.html` e `app.js` saem só depois da conferência. Se `npm install` falhar, eles ficam e **A1-04** não é marcado.
- Não mudar a API, o CORS nem a porta 8084. Não reescrever a vitrine em React. Não marcar F1-01, F1-02, F1-03 ou F1-04.
- Commit, push e PR só se o usuário pedir nesta sessão. Ramo, quando for versionar: `feature/next-admin` a partir de `main`.
- Não versionar `node_modules/` nem `.env`.

**Spec:** `docs/superpowers/specs/2026-10-21-next-admin-design.md`

---

### Task 1: Dependências e comando de desenvolvimento

**Files:**
- Modify: `frontend/package.json`
- Test: `frontend/next-admin.test.mjs`

**Interfaces:**
- Consumes: nada.
- Produces: `npm run dev` executa `next dev -p 3000`. `next`, `react`, `react-dom`, `recharts`, `tailwindcss` e `autoprefixer` ficam em `dependencies`.

- [ ] **Step 1: Write the failing test**

`frontend/next-admin.test.mjs`

```javascript
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
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd frontend && node --test next-admin.test.mjs`

Expected: FAIL porque `dependencies` ainda não existe e `dev` ainda é o echo do Python.

- [ ] **Step 3: Write the minimal implementation**

Substituir o bloco de scripts e o de dependências de `frontend/package.json` por:

```json
  "scripts": {
    "dev": "next dev -p 3000",
    "build": "next build",
    "start": "next start -p 3000"
  },
  "dependencies": {
    "next": "14.2.5",
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "recharts": "^2.12.7",
    "tailwindcss": "^3.4.17",
    "autoprefixer": "^10.4.20"
  }
```

Remover `optionalDependencies` e os scripts `serve`, `build:next` e `dev:next`. Atualizar a `description` para: `Painel Next.js na porta 3000. A vitrine estática fica em public/store.html.`

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd frontend && node --test next-admin.test.mjs`

Expected: 1 teste passando.

- [ ] **Step 5: Install**

Run: `cd frontend && npm install`

Expected: exit 0 e `frontend/package-lock.json` criado. `node_modules/` não entra no Git.

Se o install falhar por rede ou TLS, parar o plano aqui. Não mover a vitrine, não apagar `index.html` e não marcar A1-04.

- [ ] **Step 6: Commit — só se o usuário pedir nesta sessão**

```bash
git add frontend/package.json frontend/package-lock.json frontend/next-admin.test.mjs
git commit -m "build(frontend): install Next on port 3000"
```

---

### Task 2: Vitrine servida pelo Next

**Files:**
- Move: `frontend/store.html` → `frontend/public/store.html`
- Move: `frontend/store.js` → `frontend/public/store.js`
- Move: `frontend/store-logic.mjs` → `frontend/public/store-logic.mjs`
- Move: `frontend/styles.css` → `frontend/public/styles.css`
- Modify: `frontend/public/store.js` — os dois links Painel
- Modify: `frontend/store-page.test.mjs`
- Modify: `frontend/store-logic.test.mjs`
- Modify: `frontend/nfe-page.test.mjs` — só o caminho de `store.js`

**Interfaces:**
- Consumes: os quatro arquivos estáticos atuais.
- Produces: `http://localhost:3000/store.html` quando o Next estiver no ar. O href do Painel é `/`.

- [ ] **Step 1: Write the failing test**

Em `frontend/store-page.test.mjs`, trocar as leituras para `./public/store.html`, `./public/store.js` e manter `./README.md`. Acrescentar no teste do script:

```javascript
  assert.match(js, /href="\/"/);
  assert.doesNotMatch(js, /index\.html/);
```

Em `frontend/store-logic.test.mjs`, trocar o import para `./public/store-logic.mjs`.

Em `frontend/nfe-page.test.mjs`, trocar a leitura da loja para `./public/store.js`. A leitura de `./app.js` continua nesta tarefa.

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd frontend && node --test store-page.test.mjs store-logic.test.mjs nfe-page.test.mjs`

Expected: FAIL porque os arquivos ainda não estão em `public/` e o link ainda é `index.html`.

- [ ] **Step 3: Write the minimal implementation**

```bash
mkdir -p frontend/public
git mv frontend/store.html frontend/public/store.html
git mv frontend/store.js frontend/public/store.js
git mv frontend/store-logic.mjs frontend/public/store-logic.mjs
git mv frontend/styles.css frontend/public/styles.css
```

Nos dois pontos de `frontend/public/store.js`, trocar `href="./index.html"` por `href="/"`. Não alterar catálogo, carrinho, pagamento nem a chave `tshirt_customer_token`.

Não copiar `index.html` para `public/`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd frontend && node --test store-page.test.mjs store-logic.test.mjs nfe-page.test.mjs next-admin.test.mjs`

Expected: todos passando.

- [ ] **Step 5: Commit — só se o usuário pedir nesta sessão**

```bash
git add frontend/public frontend/store-page.test.mjs frontend/store-logic.test.mjs frontend/nfe-page.test.mjs
git commit -m "feat(storefront): serve the static shop from the Next app"
```

---

### Task 3: Tela NF-e

**Files:**
- Create: `frontend/src/app/nfe/page.tsx`
- Create: `frontend/src/api/nfe.ts`
- Modify: `frontend/src/api/client.ts` — não forçar JSON em `FormData`
- Modify: `frontend/src/components/AdminShell.tsx` — item NF-e
- Modify: `frontend/nfe-page.test.mjs`

**Interfaces:**
- Consumes: `apiFetch` em `frontend/src/api/client.ts` e `AdminShell`.
- Produces: `importNfe(file: File)` devolve `{ accessKey: string; direction: string }`. Rota `/nfe`.

- [ ] **Step 1: Write the failing test**

Substituir `frontend/nfe-page.test.mjs` por:

```javascript
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
  assert.match(client, /instanceof FormData/);
});

test("storefront stays untouched", () => {
  assert.equal(store.includes("/api/admin/nfe/import"), false);
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd frontend && node --test nfe-page.test.mjs`

Expected: FAIL porque `src/app/nfe/page.tsx` ainda não existe.

- [ ] **Step 3: Write the minimal implementation**

Em `apiFetch`, só definir `Content-Type` quando o corpo não for `FormData`:

```typescript
  const bodyIsForm = typeof FormData !== "undefined" && options.body instanceof FormData;
  if (!headers.has("Content-Type") && options.body && !bodyIsForm) {
    headers.set("Content-Type", "application/json");
  }
```

`frontend/src/api/nfe.ts`

```typescript
import { apiFetch } from "./client";

export type NfeImportResult = {
  accessKey: string;
  direction: string;
};

export async function importNfe(file: File): Promise<NfeImportResult> {
  const body = new FormData();
  body.append("file", file);
  return apiFetch<NfeImportResult>("/api/admin/nfe/import", { method: "POST", body });
}
```

O teste lê `/api/admin/nfe/import` na página. A página pode chamar `importNfe` e também conter a string do path, ou o teste passa se a string estiver na página. Colocar a string na página:

`frontend/src/app/nfe/page.tsx`

```tsx
"use client";

import { FormEvent, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { ApiError } from "@/api/client";
import { importNfe } from "@/api/nfe";

export default function NfePage() {
  const [message, setMessage] = useState("XML já autorizado. A chave repetida não mexe no estoque.");
  const [error, setError] = useState(false);
  const [sending, setSending] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const input = event.currentTarget.elements.namedItem("file");
    if (!(input instanceof HTMLInputElement) || !input.files?.[0]) return;
    setSending(true);
    setError(false);
    setMessage("Importando…");
    try {
      const result = await importNfe(input.files[0]);
      setMessage(`${result.accessKey} · ${result.direction}`);
    } catch (err) {
      setError(true);
      setMessage(err instanceof ApiError ? err.detail ?? err.message : "Falha na importação");
    } finally {
      setSending(false);
    }
  }

  return (
    <AdminShell>
      <section className="rounded-xl border border-[var(--border)] bg-[var(--panel)] p-6">
        <h2 className="text-lg font-semibold">NF-e</h2>
        <form onSubmit={onSubmit} className="mt-4 flex flex-wrap gap-3">
          <input name="file" type="file" accept=".xml,text/xml,application/xml" required />
          <button
            type="submit"
            disabled={sending}
            className="rounded-md bg-[var(--accent)] px-4 py-2 text-white disabled:opacity-60"
          >
            Importar
          </button>
        </form>
        <p className={`mt-3 text-sm ${error ? "text-red-400" : "text-[var(--muted)]"}`}>{message}</p>
      </section>
    </AdminShell>
  );
}
```

Em `AdminShell`, acrescentar `{ href: "/nfe", label: "NF-e" }` depois de Venda.

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd frontend && node --test nfe-page.test.mjs store-page.test.mjs store-logic.test.mjs next-admin.test.mjs`

Expected: todos passando.

- [ ] **Step 5: Commit — só se o usuário pedir nesta sessão**

```bash
git add frontend/src/app/nfe/page.tsx frontend/src/api/nfe.ts frontend/src/api/client.ts frontend/src/components/AdminShell.tsx frontend/nfe-page.test.mjs
git commit -m "feat(admin): import an invoice xml from the Next panel"
```

---

### Task 4: Conferência, retirada do HTML e README

**Files:**
- Delete: `frontend/index.html`
- Delete: `frontend/app.js`
- Modify: `frontend/README.md`
- Modify: `README.md`
- Modify: `docs/BACKLOG.md` — só se a conferência passar

**Interfaces:**
- Consumes: o Next da tarefa 3 e a vitrine em `public/`.
- Produces: o painel abre sem `index.html`. **A1-04** marcado só com a conferência feita.

- [ ] **Step 1: Stop a static server already bound to port 3000**

Se `http://localhost:3000` já responder pelo `python -m http.server` desta pasta, encerrar esse processo antes de subir o Next. Não encerrar outro serviço que não seja esse servidor estático.

- [ ] **Step 2: Run the panel**

Run: `cd frontend && npm run dev`

Expected: o Next aceita conexões em `http://localhost:3000`. Se o comando não subir, parar. Deixar `index.html`, `app.js` e o checkbox A1-04 como estão.

- [ ] **Step 3: Smoke**

Com a API em `http://localhost:8084`:

1. Abrir `http://localhost:3000`, entrar com `admin@demo.local` / `Demo12345!` e chegar ao dashboard.
2. Abrir Estoque, ajustar uma variante e ver a quantidade mudar.
3. Abrir Venda e registrar um item.
4. Abrir NF-e, enviar um XML de entrada de um SKU que existe e ver a chave com `ENTRADA`.
5. Abrir `http://localhost:3000/store.html` e seguir o link Painel até o Next.

Se a API estiver fora, a conferência não conta. Não apagar o HTML e não marcar A1-04.

- [ ] **Step 4: Remove the static admin and update the docs**

Só depois do passo 3 passar:

- Apagar `frontend/index.html` e `frontend/app.js`.
- Em `frontend/README.md`, o jeito de subir o painel passa a ser `npm install` e `npm run dev`. Manter a frase `http://localhost:3000/store.html`.
- Em `README.md`, trocar `python -m http.server 3000` por `npm run dev` na pasta `frontend`. No roadmap, A1-04 deixa de ser o próximo opcional e entra em **Feito**. O trade-off deixa de dizer que o Next ainda não foi instalado.
- Em `docs/BACKLOG.md`:

```markdown
- [x] **A1-04** (opcional) Migrar SPA para Next.js quando `npm install` estiver ok
```

Não marcar F1-01, F1-02, F1-03 nem F1-04.

- [ ] **Step 5: Re-run the Node tests**

Run: `cd frontend && node --test next-admin.test.mjs nfe-page.test.mjs store-page.test.mjs store-logic.test.mjs`

Expected: todos passando. O teste da vitrine ainda encontra `http://localhost:3000/store.html` no README do frontend.

- [ ] **Step 6: Commit — só se o usuário pedir nesta sessão**

```bash
git add frontend/index.html frontend/app.js frontend/README.md README.md docs/BACKLOG.md
git commit -m "docs(frontend): make Next the admin entry point"
```

Não incluir `index.html` e `app.js` se o passo 3 não tiver passado e os arquivos continuarem no disco.

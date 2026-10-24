# Frontend — Tshirt Store

Painel Next.js na porta 3000. A vitrine estática fica em `public/store.html`.

## Como rodar

1. API em `http://localhost:8084` (MySQL + `mvn spring-boot:run`).
2. Nesta pasta:

```bash
npm install
npm run dev
```

3. Painel: http://localhost:3000
4. Login do painel começa vazio. Com a API no perfil `local`: `admin@demo.local` / `Demo12345!`

CORS da API libera `http://localhost:3000`.

`npm install` neste ambiente precisa de `NODE_OPTIONS=--use-system-ca`. O Next permanece em 14.2.5. TypeScript 5 fica em `devDependencies`, porque o Next 14 não sobe com o TypeScript 7 que ele tentaria instalar sozinho.

## Smoke

1. Login admin
2. Estoque: ajuste `delta` + motivo
3. Venda: escolha variante, qtd e preço
4. NF-e: XML de entrada de um SKU existente
5. Dashboard: barra do dia com a receita

Variantes vazias: crie produto/variante no Swagger (`/api/admin/products`).

## Vitrine do cliente

Com a API em `http://localhost:8084` e `npm run dev` nesta pasta, abra http://localhost:3000/store.html

O painel admin continua em http://localhost:3000/

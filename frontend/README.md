# Frontend — Tshirt Store Backoffice

SPA estático (sem build) para a fase 1: login ADMIN → estoque → venda → dashboard.

## Por que estático?

O scaffold Next.js (`src/app`, `package.json`) ficou preparado, mas `npm install` falhou neste ambiente (rede/TLS). A fatia vertical foi entregue com HTML/JS puro que consome a mesma API.

## Como rodar

1. API em `http://localhost:8084` (MySQL + `mvn spring-boot:run`).
2. Neste pasta:

```bash
python -m http.server 3000
```

3. Abra http://localhost:3000  
4. Login: `admin@demo.local` / `Demo12345!`

CORS da API libera `http://localhost:3000`.

## Smoke

1. Login admin  
2. Estoque: ajuste `delta` + motivo  
3. Venda: escolha variante, qtd e preço  
4. Dashboard: barra do dia com a receita  

Variantes vazias: crie produto/variante no Swagger (`/api/admin/products`).

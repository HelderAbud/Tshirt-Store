# Tshirt-Store

> **Loja única de revenda de roupas** — API Spring, backoffice, vitrine do cliente e importação de NF-e. Ver [`docs/BACKLOG.md`](docs/BACKLOG.md).

Backend + SPA de gestão para uma loja de camisetas/roupas. API REST em **Spring Boot**; front admin em [`frontend/`](frontend/).

---

## Resumo para LinkedIn / vitrine (copiar)

**GitHub:** https://github.com/HelderAbud/Tshirt-Store

**T-Shirt Store** *(loja única — API + backoffice)*

API e painel admin: JWT, catálogo com variantes, carrinho/pedidos e-commerce, venda de balcão, ajuste de estoque, dashboard de receita diária, Problem Details e Actuator.

**Tecnologias:** Java 21, Spring Boot 3, Spring Security (JWT), MySQL, Flyway, springdoc-openapi, Actuator, SPA admin (HTML/JS)

**Destaques (estado real):**

- Auth JWT + roles ADMIN/CUSTOMER
- Catálogo, variantes, carrinho, pedidos e pagamento mock
- Backoffice: `/api/admin/inventory/*`, `/api/admin/sales` (+ `/daily`) e UI em `frontend/`
- Vitrine do cliente em `frontend/store.html` (catálogo, carrinho e pagamento mock)
- Importação de XML de NF-e já autorizado (`POST /api/admin/nfe/import`) com impacto no estoque
- Erros RFC 7807 + Actuator health/info/metrics
- CI (Spotless + testes)

---

## Visão para portfólio

| Campo | Valor |
|--------|--------|
| **Pitch em uma linha** | Loja única de roupas: API, backoffice, vitrine e importação de NF-e. Next.js continua opcional. |
| **Deploy / demo** | Local: API **8084**, MySQL **3308**, front **3000** |
| **Vídeo ou post LinkedIn** | Pendente |
| **Destaque técnico** | JWT, Flyway, venda balcão + movimentos, Problem Details, Actuator |
| **Etapas** | Estado atual no [roadmap](#roadmap). [`docs/portfolio/etapas.md`](docs/portfolio/etapas.md) é o recorte de 2026-07-27. |

---

## Sumário

- [Visão geral](#visão-geral)
- [Stack](#stack)
- [Pré-requisitos](#pré-requisitos)
- [Como rodar localmente](#como-rodar-localmente)
- [API e documentação](#api-e-documentação)
- [Autenticação](#autenticação)
- [Testes e qualidade](#testes-e-qualidade)
- [CI](#ci)
- [Estrutura do repositório](#estrutura-do-repositório)
- [Trade-offs](#trade-offs)
- [Roadmap](#roadmap)
- [Licença](#licença)

---

## Visão geral

O projeto segue um MVP em marcos documentados em [`docs/BACKLOG.md`](docs/BACKLOG.md). O backend inclui:

- Healthcheck, **OpenAPI/Swagger**, **Flyway** para versionar o banco
- **Autenticação com JWT** (cadastro CUSTOMER, login, filtro Bearer, endpoint `/api/me`) e **roles ADMIN/CUSTOMER**
- **CRUD admin de Product** e **catálogo público** (`/api/catalog/products`)
- **Carrinho** (`/api/cart`) e **pedidos** (`/api/orders`) — item só por variante; estoque baixa no pagamento mock
- **Problem Details** (RFC 7807) e **Actuator** (health/info/metrics)
- **Backoffice:** ajuste de estoque, venda de balcão, série diária + SPA em `frontend/`
- **Vitrine:** catálogo, carrinho e checkout mock em `frontend/store.html`
- **NF-e:** importação de XML já autorizado, com entrada ou saída de estoque numa transação
- **CI** no GitHub Actions (formatação + testes)

---

## Stack

| Camada | Tecnologia |
|--------|------------|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.2 |
| API | Spring Web (REST) |
| Persistência | Spring Data JPA, MySQL 8 |
| Migrações | Flyway |
| Segurança | Spring Security, JWT (JJWT), BCrypt |
| Observabilidade | Spring Boot Actuator, Micrometer |
| Documentação API | springdoc-openapi (Swagger UI) |
| Build | Maven |
| Testes | JUnit 5, MockMvc, Mockito |
| Formatação | Spotless (Google Java Format) |
| Infra local | Docker Compose (MySQL) |

---

## Pré-requisitos

- **JDK 21**
- **Maven 3.9+** (ou use o wrapper se existir no projeto)
- **Docker** e **Docker Compose** (para subir o MySQL de desenvolvimento)

---

## Como rodar localmente

### 1. Subir o MySQL

Na raiz do repositório:

```bash
cp .env.example .env
cd infra
docker compose --env-file ../.env up -d
```

Edite o `.env` local antes de rodar fora de experimentos. O Compose sobe MySQL 8 com:

- Banco: `loja_revenda`
- Usuário e senha definidos por `MYSQL_USER` e `MYSQL_PASSWORD`
- Porta (host): `3308` (matriz portfólio; container interno 3306)

Aguarde o container ficar saudável antes de iniciar a API.

### 2. Subir o backend

```bash
cd backend
mvn spring-boot:run
```

A aplicação sobe na porta **8084** (`server.port` / `SERVER_PORT`; matriz portfólio). Defina também `JWT_SECRET` com um valor Base64 de pelo menos 256 bits.

### 3. (Opcional) Apenas testes sem Docker

Os testes usam **H2 em memória** e o perfil `test` ([`backend/src/test/resources/application-test.yml`](backend/src/test/resources/application-test.yml)):

```bash
cd backend
mvn test
```

---

## API e documentação

| Recurso | URL (local) |
|---------|----------------|
| OpenAPI (JSON) | http://localhost:8084/v3/api-docs |
| Swagger UI | http://localhost:8084/swagger-ui.html |
| Health | `GET` http://localhost:8084/api/health |
| Actuator health / info | http://localhost:8084/actuator/health · `/actuator/info` (públicos) |
| Actuator metrics | http://localhost:8084/actuator/metrics (**ADMIN** + JWT) |

### Observabilidade

Actuator expõe `health` (com probes liveness/readiness), `info` e `metrics`. Detalhes de componentes do health só com utilizador autenticado (`show-details: when_authorized`).

### Backoffice (fase 1)

| Método | Path | Auth |
|--------|------|------|
| POST | `/api/admin/inventory/adjust` | ADMIN (`variantId`, `delta`, `reason`) |
| GET | `/api/admin/inventory/stock` | ADMIN |
| GET | `/api/admin/inventory/low-stock?threshold=` | ADMIN |
| POST | `/api/admin/sales` | ADMIN (itens com `variantId`, `qty`, `unitPrice`) |
| GET | `/api/admin/sales?from=&to=` | ADMIN (datas ISO) |
| GET | `/api/admin/sales/daily?from=&to=` | ADMIN |

UI: [`frontend/README.md`](frontend/README.md) — `python -m http.server 3000` → http://localhost:3000 (painel) e http://localhost:3000/store.html (vitrine).

### NF-e (fase 3)

| Método | Path | Auth |
|--------|------|------|
| POST | `/api/admin/nfe/import` | ADMIN (multipart, campo `file`, XML até 1 MB) |

**Nota:** pedidos e-commerce (`/api/orders` + pay) e vendas de balcão (`/api/admin/sales`) são fluxos separados de baixa de estoque. A importação de NF-e é um terceiro caminho e reutiliza o ajuste de estoque.

### Erros (Problem Details)

Respostas de erro de negócio/validação seguem [RFC 7807](https://datatracker.ietf.org/doc/html/rfc7807) via `ProblemDetail` do Spring (`type`, `title`, `status`, `detail`). Validação Bean Validation usa `detail: "Validation failed"` e a extensão `errors` (mapa campo → mensagem).

---

## Autenticação

- **Cadastro:** `POST /api/auth/register` com JSON `{ "email", "password" }` (mínimo 8 caracteres) — cria sempre `CUSTOMER`, responde **201** com `{ "token", "email", "role" }`. Email já existente → **409**.
- **Login:** `POST /api/auth/login` com JSON `{ "email", "password" }` — resposta inclui `token` (JWT), `email` e `role` (`ADMIN` ou `CUSTOMER`).
- **Token:** o JWT vai no JSON da resposta. Cookie httpOnly fica para o frontend (ainda adiado). Rotas protegidas usam header `Authorization: Bearer <token>`.
- **Exemplo:** `GET /api/me` retorna o usuário autenticado (email e role).
- **Roles:** `/api/admin/**` exige `ADMIN` (CUSTOMER → 403; sem token → 401). `/api/catalog/**` é público.

### Utilizadores demo (seed Flyway V4)

Só para **local / portfólio** — não use em produção.

| Email | Senha | Role |
|-------|-------|------|
| `admin@demo.local` | `Demo12345!` | ADMIN |
| `customer@demo.local` | `Demo12345!` | CUSTOMER |

### Demo em ~5 minutos

1. `cd infra && docker compose --env-file ../.env up -d` (MySQL host **3308**)
2. `cd backend && mvn spring-boot:run` (API **8084**; Flyway aplica V1–V4)
3. Login admin:
   ```bash
   curl -s -X POST http://localhost:8084/api/auth/login \
     -H "Content-Type: application/json" \
     -d "{\"email\":\"admin@demo.local\",\"password\":\"Demo12345!\"}"
   ```
4. Copie o `token` e crie um produto:
   ```bash
   curl -s -X POST http://localhost:8084/api/admin/products \
     -H "Authorization: Bearer <token>" \
     -H "Content-Type: application/json" \
     -d "{\"name\":\"Camiseta Demo\",\"sku\":\"DEMO-001\",\"price\":59.90,\"stockQty\":5}"
   ```
5. Catálogo público: `GET http://localhost:8084/api/catalog/products`

Swagger: http://localhost:8084/swagger-ui.html

### Product (MVP Opção B)

| Método | Path | Auth |
|--------|------|------|
| POST | `/api/admin/products` | ADMIN |
| GET | `/api/admin/products/{id}` | ADMIN |
| PUT | `/api/admin/products/{id}` | ADMIN |
| DELETE | `/api/admin/products/{id}` | ADMIN |
| POST | `/api/admin/products/{id}/variants` | ADMIN |
| GET | `/api/admin/products/{id}/variants` | ADMIN |
| GET | `/api/catalog/products?page=&size=` | público (sem `variants`) |
| GET | `/api/catalog/products/{id}` | público (inclui `variants`) |

### Carrinho e pedidos (B3)

| Método | Path | Auth |
|--------|------|------|
| GET | `/api/cart` | autenticado |
| PUT | `/api/cart/items` | autenticado (`variantId`, `qty` >= 1; substitui qty) |
| DELETE | `/api/cart/items/{variantId}` | autenticado |
| POST | `/api/orders` | autenticado (checkout; limpa carrinho; não baixa estoque) |
| POST | `/api/orders/{id}/pay` | dono + header `Idempotency-Key` (mock sempre aprova; baixa variante e alinha `stockQty` do produto à soma das variantes) |
| GET | `/api/orders` | autenticado (lista sem items) |
| GET | `/api/orders/{id}` | autenticado (detalhe; 404 se não for o dono) |
| PATCH | `/api/admin/orders/{id}/status` | ADMIN (`CREATED` → `CANCELLED`) |

Detalhes de modelo e fluxo estão cobertos por testes no pacote `com.revenda`.

---

## Testes e qualidade

```bash
cd backend
mvn test
```

Formatação (deve passar no CI):

```bash
cd backend
mvn spotless:check
```

Para aplicar formatação automaticamente:

```bash
mvn spotless:apply
```

Checklist rápido após mudanças:

1. `mvn test` e `mvn spotless:check` verdes
2. Smoke: login demo → criar produto → listar catálogo
3. Conferir Swagger em http://localhost:8084/swagger-ui.html

---

## CI

O workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml) roda em **push** e **pull request** para `main`, `develop`, `feature/*`, `release/*` e `hotfix/*`:

1. `mvn spotless:check`
2. `mvn -DskipTests package`
3. `mvn test`

---

## Estrutura do repositório

```
Tshirt-Store/
├── backend/                 # API Spring Boot (Maven)
│   └── src/main/java/com/revenda/
│       ├── controller/      # REST
│       ├── service/         # Regras e JWT
│       ├── repository/      # Spring Data JPA
│       ├── domain/          # Entidades
│       ├── exception/       # Problem Details (RFC 7807)
│       └── config/          # Segurança, filtros JWT
├── frontend/                # Painel admin e vitrine (HTML/JS)
├── docs/                    # Backlog, workflow, arquitetura
├── infra/
│   └── docker-compose.yml   # MySQL para desenvolvimento
└── README.md
```

---

## Trade-offs

Decisões conscientes do MVP (honestidade de portfólio):

| Escolha | Porquê | Limite |
|---------|--------|--------|
| HTML/JS no lugar de Next.js | Painel e vitrine entregues sem depender de `npm install` | Next.js segue opcional (A1-04) |
| Pagamento mock + idempotência | Prova fluxo de estoque sem gateway real | Não processa dinheiro |
| MySQL local + H2 nos testes | Setup simples e CI rápido | Sem Testcontainers neste MVP |
| JWT no body (sem cookie httpOnly) | Simples para Swagger/curl; cookie fica para o front | Menos alinhado a SPA real |
| Actuator metrics só ADMIN | Evita vazamento público de métricas | Sem Prometheus/Grafana ainda |
| Sem deploy público | Custo zero; demo local com seed | Recrutador precisa subir local |

---

## Roadmap

O planejamento detalhado está em **[`docs/BACKLOG.md`](docs/BACKLOG.md)**.

- **Feito:** B0–B4 · P1 · A1 backoffice (API + SPA) · F2 vitrine · F3 importação de NF-e
- **Próximo:** nenhum marco obrigatório aberto
- **Opcional:** A1-04 Next.js quando npm/rede permitir

---

## Licença

Distribuído sob a licença [MIT](LICENSE). Copyright (c) 2026 Helder Abud.

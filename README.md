# Tshirt-Store

> **MVP em curso** (Opção B) — roles, catálogo, carrinho e pedidos. Frontend adiado. Ver [`TRILHA-DIA-A-DIA.md`](TRILHA-DIA-A-DIA.md).

Backend de um **e-commerce de camisetas** (loja de revenda) em MVP. API REST em **Spring Boot**; frontend Next.js ainda adiado.

---

## Resumo para LinkedIn / vitrine (copiar)

**GitHub:** https://github.com/HelderAbud/Tshirt-Store

**T-Shirt Store API** *(MVP em curso)*

API REST para loja de camisetas: healthcheck, JWT com roles, Flyway/MySQL, Swagger e CI. Catálogo, variantes, carrinho persistido e pedidos; frontend adiado.

**Tecnologias:** Java 21, Spring Boot 3, Spring Security (JWT), MySQL, Flyway, springdoc-openapi, GitHub Actions

**Destaques (estado real):**

- Auth JWT (`POST /api/auth/register`, `POST /api/auth/login`, `/api/me`) + roles ADMIN/CUSTOMER
- Admin Product + variantes + catálogo público
- Carrinho persistido (`/api/cart`) e pedidos (`POST/GET /api/orders`, PATCH admin status)
- Health + OpenAPI/Swagger + Flyway
- CI (Spotless + testes)

---

## Visão para portfólio

| Campo | Valor |
|--------|--------|
| **Pitch em uma linha** | API REST (camisetas) com JWT, catálogo com variantes, carrinho e pedidos — frontend adiado. |
| **Deploy / demo** | Não publicado ainda |
| **Vídeo ou post LinkedIn** | Pendente — após fechar DoD Opção B |
| **Destaque técnico** | JWT roles, Flyway, Swagger/OpenAPI, GitHub Actions, Spotless |
| **Etapas do MVP** | [`docs/portfolio/etapas.md`](docs/portfolio/etapas.md) · screenshot [`docs/screenshots/swagger-products.png`](docs/screenshots/swagger-products.png) |

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
- [Roadmap](#roadmap)
- [Licença](#licença)

---

## Visão geral

O projeto segue um MVP em marcos documentados em [`docs/BACKLOG.md`](docs/BACKLOG.md). Hoje o backend inclui:

- Healthcheck, **OpenAPI/Swagger**, **Flyway** para versionar o banco
- **Autenticação com JWT** (cadastro CUSTOMER, login, filtro Bearer, endpoint `/api/me`) e **roles ADMIN/CUSTOMER**
- **CRUD admin de Product** e **catálogo público** (`/api/catalog/products`)
- **Carrinho** (`/api/cart`) e **pedidos** (`/api/orders`) — item só por variante; estoque baixa no pagamento mock
- **CI** no GitHub Actions (formatação + testes)

Frontend ainda não está implementado (ver [Roadmap](#roadmap)).

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
| Actuator (health/info) | http://localhost:8084/actuator |

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
│       └── config/          # Segurança, filtros JWT
├── docs/                    # Backlog, workflow, arquitetura
├── infra/
│   └── docker-compose.yml   # MySQL para desenvolvimento
└── README.md
```

---

## Roadmap

O planejamento detalhado (B0 → B4, frontend F1, polimento P1) está em **[`docs/BACKLOG.md`](docs/BACKLOG.md)**.

Próximos marcos: frontend Next.js (F1 adiado).

---

## Licença

Distribuído sob a licença [MIT](LICENSE). Copyright (c) 2026 Helder Abud.

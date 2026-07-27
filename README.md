# Tshirt-Store

> **MVP em curso** (Opção B) — roles ADMIN/CUSTOMER + Product/catálogo. Sem carrinho/frontend nesta fatia. Ver [`TRILHA-DIA-A-DIA.md`](TRILHA-DIA-A-DIA.md) e [`.cursor/plans/plan-2026-07-27-tshirt-mvp-b.md`](.cursor/plans/plan-2026-07-27-tshirt-mvp-b.md).

Backend de um **e-commerce de camisetas** (loja de revenda) em MVP. Este repositório contém a **API REST** em **Spring Boot**; carrinho, pedidos e frontend **ainda não** estão no escopo desta trilha.

---

## Resumo para LinkedIn / vitrine (copiar)

**GitHub:** https://github.com/HelderAbud/Tshirt-Store

**T-Shirt Store API** *(MVP em curso)*

API REST para loja de camisetas: healthcheck, JWT com roles, Flyway/MySQL, Swagger e CI. Catálogo Product em implementação; carrinho/pedidos/front adiados.

**Tecnologias:** Java 21, Spring Boot 3, Spring Security (JWT), MySQL, Flyway, springdoc-openapi, GitHub Actions

**Destaques (estado real):**

- Auth JWT (`POST /api/auth/login`, `/api/me`) + roles ADMIN/CUSTOMER
- Admin Product + catálogo público (Opção B)
- Health + OpenAPI/Swagger + Flyway
- CI (Spotless + testes)

---

## Visão para portfólio

| Campo | Valor |
|--------|--------|
| **Pitch em uma linha** | API REST (camisetas) com JWT + roles, Product admin e catálogo público — MVP backend em curso; carrinho/front adiados. |
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
- **Autenticação com JWT** (login, filtro Bearer, endpoint `/api/me`) e **roles ADMIN/CUSTOMER**
- **CRUD admin de Product** e **catálogo público** (`/api/catalog/products`)
- **CI** no GitHub Actions (formatação + testes)

Carrinho, pedidos e frontend ainda não estão implementados (ver [Roadmap](#roadmap)).

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

- **Login:** `POST /api/auth/login` com JSON `{ "email", "password" }` — resposta inclui `token` (JWT), `email` e `role` (`ADMIN` ou `CUSTOMER`).
- **Rotas protegidas:** enviar header `Authorization: Bearer <token>`.
- **Exemplo:** `GET /api/me` retorna o usuário autenticado (email e role).
- **Roles:** `/api/admin/**` exige `ADMIN` (CUSTOMER → 403; sem token → 401). `/api/catalog/**` é público.

### Product (MVP Opção B)

| Método | Path | Auth |
|--------|------|------|
| POST | `/api/admin/products` | ADMIN |
| GET | `/api/admin/products/{id}` | ADMIN |
| PUT | `/api/admin/products/{id}` | ADMIN |
| DELETE | `/api/admin/products/{id}` | ADMIN |
| GET | `/api/catalog/products?page=&size=` | público |
| GET | `/api/catalog/products/{id}` | público |

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

Próximos marcos típicos: variantes de produto, carrinho e pedidos, depois frontend Next.js (F1 adiado nesta trilha).

---

## Licença

Distribuído sob a licença [MIT](LICENSE). Copyright (c) 2026 Helder Abud.

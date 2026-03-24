# Tshirt-Store

Backend de um **e-commerce de camisetas** (loja de revenda) em construção. Este repositório contém o **API REST** em **Spring Boot**; o frontend (Next.js) está planejado no roadmap.

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
- **Autenticação com JWT** (login, filtro Bearer, endpoint `/api/me`)
- **CI** no GitHub Actions (formatação + testes)

Catálogo de produtos, carrinho, pedidos e frontend ainda não estão implementados (ver [Roadmap](#roadmap)).

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
cd infra
docker compose up -d
```

Isso sobe MySQL 8 com:

- Banco: `loja_revenda`
- Usuário: `app` / senha: `app` (alinhado a [`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml))
- Porta: `3306`

Aguarde o container ficar saudável antes de iniciar a API.

### 2. Subir o backend

```bash
cd backend
mvn spring-boot:run
```

A aplicação sobe na porta **8080** (padrão Spring Boot), salvo override em variáveis de ambiente ou perfil.

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
| OpenAPI (JSON) | http://localhost:8080/v3/api-docs |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| Health | `GET` http://localhost:8080/api/health |
| Actuator (health/info) | http://localhost:8080/actuator |

---

## Autenticação

- **Login:** `POST /api/auth/login` com JSON `{ "email", "password" }` — resposta inclui `token` (JWT), `email` e `role`.
- **Rotas protegidas:** enviar header `Authorization: Bearer <token>`.
- **Exemplo:** `GET /api/me` retorna o usuário autenticado (email e role).

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

Próximos marcos típicos: autorização por role (ADMIN/CUSTOMER), CRUD de produtos, carrinho e pedidos, depois frontend Next.js.

---

## Licença

Veja o arquivo [`LICENSE`](LICENSE) na raiz do repositório.

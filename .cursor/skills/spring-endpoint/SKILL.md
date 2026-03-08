---
name: spring-endpoint
description: Cria ou modifica endpoints REST em Java Spring Boot seguindo boas práticas do projeto. Use ao criar ou alterar endpoints REST, operações CRUD, controllers Spring ou novas rotas de API.
---

# Skill: Spring Endpoint

## Objetivo

Esta skill cria ou modifica endpoints REST seguindo boas práticas de projetos Java Spring Boot.

**Stack padrão:** Java 21, Spring Boot 3, Spring Data JPA, PostgreSQL, Flyway, Spring Security, Testcontainers.

---

## Fluxo para criação de endpoint

### 1. Entender o caso de uso

- Qual recurso está sendo manipulado?
- Qual ação será executada?
- Exemplos: Criar produto, Listar produtos, Atualizar estoque

### 2. Definir contrato da API

- Rota REST
- Método HTTP
- DTO de request
- DTO de response

Exemplo: `POST /api/produtos` → Request: `{"nome": "Camiseta", "preco": 99.90}`

### 3. Modelagem de domínio

Criar ou atualizar entidade JPA (ex.: Produto, Pedido, Cliente).

### 4. Persistência

Criar repository usando Spring Data JPA (ex.: `ProdutoRepository`).

### 5. Implementação em camadas

| Camada | Responsabilidade |
|--------|------------------|
| Controller | Recebe requisição, valida DTO, chama service |
| Service | Executa lógica de negócio |
| Repository | Acessa banco |

### 6. Banco de dados

Se necessário: criar **nova** migration Flyway.

**Regra crítica:** Nunca alterar migrations antigas. Sempre criar nova migration.

### 7. Validação

Usar Bean Validation: `@NotNull`, `@NotBlank`, `@Positive`, etc.

### 8. Segurança

Verificar se endpoint precisa de autenticação e/ou autorização. Usar Spring Security com JWT.

### 9. Testes

- Testes unitários: JUnit + Mockito
- Testes de integração: Testcontainers

---

## Checklist rápido

- [ ] Contrato da API definido (rota, método, DTOs)
- [ ] Entidade JPA e Repository criados ou atualizados
- [ ] Migration Flyway criada (se schema mudou)
- [ ] Controller → Service → Repository implementados
- [ ] Bean Validation nos DTOs de entrada
- [ ] Segurança verificada (JWT, roles)
- [ ] Testes unitários e de integração

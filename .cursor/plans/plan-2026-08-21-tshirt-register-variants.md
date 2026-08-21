# Tshirt-Store — Register + Unique 409 + ProductVariant

> **For agentic workers:** execute task-by-task with TDD. **Não** commit/push/PR sem pedido explícito do Helder (passo 4 HITL).

**Data:** 2026-08-21  
**Trilha:** Helder Normal · skills: `to-spec` → `to-issues` → `tdd` → `slice-verification`  
**Branch (quando executar):** a partir de `develop` atualizada → `feature/b102-register-variants`  
**Estado:** spec/plano gravados (passo 1). Código ainda não iniciado. Commit/PR = passo 4, só após aprovação.

**Goal:** Fechar o gap B1-01 (cadastro CUSTOMER + 409 de unique) e em seguida B2-02 (variantes size/color/sku) sem carrinho nem frontend.

**Architecture:** Manter camadas atuais (`controller` DTO record → `service` → `repository` → Flyway). Register reutiliza `AuthService` + `JwtService` e o JSON de login. Unique do banco cai num handler global de `DataIntegrityViolationException`. Variante é entidade filha de `Product`; catálogo detalhe inclui `variants`; listagem paginada não faz join (anti N+1).

**Tech Stack:** Java 21, Spring Boot 3.2, Spring Security JWT, JPA, Flyway, H2 nos testes, MockMvc, Spotless.

## Global Constraints

- Prefixos REST existentes: `/api/auth`, `/api/admin`, `/api/catalog`
- Roles: só `ADMIN` e `CUSTOMER`; register nunca cria `ADMIN`
- JWT no JSON (`token`); cookie httpOnly fica para o front (adiado)
- Schema só via nova migration (`V5__…`); não editar V1–V4
- Testes: `cd backend && mvn test` e `mvn spotless:check`
- Autor Git: Helder Abud; sem `Co-authored-by: Cursor`
- GitFlow: feature → PR para `develop`; sem merge automático
- Fora de escopo: carrinho, pedido, pagamento, Next.js, Problem Details (P1-01), recarregar role no banco a cada request, PUT/DELETE de variante, busca `?q=`

---

## Spec

### Problem

O backlog e os prompts de IA prometem `POST /auth/register` e ProductVariant. O código em `main` só tem login + Product simples. Unique no MySQL (`users.email`, `products.sku`) pode virar 500 em corrida porque o service só faz `exists*` antes do `save`.

### Scope

**Fatia 3A (executar primeiro)**

- `POST /api/auth/register` público
- Body: `email` (`@Email` + `@NotBlank`), `password` (`@NotBlank` + `@Size(min = 8)`)
- Sempre `UserRole.CUSTOMER`, senha com BCrypt
- Sucesso: **201** `{ "token", "email", "role": "CUSTOMER" }` (mesmo shape de `LoginResponse`)
- Email duplicado: **409**
- Payload inválido: **400**
- `SecurityFilterChain`: `permitAll` em `/api/auth/login` e `/api/auth/register`
- `GlobalExceptionHandler`: `DataIntegrityViolationException` → 409 `{ "status": 409, "error": "Resource already exists" }` (sem SQL na mensagem)
- Docs: `docs/BACKLOG.md` B1-01, `README.md` (auth), `docs/ai/PROJECT_CONTEXT.md` (JWT no JSON)

**Fatia 2 (só depois da 3A verde + aprovação verbal do Helder)**

- Tabela `product_variants` (Flyway V5)
- `POST /api/admin/products/{id}/variants` e `GET /api/admin/products/{id}/variants` (ADMIN)
- Campos: `color`, `size`, `sku` UNIQUE, `price`, `stockQty` (>= 0)
- Produto inexistente: 404; SKU duplicado: 409; validação: 400; CUSTOMER: 403
- `GET /api/catalog/products/{id}` inclui `variants` (array, pode ser vazio)
- Listagem `GET /api/catalog/products` **não** carrega variantes
- Product atual (`sku`/`price`/`stockQty` no pai) permanece

### Out of scope

Carrinho, `variantId` no pedido, unificar SKU produto+variante numa constraint global, frontend, deploy, refresh token, RFC 7807.

### Decisions

| Tema | Decisão |
|------|---------|
| Register devolve JWT? | Sim, 201 + mesmo JSON do login |
| Quem pode se cadastrar? | Só CUSTOMER |
| Unique 409 | Handler global + `exists*` no caminho feliz |
| SKU do Product | Continua no pai; variante tem SKU próprio (namespaces separados até o carrinho) |
| Endpoints variante | Só POST + GET list; sem PUT/DELETE nesta fatia |
| Catálogo list | Sem `variants` (N+1) |

### Risks

- SKU duplicado entre tabela `products` e `product_variants` não é detectado (dívida até B3)
- Seed V4 e register podem colidir se alguém cadastrar `admin@demo.local` / `customer@demo.local` → 409 esperado
- Handler 409 genérico cobre unique de email e sku; não distingue o campo (aceitável no MVP)

### Verification

- `cd backend && mvn test`
- `cd backend && mvn spotless:check`
- Swagger: register + variants visíveis
- Checklist `docs/ai/DEFINITION_OF_DONE.md` (backend)

---

## File map

| Path | Papel |
|------|--------|
| `backend/src/main/java/com/revenda/controller/AuthController.java` | `POST /register` |
| `backend/src/main/java/com/revenda/service/AuthService.java` | `register(email, rawPassword)` |
| `backend/src/main/java/com/revenda/repository/UserRepository.java` | `existsByEmail` |
| `backend/src/main/java/com/revenda/config/SecurityConfig.java` | permitAll register |
| `backend/src/main/java/com/revenda/exception/GlobalExceptionHandler.java` | 409 integrity |
| `backend/src/test/java/com/revenda/controller/AuthRegisterTest.java` | MockMvc register (novo) |
| `backend/src/main/resources/db/migration/V5__create_product_variants_table.sql` | Schema variantes |
| `backend/src/main/java/com/revenda/domain/ProductVariant.java` | Entidade |
| `backend/src/main/java/com/revenda/repository/ProductVariantRepository.java` | `existsBySku` |
| `backend/src/main/java/com/revenda/service/ProductVariantService.java` | create + list |
| `backend/src/main/java/com/revenda/controller/AdminProductVariantController.java` | `/api/admin/products/{id}/variants` |
| `backend/src/main/java/com/revenda/controller/CatalogProductController.java` | detalhe com `variants` |
| `docs/BACKLOG.md`, `README.md`, `docs/ai/PROJECT_CONTEXT.md` | Contrato escrito |

---

## Slices (to-issues)

| ID | Título | Tipo | Blocked by | Aceite |
|----|--------|------|------------|--------|
| S1 | Register CUSTOMER 201 + 400/409 | HITL após verde | None | Register público; nunca ADMIN; docs B1-01 |
| S2 | 409 genérico de unique no banco | AFK | S1 | Integrity → 409; SKU duplicado ainda 409 |
| S3 | ProductVariant POST/GET admin | HITL | S1+S2 | V5 + 201/409/400/403/404 |
| S4 | Catálogo detalhe com `variants` | HITL | S3 | Detail tem array; list sem variantes |

Execução: **S1+S2 = Fatia 3A**. Parar e pedir OK. **S3+S4 = Fatia 2**. Passo 4 (commit/PR) só com “pode commit/PR”.

---

### Task 1: Register (S1)

**Files:**
- Create: `backend/src/test/java/com/revenda/controller/AuthRegisterTest.java`
- Modify: `AuthController.java`, `AuthService.java`, `UserRepository.java`, `SecurityConfig.java`
- Docs: `docs/BACKLOG.md`, `README.md` (secção Autenticação), `docs/ai/PROJECT_CONTEXT.md`

**Interfaces:**
- Consumes: `UserRepository.findByEmail`, `PasswordEncoder`, `JwtService.generateToken`
- Produces: `AuthService.register(String email, String rawPassword) -> User` (throws conflict se email existe)

- [ ] **Step 1: Teste que falha** — `AuthRegisterTest` `@SpringBootTest` + MockMvc:

  - POST `/api/auth/register` `{"email":"novo@demo.local","password":"Demo12345!"}` → 201, `role=CUSTOMER`, `token` não vazio
  - mesmo email de novo → 409
  - password `"short"` → 400
  - token do register **não** acessa `POST /api/admin/products` → 403

- [ ] **Step 2:** `cd backend && mvn test -Dtest=AuthRegisterTest` → FAIL (404 ou 401 no register)

- [ ] **Step 3: Implementação mínima**

  - `UserRepository.existsByEmail(String email)`
  - `AuthService.register`: se existe → `DuplicateEmailException` **ou** reutilizar padrão 409; senão `save(new User(email, encoder.encode(password), CUSTOMER))`
  - Preferir uma `DuplicateResourceException` / `DuplicateEmailException` paralela a `DuplicateSkuException` se o handler de integrity ainda não existir (Task 2 cobre integrity)
  - `AuthController.register` → 201 + `LoginResponse`
  - `SecurityConfig`: incluir `"/api/auth/register"` no `permitAll` junto com login

- [x] **Step 4:** `mvn test -Dtest=AuthRegisterTest,AdminProductAuthorizationTest,DemoUserSeedLoginTest` → PASS

- [ ] **Step 5:** Atualizar docs. **Não commit** até passo 4 HITL.

---

### Task 2: Integrity 409 (S2)

**Files:**
- Modify: `GlobalExceptionHandler.java`
- Test: estender `AdminProductControllerTest` **ou** teste de service; o 409 sequencial de SKU já existe — garantir que o handler novo não quebra mensagem útil do `DuplicateSkuException` (esse continua específico)

- [ ] **Step 1:** Handler:

```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<Map<String, Object>> handleConflict(DataIntegrityViolationException ex) {
  Map<String, Object> body = new HashMap<>();
  body.put("status", 409);
  body.put("error", "Resource already exists");
  return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
}
```

- [ ] **Step 2:** `mvn test` → PASS (SKU duplicado ainda 409 via `DuplicateSkuException` **antes** de bater no banco)

- [ ] **Step 3:** Docs se necessário. **Não commit.**

**HITL:** mostrar evidência `mvn test` da Fatia 3A. Só então Fatia 2.

---

### Task 3: Variantes admin (S3)

**Files:**
- Create: `V5__create_product_variants_table.sql`
- Create: `ProductVariant.java`, `ProductVariantRepository.java`, `ProductVariantService.java`, `AdminProductVariantController.java`
- Test: `backend/src/test/java/com/revenda/controller/AdminProductVariantControllerTest.java`

**Schema V5:**

```sql
CREATE TABLE product_variants (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  color VARCHAR(64) NOT NULL,
  size VARCHAR(32) NOT NULL,
  sku VARCHAR(100) NOT NULL UNIQUE,
  price DECIMAL(12, 2) NOT NULL,
  stock_qty INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_product_variants_product
    FOREIGN KEY (product_id) REFERENCES products(id)
);
CREATE INDEX idx_product_variants_product_id ON product_variants(product_id);
```

**DTO request:** `@NotBlank color`, `@NotBlank size`, `@NotBlank sku`, `@NotNull @DecimalMin("0.00") price`, `@NotNull @Min(0) stockQty`

- [ ] Testes: POST sem token 401; CUSTOMER 403; produto 999999 404; POST válido 201; SKU repetido 409; price negativo 400; GET list contém o criado
- [ ] `ProductVariantService.create` chama `ProductService.getById` (404) e `existsBySku`
- [ ] `mvn test` PASS
- [ ] **Não commit** até HITL

---

### Task 4: Catálogo detalhe (S4)

**Files:**
- Modify: `CatalogProductController.java` record para incluir `List<CatalogVariantResponse> variants`
- Modify: `ProductService.getById` **ou** query no repository com `@EntityGraph` / `JOIN FETCH` só no detalhe
- Test: `CatalogProductControllerTest` — detalhe com 0 variantes `$.variants` array vazio; após criar variante, detalhe contém `sku` da variante; `GET /api/catalog/products` **não** exige `$.content[0].variants`

- [ ] `mvn test` + `mvn spotless:check`
- [ ] Marcar `[x] B2-02` e register em `docs/BACKLOG.md`
- [ ] **Não commit**

---

## Passo 4 (HITL) — só se o Helder pedir

1. `git status` / `git diff` — sem `.env` / secrets  
2. Commit Conventional: `feat(auth): add customer register and unique conflict handler` e, se a fatia 2 já estiver no mesmo branch, segundo commit `feat(catalog): add product variants` **ou** um commit se o Helder preferir um PR único  
3. Push + `gh pr create --base develop`  
4. **Não mergear**

---

## Self-review

- Spec 3A + B2-02 cobertos por Tasks 1–4  
- Sem TBD  
- Nomes: `/api/auth/register`, `product_variants`, `stockQty` JSON = `stock_qty` coluna (igual Product)  
- Commit não é automático neste plano  

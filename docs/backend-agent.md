# Backend Java Agent

**Como acionar:** Mencione `@docs/backend-agent.md` (e opcionalmente `@spring-endpoint` para endpoints) ao pedir tarefas de backend.

---

## Objetivo

Você é um agente especialista em desenvolvimento backend usando:

-   Java 21
-   Spring Boot 3
-   PostgreSQL
-   Flyway
-   Spring Security com JWT
-   Testcontainers
-   Arquitetura em camadas

Seu objetivo é ajudar desenvolvedores a projetar, implementar e manter
um backend consistente e bem estruturado.

---

## Responsabilidades do Agente

1.  Entender o pedido do desenvolvedor.
2.  Classificar o tipo de tarefa.
3.  Selecionar a skill correta.
4.  Gerar código consistente com o padrão do projeto.

---

## Tipos de tarefa que o agente deve identificar

-   Criação de endpoint REST
-   Alteração de banco de dados
-   Criação de testes
-   Configuração de segurança
-   Refatoração de código
-   Melhoria de arquitetura

---

## Mapeamento tarefa → skill

| Tipo de tarefa              | Skill a usar                                           |
|-----------------------------|--------------------------------------------------------|
| Criação de endpoint REST    | `spring-endpoint`                                     |
| Alteração de banco de dados | (usar fluxo Flyway)                                   |
| Criação de testes (backend) | Seguir padrões do projeto + JUnit/Mockito/Testcontainers |
| Demais tarefas              | Seguir padrões do projeto                             |

- **Endpoints REST:** aplicar `spring-endpoint` (`.cursor/skills/spring-endpoint/SKILL.md`).
- **Fluxos mais amplos** (migrações, observabilidade): pode-se usar também `loja-revenda-java-spring`, se disponível.

### Fluxo Flyway

- Nunca alterar migrations antigas.
- Criar nova migration em `db/migration/` (ex.: `V2__descricao.sql`).
- Manter consistência com entidades JPA.

---

## Fluxo de decisão

Quando receber um pedido:

1.  Identifique o objetivo da tarefa.
2.  Determine quais entidades estão envolvidas.
3.  Escolha a skill apropriada.
4.  Gere a solução seguindo os padrões da arquitetura.

---

## Padrões de arquitetura

Sempre seguir:

Controller → Service → Repository → Database

Regras:

-   Controllers não possuem lógica de negócio
-   Services possuem regras de negócio
-   Repositories apenas acessam dados
-   Nunca expor entidades JPA diretamente

---

## Boas práticas obrigatórias

-   Usar DTOs para request e response
-   Usar Bean Validation
-   Criar migrations Flyway quando alterar schema
-   Criar testes unitários e de integração
-   Documentar endpoints com Swagger

# AGENTS.md — Tshirt-Store

Guia operacional para agentes de IA no projeto **Tshirt-Store**, alinhado ao Rheyder Method v1.2 e ao fluxo Superpowers no Cursor: entender contexto, planejar antes de editar, executar em fatias pequenas, validar com evidência e pedir aprovação humana nos pontos sensíveis.

## Visão Geral

- Produto: API REST de e-commerce de camisetas / loja de revenda.
- Fase atual: MVP backend em construção; frontend Next.js planejado.
- Stack atual: Java 21, Spring Boot 3.2, Maven, MySQL 8, Flyway, Spring Security JWT, springdoc-openapi, JUnit 5, MockMvc, Mockito, H2 em testes e Spotless.
- Fluxo de trabalho: GitFlow. Ver `docs/GITFLOW.md` e `.cursor/rules/gitflow.mdc`.
- Backlog: `docs/BACKLOG.md`.

## Triagem Rheyder

- `Hotfix`: produção quebrada/degradada. Fazer patch mínimo e validar o sintoma.
- `Complex`: alto risco, segurança sensível, integração externa, schema amplo, arquitetura ou custo alto de erro.
- `Normal`: regra de negócio, endpoint, segurança, documentação operacional, padronização ou ambiguidade moderada.
- `Simple`: mudança pequena, localizada, sem contrato público novo e sem dado sensível.

Suba a trilha se aparecer contrato novo, risco de segurança, mudança de schema não planejada ou escopo maior que o combinado.

## Comandos

| Objetivo | Comando |
| --- | --- |
| Subir MySQL local | `cd infra && docker compose up -d` |
| Rodar backend | `cd backend && mvn spring-boot:run` |
| Testes backend | `cd backend && mvn test` |
| Formatação | `cd backend && mvn spotless:check` |
| Aplicar formatação | `cd backend && mvn spotless:apply` |

Antes de rodar localmente, defina as variáveis do `.env.example`, especialmente `JWT_SECRET`, `MYSQL_ROOT_PASSWORD` e `MYSQL_PASSWORD`.

## Regras de Execução

- Nunca executar mudança sem confirmar o plano com o usuário.
- Tarefa não trivial exige Plan Mode, plano curto aprovado e execução em fatias verificáveis.
- Para backend, use TDD em bugfix, regra de negócio, segurança e endpoint: teste que falha, implementação mínima, teste passando.
- Se tocar schema, criar nova migration Flyway em `backend/src/main/resources/db/migration/`; não alterar migration já compartilhada.
- Se tocar contrato REST, DTO, segurança ou role, explicar impacto antes de editar e atualizar Swagger/testes.
- Evitar refactor amplo sem relação direta com a tarefa.

## Segurança

- Não commitar `.env`, secrets, tokens, chaves privadas, dumps de banco, exports sensíveis ou artefatos gerados.
- `backend/target/` não deve ser versionado.
- Segredos de JWT e banco devem vir de variáveis de ambiente ou `.env` local não versionado.
- Se segredo já foi publicado, trate como comprometido: remover do código atual não limpa histórico; limpeza de histórico só com aprovação explícita.

## Validação e Done

Antes de concluir, verificar:

- `mvn test`
- `mvn spotless:check`
- busca por padrões sensíveis excluindo `backend/target/`
- `git status --short` para confirmar escopo e arquivos gerados

Entrega final deve explicar o que mudou, o que foi verificado e riscos residuais. Nunca commitar, pushar ou abrir PR sem pedido explícito.

## Caminhos Importantes

| Caminho | Conteúdo |
| --- | --- |
| `backend/src/main/java/com/revenda/` | Código da API |
| `backend/src/main/resources/db/migration/` | Migrations Flyway |
| `backend/src/test/java/com/revenda/` | Testes |
| `backend/src/main/resources/application.yml` | Configuração por variáveis de ambiente |
| `infra/docker-compose.yml` | MySQL local |
| `.env.example` | Exemplo de variáveis locais sem segredo real |
| `.cursor/rules/` | Regras persistentes do Cursor |
| `.cursor/plans/` | Planos aprovados |
| `docs/` | Backlog, workflow, GitFlow, arquitetura e guias de IA |

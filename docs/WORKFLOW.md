# Workflow — Helder Method + Superpowers

## 0. Regra de ouro

Pequeno, testável, com CI verde e sem conteúdo sensível publicado.

## 1. Triar a tarefa

Antes de executar, classifique:

- `Hotfix`: produção quebrada/degradada; corrigir o mínimo e provar o sintoma.
- `Complex`: segurança sensível, arquitetura, integração, schema amplo ou alto custo de erro.
- `Normal`: endpoint, regra de negócio, documentação operacional, padronização ou ambiguidade moderada.
- `Simple`: mudança pequena, localizada, sem contrato público novo e sem dado sensível.

Se aparecer risco novo, suba a trilha e realinhe antes de continuar.

## 2. Escolher item e fechar contexto

- Abra `docs/BACKLOG.md`.
- Leia o item e critérios de aceite, quando houver.
- Consulte `AGENTS.md`, `.cursor/rules/` e `docs/ai/DEFINITION_OF_DONE.md`.
- Confirme contratos afetados: endpoint, DTO, schema, role, JWT, documentação ou workflow.

## 3. Planejar antes de editar

Para tarefa não trivial:

1. Use Plan Mode.
2. Faça brainstorming curto da solução.
3. Escreva um plano de 3-6 passos.
4. Liste edge cases e testes recomendados.
5. Aguarde aprovação humana antes de editar.

Planos relevantes devem ficar em `.cursor/plans/`.

## 4. Criar branch GitFlow

```bash
git checkout develop
git pull
git checkout -b feature/<codigo-ou-assunto>
```

Não commite, faça push ou abra PR sem pedido explícito.

## 5. Executar em fatias

- Faça uma fatia verificável por vez.
- Para backend, use TDD em endpoint, regra de negócio, bugfix e segurança:
  1. teste que falha;
  2. implementação mínima;
  3. teste passando;
  4. refatoração pequena se necessária.
- Se tocar schema, crie nova migration Flyway em `backend/src/main/resources/db/migration/`.
- Se tocar segurança/JWT, não hardcode segredos; use variáveis de ambiente.

## 6. Configuração local segura

1. Copie `.env.example` para `.env`.
2. Gere valores locais para `MYSQL_ROOT_PASSWORD`, `MYSQL_PASSWORD` e `JWT_SECRET`.
3. Suba o MySQL:

```bash
cd infra
docker compose --env-file ../.env up -d
```

4. Rode o backend com as variáveis do ambiente carregadas:

```bash
cd backend
mvn spring-boot:run
```

## 7. Validar localmente

Backend:

```bash
cd backend
mvn test
mvn spotless:check
```

Auditoria de sensíveis:

```bash
git status --short
```

Procure padrões como `password`, `secret`, `token`, `api_key`, `private key` e confirme que achados são placeholders ou variáveis, nunca segredo real.

## 8. Revisar antes de finalizar

Antes de considerar pronto:

- Confira o diff e escopo.
- Confirme que `backend/target/`, `.env`, dumps, tokens e chaves não estão versionados.
- Explique o que mudou, como foi verificado e riscos residuais.
- Para mudança relevante, abra PR com resumo, testes e riscos quando o usuário pedir.

## Dica

Padronize erros cedo com um `@ControllerAdvice` simples e reaproveite nos endpoints futuros, mas só faça isso quando entrar no backlog ou plano aprovado.


# Como executar no Cursor (resumo)

## Subir infra + backend + frontend
```bash
./scripts/up.sh
./scripts/backend-run.sh
./scripts/frontend-dev.sh
```

Acessos:
- Front: http://localhost:3000
- Swagger: http://localhost:8080/swagger-ui/index.html
- Health: http://localhost:8080/actuator/health

## Como usar a IA sem virar muleta
1) Escolha um item do backlog em `docs/BACKLOG.md`
2) Copie o prompt correspondente em `docs/ai/BACKLOG_PROMPTS.md`
3) Depois rode os testes:
```bash
./scripts/backend-test.sh
./scripts/frontend-lint.sh
```

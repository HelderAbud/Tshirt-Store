# Workflow (como trabalhar igual empresa)

## 0) Regra de ouro
Pequeno, testável, com CI verde.

## 1) Escolha uma tarefa
- Abra `docs/BACKLOG.md`
- Pegue a próxima tarefa (ex.: **B2-02**)
- Leia os critérios de aceite e dependências

## 2) Crie uma branch (GitFlow: use `feature/`)
```bash
git checkout develop
git pull
git checkout -b feature/B2-02-variants
```

## 3) Peça para o Cursor implementar (com o prompt certo)
- Abra `docs/ai/PROJECT_CONTEXT.md` e `docs/ai/DEFINITION_OF_DONE.md`
- Copie o prompt do item em `docs/ai/BACKLOG_PROMPTS.md`
- Cole no chat do Cursor e deixe ele gerar o incremento

## 4) Valide localmente
Backend:
```bash
./scripts/backend-test.sh
```
Frontend:
```bash
./scripts/frontend-lint.sh
```

## 5) PR (mesmo que seja no repo dele)
- Abra PR usando o template automático (já incluído)
- Preencha: o que mudou, como testar, riscos

## 6) Merge / finalizar
- CI verde
- Checklist DoD marcado
- Merge e pegue o próximo item
## Dica (evita retrabalho)
- Padronize erros cedo (um `@ControllerAdvice` simples) e reaproveite em todos endpoints.


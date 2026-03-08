# Start here (execução rápida)

## 1) Como usar este pacote
Copie o conteúdo deste pacote para a **raiz** do repositório do projeto (`loja-revenda/`).  
Ele adiciona:
- `docs/` (guia, backlog e playbook)
- `docs/ai/` (contexto e prompts pra IA)
- `.cursor/` + `.cursorrules` (regras do Cursor)
- `.github/` (templates de PR e Issue)
- `scripts/` (atalhos pra rodar local)

## 2) Subir tudo local (3 comandos)
```bash
./scripts/up.sh
./scripts/backend-run.sh
./scripts/frontend-dev.sh
```

## 3) Começar a executar o backlog
1. Abra `docs/BACKLOG.md`
2. Pegue o primeiro item não concluído (ex.: B0-01)
3. Siga `docs/WORKFLOW.md` (fluxo de branch/PR/validar)
4. Use os prompts prontos em `docs/ai/BACKLOG_PROMPTS.md`

## 4) Critério “está profissional”
Veja `docs/ai/DEFINITION_OF_DONE.md` (DoD).  
Se não passou no DoD, não está pronto.

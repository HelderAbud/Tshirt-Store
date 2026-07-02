# Definition of Done (DoD)

Uma tarefa só está pronta quando:

## Backend
- [ ] `./scripts/backend-test.sh` passa
- [ ] (se alterou schema) migration Flyway criada e aplicada
- [ ] Endpoint documentado no Swagger
- [ ] Erros padronizados e status corretos
- [ ] Pelo menos 1 teste relevante (unit ou integração)
- [ ] Fluxos críticos com Testcontainers (quando aplicável)

## Frontend
- [ ] `./scripts/frontend-lint.sh` passa (se existir)
- [ ] Estados: loading/empty/error tratados
- [ ] Integração com API com tratamento de erro

## Qualidade/Profissionalismo
- [ ] PR pequeno
- [ ] CI verde (quando configurado)
- [ ] Sem logar senha/token/PII

# Code Review Checklist (Backend)

- API: status HTTP correto, DTO + validação, erros padronizados
- Domínio: regras no service, transição de status de pedido válida
- JPA: evitar N+1, índices/constraints, transações em fluxos críticos
- Confiabilidade: idempotência (pagamento), estoque não pode ficar negativo
- Testes: unit + integração (Testcontainers) em fluxos críticos
- Segurança: roles corretas, sem dados sensíveis em logs

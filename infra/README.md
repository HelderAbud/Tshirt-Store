# Infra — desenvolvimento local

## MySQL (B0-02)

- **Subir:** `./scripts/up.sh` ou `cd infra && docker compose up -d`
- **Parar:** `./scripts/down.sh` ou `cd infra && docker compose down -v`

| Config        | Valor          |
|---------------|----------------|
| Porta         | 3306           |
| Banco         | `loja_revenda` |
| User (app)    | `app`          |
| Password (app)| `app`          |
| Root password | `root`         |

Volume `mysql_data` persiste os dados entre subidas.

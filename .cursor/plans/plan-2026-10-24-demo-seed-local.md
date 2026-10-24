# Plan — seed demo só no perfil local

**Data:** 2026-10-24  
**Status:** executada, sem commit  
**Trilha:** Normal

## Contrato

- A V4 não é editada.
- `V11` apaga `admin@demo.local` e `customer@demo.local`, e as linhas de carrinho, pedido e idempotência desses usuários.
- O perfil `local` recria os dois usuários com `Demo12345!` se eles não existirem.
- Sem esse perfil, o login desses emails responde 401.
- O painel e a vitrine não nascem com email nem senha preenchidos.
- O README diz que o seed é do perfil `local` e que o Flyway vai até a V11.

## Fora

- CNPJ e mapeamento de SKU da NF-e.
- Cookie httpOnly.
- Apagar `conversa_a_partir_meu_irmao_mandou_isso.md` do histórico.
- Restaurar `.env.example`, que está apagado só na pasta local.

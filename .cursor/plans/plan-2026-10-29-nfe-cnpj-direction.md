# Plano — direção da NF-e pelo CNPJ

**Data:** 2026-10-29  
**Status:** verificado em 2026-10-29. Sem commit.  
**Spec:** `docs/superpowers/specs/2026-10-29-nfe-cnpj-direction-design.md`

1. O parser devolve CNPJ do emitente, CNPJ do destinatário e `cProd`. `cProd` vazio responde `cProd is required`. `tpNF` não define direção.
2. `STORE_CNPJ` decide `ENTRADA` ou `SAIDA`. A V12 cria `supplier_sku_maps`. A entrada usa o mapa; a saída usa o `cProd` como SKU.
3. `POST` e `GET /api/admin/nfe/supplier-codes`. A página de NF-e ganha o formulário.
4. `mvn test` e `node --test` no frontend. Sem commit nesta sessão.

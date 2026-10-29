# Spec — Direção da NF-e pelo CNPJ

**Data:** 2026-10-29  
**Status:** aprovada  
**Trilha:** Normal

Substitui, na spec de 2026-10-18, a direção por `tpNF` e o vínculo `cProd` = SKU na entrada. O restante daquela spec continua valendo: chave de 44 dígitos, quantidade inteira, tudo ou nada, chave repetida, estoque insuficiente e o limite de 1 MB.

## Problem

`tpNF` é o ponto de vista de quem emitiu. Uma nota de compra chega com `tpNF` 1 e o estoque cairia. O `cProd` do fornecedor também não é o SKU da loja.

## Scope

- A loja tem um CNPJ, em `STORE_CNPJ`, com 14 dígitos. Não há cadastro de empresa.
- A direção compara esse CNPJ com o emitente e o destinatário do XML.
- Entrada resolve o item por uma tabela de mapeamento. Saída continua com `cProd` igual ao SKU.
- `POST` e `GET /api/admin/nfe/supplier-codes`, só ADMIN.
- A página de NF-e ganha o formulário do mapeamento: CNPJ, código e SKU.
- Migration nova `V12`. A `V10` não muda.

## Direção

Os CNPJs são só dígitos. `STORE_CNPJ` ausente, vazio ou com tamanho diferente de 14: **400**, `detail` = `Store CNPJ is not configured`. Nada é gravado.

- Destinatário é a loja e emitente não é: `ENTRADA`, delta positivo.
- Emitente é a loja e destinatário não é: `SAIDA`, delta negativo.
- Os dois são a loja, ou nenhum é: **400**, `detail` = `NF-e does not belong to the store`. Nada é gravado.

Um CNPJ ausente ou com tamanho diferente de 14 naquele lado significa que aquele lado não é a loja. Destinatário com CPF, por exemplo, não impede a saída quando o emitente é a loja.

`tpNF` não decide a direção.

## Itens

Uma transação só. Falha em uma linha desfaz a nota.

- `cProd` vazio: **400**, `detail` = `cProd is required`.
- Entrada: a chave do mapa é o CNPJ do emitente mais o `cProd`. Sem linha, **404**, `detail` = `Supplier code not mapped: {cProd}`. Nada é gravado.
- Saída: `cProd` igual ao SKU da variante. SKU desconhecido: **404**, `detail` = `Variant not found: {sku}`.
- O SKU do mapa, se a variante não existir mais: o mesmo **404** `Variant not found: {sku}`.
- Linhas que caem no mesmo SKU somam a quantidade antes de gravar.
- `qCom`, chave repetida, estoque insuficiente e arquivo acima de 1 MB seguem a spec de 2026-10-18.

## Mapa

Tabela `supplier_sku_maps`:

- `supplier_cnpj` VARCHAR(14), só dígitos
- `supplier_code` VARCHAR(60), o `cProd`
- `sku` VARCHAR(100), o SKU da variante
- único em (`supplier_cnpj`, `supplier_code`)

`POST /api/admin/nfe/supplier-codes` com `{ supplierCnpj, supplierCode, sku }`:

- CNPJ sem 14 dígitos, código vazio ou maior que 60: **400**, `detail` = `Validation failed`.
- SKU inexistente: **404**, `detail` = `Variant not found: {sku}`.
- Par CNPJ + código já mapeado: **409**, `detail` = `Supplier code already mapped`.
- Sucesso: **201** com `supplierCnpj`, `supplierCode` e `sku`.

`GET /api/admin/nfe/supplier-codes` devolve a lista desses três campos. CUSTOMER nos dois caminhos: **403**.

## Fora

- SEFAZ, certificado, assinatura, dígito verificador e protocolo
- Mais de uma loja ou tela de cadastro de empresa
- Dashboard, fuso e movimento de estoque no pagamento
- Editar a `V10` ou a spec de 2026-10-18 além do aviso de substituição

## Aceite

- XML em que o destinatário é a loja aumenta o estoque pelo SKU mapeado.
- XML em que o emitente é a loja diminui o estoque pelo `cProd`.
- CNPJ da loja nos dois lados, ou em nenhum, responde **400** e o estoque não muda.
- Entrada sem mapa responde **404** `Supplier code not mapped: {cProd}` e nada é gravado.
- `cProd` vazio responde **400** `cProd is required`.
- O mesmo `chNFe` na segunda chamada continua **409**, com o estoque da primeira.
- CUSTOMER em `POST /api/admin/nfe/supplier-codes` recebe **403**.
- Na página de NF-e, o formulário grava um mapa e a importação de entrada usa esse mapa.

## Riscos

- `STORE_CNPJ` errado faz toda importação cair em **400**.
- O mapa guarda o SKU em texto. Se o SKU da variante mudar, a entrada passa a responder **404**.
- A presença de `chNFe` continua sem provar que a nota foi autorizada.

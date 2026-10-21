# Spec — Importação de NF-e (F3-01)

**Data:** 2026-10-18  
**Status:** aprovada  
**Trilha:** Complex

## Problem

O estoque só muda por ajuste manual, venda de balcão ou pagamento do pedido. A loja não consegue aplicar uma NF-e já autorizada, de compra ou de venda, sem chamar a SEFAZ.

## Scope

- `POST /api/admin/nfe/import`, só ADMIN, campo multipart `file`.
- XML com no máximo 1 MB. Acima disso, ou sem arquivo, a API responde **400**.
- Parser pelos nomes locais dos elementos, independente do namespace.
- Tela NF-e no painel (`frontend/index.html` continua o mesmo arquivo; a navegação entra em `frontend/app.js`).
- Migration nova `nfe_imports`, sem editar a `V9`.
- Checkbox F3-01 em `docs/BACKLOG.md` só depois do fluxo verificado.

### Leitura

- `chNFe` com 44 dígitos. Ausente, vazio ou com outro tamanho: **400**. A API não confere assinatura nem consulta a SEFAZ.
- `tpNF` **0** é `ENTRADA` (delta positivo). **1** é `SAIDA` (delta negativo). Outro valor: **400**.
- Cada `det/prod`: `cProd` é o SKU da variante, comparação exata. `qCom` numérico e inteiro maior que zero. `10.0000` vale 10. `1.5`, zero, negativo ou texto: **400**.
- Linhas com o mesmo SKU somam a quantidade antes de gravar.
- XML sem nenhum item: **400**.

### Estoque

Uma transação só.

- SKU desconhecido: **404**, `detail` = `Variant not found: {sku}`. Nada é gravado.
- Saída que deixaria estoque negativo: **409**, `detail` = `Insufficient stock for {sku}`. Nada é gravado.
- Chave já importada: **409** com `detail` `NF-e already imported`. O estoque não muda.
- Sucesso: **201** com `accessKey`, `direction` (`ENTRADA` ou `SAIDA`) e `lines` (`sku`, `qty`, `delta`).
- Cada variante usa o ajuste já existente: tipo `ADJUSTMENT`, delta com sinal, motivo `NF-e {chNFe}`.

## Out of scope

- SEFAZ, certificado, emissão, cancelamento, carta de correção e DANFE
- Validar assinatura do XML
- A vitrine (`store.html` / `store.js`)
- Tipo novo de movimento de estoque
- Editar `V9`

## Acceptance criteria

- XML de entrada com dois itens, um deles repetido no SKU, soma a quantidade e aumenta o estoque uma vez por variante.
- XML de saída diminui o estoque.
- O mesmo `chNFe` na segunda chamada responde **409** e o estoque fica igual ao da primeira.
- SKU ausente responde **404** e o estoque de todas as variantes da nota fica inalterado.
- `qCom` fracionário responde **400** e nada é gravado.
- Saída maior que o estoque responde **409** e nada é gravado.
- CUSTOMER em `POST /api/admin/nfe/import` recebe **403**.
- No painel, o botão NF-e envia o arquivo e mostra a chave e a direção, ou o `detail` do erro.

## Decisions

- A direção vem só de `tpNF`.
- O vínculo do item é só `cProd` = SKU da variante.
- A chave vem do elemento `chNFe`, não do atributo `Id`.
- `10.0000` é inteiro. Qualquer fração real é rejeitada.
- Falha em uma linha desfaz a nota inteira.
- O movimento reusa `ADJUSTMENT`.

## Risks

- Dois fluxos já baixam estoque (pagamento do pedido e venda de balcão). Esta fatia soma um terceiro, o ajuste da NF-e.
- A presença de `chNFe` não prova que a nota foi autorizada.
- XML grande ou malformado precisa falhar sem gravar movimento parcial.

## Verification

- Teste do parser com um XML mínimo, sem dados reais de cliente.
- Teste do endpoint: entrada, saída, chave repetida, SKU ausente, quantidade fracionária, estoque insuficiente e **403** para CUSTOMER.
- `mvn test` e `mvn spotless:check`.
- Com a API no ar, no painel: escolher um XML de entrada, ver o estoque subir, enviar o mesmo arquivo de novo e ver o **409**.

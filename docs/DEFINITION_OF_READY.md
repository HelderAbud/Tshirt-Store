# Definition of Ready (DoR)

Antes de começar QUALQUER tarefa (issue, card, PR), preencha estes 5 pontos.  
Se você não consegue responder com clareza, a tarefa ainda está nebulosa: **refine antes de codar**.

---

## 1) Objetivo

**Pergunta que isso responde:** *“Por que essa tarefa existe?”*

- **Resumo em 1–2 frases**, sempre começando com verbo:
  - **Exemplo (cadastro de produto)**:  
    `Permitir que o admin cadastre novas camisetas na loja, com nome, descrição, preço e estoque.`
  - **Exemplo (carrinho)**:  
    `Permitir que o cliente adicione/remova camisetas no carrinho e veja o total atualizado.`

- **Conexão com o projeto maior (opcional, mas recomendado):**
  - `Essa tarefa faz parte do MVP do e‑commerce de camisetas para minha transição de carreira.`

Checklist rápido:

- [ ] Objetivo cabe em 1–2 frases  
- [ ] Explica claramente o “por quê” (não só o “o quê”)  
- [ ] Está alinhado com o MVP da loja (produtos, carrinho, pagamento, etc.)

---

## 2) Endpoints/telas afetados

**Pergunta que isso responde:** *“O que vai mudar na interface e na API?”*

- **Liste endpoints REST / rotas de API** envolvidos:
  - `POST /products`
  - `GET /products`
  - `POST /cart/add`
  - `GET /cart`
  - `POST /auth/login`

- **Liste telas do frontend** (se aplicável):
  - `Página de listagem de produtos`
  - `Página do carrinho`
  - `Tela de login/registro`

- **Indique se é:**
  - **Novo endpoint/tela**
  - **Alteração em algo existente**
  - **Remoção/depreciação**

Checklist rápido:

- [ ] Todos os endpoints afetados listados  
- [ ] Todas as telas afetadas listadas  
- [ ] Claridade se é criação, alteração ou remoção

---

## 3) Banco: muda schema?

**Pergunta que isso responde:** *“Vou precisar mudar tabelas/colunas do banco?”*

- **Responda SIM ou NÃO**:
  - `Muda schema? SIM` / `Muda schema? NÃO`

- Se **SIM**, detalhe:
  - **Novas tabelas**:
    - `product (id, name, description, price, stock, created_at)`
    - `cart_item (id, user_id, product_id, quantity, created_at)`
  - **Novas colunas em tabelas existentes**:
    - `order.total_amount (numeric)`
    - `user.role (varchar)`
  - **Alterações perigosas**:
    - `alter table product drop column ...`
    - `alter type`, `rename column`, etc.

- Se houver **migração** (Flyway/Liquibase/SQL manual), descreva:
  - Onde o script vai ficar  
  - Como será aplicado (local e produção)

Checklist rápido:

- [ ] Especificado SIM/NÃO para mudança de schema  
- [ ] Tabelas/colunas novas ou alteradas descritas  
- [ ] Riscos de migração considerados (dados existentes, downtime, rollback)

---

## 4) Riscos

**Pergunta que isso responde:** *“O que pode dar ruim se eu errar nessa tarefa?”*

Liste **de 2 a 5 riscos**, focando em e‑commerce de camisetas:

- **Transação / Concorrência**
  - `Dois usuários comprando a última peça ao mesmo tempo podem gerar estoque negativo.`
- **Estoque / Dados críticos**
  - `Atualização errada de stock pode mostrar produto disponível quando acabou.`
- **Performance / N+1**
  - `Listagem de produtos com join em várias tabelas pode gerar N+1 e ficar lenta.`
- **Segurança / Autenticação**
  - `Endpoint de criação de produto sem verificar se o usuário é admin.`
  - `Exposição de dados sensíveis no payload ou nos logs.`
- **Outros**
  - `Erros de UX que confundem o usuário (por ex: carrinho não atualiza total).`

Checklist rápido:

- [ ] Pelo menos 1 risco de **dados/estoque**  
- [ ] Pelo menos 1 risco de **segurança ou autorização**  
- [ ] Pelo menos 1 risco de **performance/experiência do usuário**  
- [ ] Riscos estão ligados diretamente ao objetivo da tarefa

---

## 5) Como testar

**Pergunta que isso responde:** *“Como eu sei que isso funciona e não quebrou nada?”*

- **Teste manual (frontend + backend):**
  - [ ] **Fluxo feliz** (cenário principal)  
    - Ex.: criar produto, listar, adicionar ao carrinho, finalizar pedido.
  - [ ] **Fluxos de erro/validação**  
    - Ex.: tentar criar produto sem nome, sem preço, com estoque negativo.
  - [ ] **Cenários de borda**  
    - Ex.: carrinho vazio, estoque 1, usuário não logado tentando acessar rota protegida.

- **Testes automatizados esperados:**
  - [ ] **Unitários (Service, regras de negócio)**  
    - Ex.: cálculo de total do carrinho, regras de desconto, atualizações de estoque.
  - [ ] **Integração (API + banco)**  
    - Ex.: teste de `POST /products` gravando no PostgreSQL; `GET /products` retornando lista correta.
  - [ ] **E2E (se aplicável)**  
    - Ex.: fluxo completo no navegador: login → escolher produto → adicionar ao carrinho → fechar pedido.

- **Critérios de aceite (exemplo para cadastro de produto):**
  - [ ] Consigo criar um produto válido e ele aparece na listagem.  
  - [ ] Não consigo criar produto com dados inválidos (validações no backend).  
  - [ ] Endpoint protegido: apenas usuário admin consegue criar produto.  
  - [ ] Logs e erros não expõem informações sensíveis.

---

> ⚠️ Regra de ouro: **se algum desses 5 pontos não estiver claro, não comece a implementar.**  
> Refine a tarefa, faça anotações e ajuste o objetivo/endpoints antes de abrir o editor.
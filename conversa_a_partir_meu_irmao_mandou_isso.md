## Usuário
**Mensagem:**  
meu irmão mandou isso:  
"tava pensando em fazer um projeto em java pra uma joja virtual pra revender roupa... fazer pro meu pai igual ele queria so colocar etiqueta dele e revender ja que ele tem o canal e ja e a oportunidade de fazer um projeto real ne"

me diga uma stack pra ele usar (java based)

---

## Assistente
Pra ele fazer uma **loja virtual de revenda de roupas** e ainda ficar **rápido + profissional**, eu indicaria uma stack **Java/Spring monolítica (bem modular)** no começo. Microserviços só depois (se um dia fizer sentido).

### Stack recomendada (Java-based, “padrão mercado”, sem complicar)

**Linguagem e build**
- **Java 21** (ou 17 se preferirem LTS mais comum)
- **Maven** (mais simples de achar exemplo e suporte) ou Gradle (se vocês já usam)

**Backend (API)**
- **Spring Boot 3**
- **Spring Web** (REST)
- **Spring Validation** (Bean Validation)
- **Spring Data JPA + Hibernate**
- **Flyway** (migração de banco)
- **Spring Security** (login + permissões)
  - Começa com **JWT** (ou sessão, se for bem simples)
- **springdoc-openapi** (Swagger UI)

**Banco e infraestrutura local**
- **PostgreSQL**
- **Docker + docker-compose** (app + banco)

**Testes**
- **JUnit 5**
- **Mockito**
- **Testcontainers** (integração com Postgres de verdade)

**Observabilidade**
- **Spring Boot Actuator**
- **Micrometer**

**Qualidade/CI**
- **GitHub Actions**
- Opcional: **Spotless/Checkstyle**

**Extras (só quando precisar)**
- **Redis**, mensageria, busca, etc (depois)
- **Front-end**: começar com Swagger; depois UI (Thymeleaf ou React/Next)

---

## Usuário
Java 21, Maven, n seria melhor MySQL ao invés de Postgre?  
Qualidade/CI sem o opcional  
Extras só com front and react/next...

Crie um arquivo .md completo com toda a stack e um backlog inicial pra dar total direcionamento pra ele

---

## Assistente
Criei um arquivo `.md` completo com stack (Java 21 + Maven + **MySQL**) e backlog inicial.  
**Arquivo entregue:** `stack_e_backlog_loja_revenda.md`

---

## Usuário
gere um pequeno trecho dizendo como ele pode executar isso usando o cursor

---

## Assistente
Forneci um trecho em Markdown para colar no README, explicando como rodar **infra (docker compose)**, **backend (mvnw)** e **frontend (npm)** pelo terminal do Cursor, incluindo URLs de acesso e um prompt padrão de uso da IA.

---

## Usuário
quero, crie como um arquivo .md

---

## Assistente
Criei o arquivo: `como_executar_no_cursor.md`

---

## Usuário
algum setup IA inicial pra adicionar no cursor pra este projeto?

---

## Assistente
Sugeri um setup inicial leve (sem virar muleta):
- `docs/ai/PROJECT_CONTEXT.md` (contexto fixo do projeto)
- `docs/ai/DEFINITION_OF_DONE.md` (DoD)
- `docs/ai/PROMPTS.md` (prompts curinga)
- `docs/ai/CODE_REVIEW_CHECKLIST.md` (checklist de review)

Expliquei como usar no Cursor (referenciar os arquivos e pedir incrementalmente).

---

## Usuário
quero sim, gere também o cursorrules para já utilizar o setup da forma correta

---

## Assistente
Gerei um pacote com:
- `docs/ai/*.md`
- `.cursorrules` (legacy)
- `.cursor/index.mdc` + `.cursor/rules/backend.mdc` + `.cursor/rules/frontend.mdc`
- `docs/ai/README.md` (como usar)

---

## Usuário
já deixe tudo integrado, faça melhorias se necessário para ele já conseguir ir executando

---

## Assistente
Gerei uma versão integrada contendo:
- `docs/START_HERE.md`, `docs/WORKFLOW.md`, `docs/BACKLOG.md`, `docs/RUN_CURSOR.md`
- `docs/ai/BACKLOG_PROMPTS.md` com prompts por item
- `.github/` com templates de PR/Issue
- `scripts/` com atalhos (`up.sh`, `backend-run.sh`, `frontend-dev.sh`, etc.)

---

## Usuário
eu quero deixar ele se virar um pouco também... quero um setup minimalista mas bastante funcional que ajude ele mas que também faça ele quebrar a cabeça pra aprender algumas coisas

---

## Assistente
Sugeri manter o setup existente, mas aplicar um “modo aprendizado”:
- Guardrails para evitar “feature gigante”
- Pedir plano + edge cases + testes antes de codar
- Trabalhar em incrementos (um passo por vez)
- Manter validação (testes/migrations) como rotina

---

## Usuário
vou usar o setup que já foi criado mesmo, há alguma forma de melhorá-lo?

---

## Assistente
Propus melhorias objetivas:
- reforçar guardrails no `.cursor/index.mdc`
- incentivar prompts genéricos (em vez de piloto automático por tarefa)
- ajustar cobertura JaCoCo por fase
- melhorar template de PR (trade-offs + escopo)
- adicionar Definition of Ready (DoR)
- criar `scripts/verify.sh`
- padronizar erros cedo
- registrar decisões em ADRs

---

## Usuário
quero sim, crie um README pra explicar todo o setup e como ele pode utilizar

---

## Assistente
Gerei uma versão melhorada do setup, adicionando:
- guardrails anti-muleta no Cursor rules
- `docs/DEFINITION_OF_READY.md`
- `scripts/verify.sh`
- melhorias no template de PR
- pasta `docs/adr/` com template
- **README_SETUP.md** explicando como usar tudo

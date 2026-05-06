# bankjure

Ledger financeiro imutável em **Clojure** com **Datomic Local** e API HTTP. Frontend em **React**, **TypeScript** e **Vite** (tipografia Literata + Atkinson Hyperlegible, tema papel quente), shell com métricas do **Yoga Layout** WASM.

## Requisitos

- Clojure CLI (`clojure`)
- Node.js 20+ e npm (para `web/`)
- Babashka (`bb`) opcional para atalhos em `bb.edn`

## API (Ring + Jetty)

Na raiz do repositório:

```bash
clojure -M:api
```

Servidor em `http://127.0.0.1:8080`.

### Rotas

| Método | Caminho | Descrição |
|--------|---------|-----------|
| GET | `/api/health` | Saúde |
| POST | `/api/accounts` | Cria conta (`{"owner":"..."}`) |
| GET | `/api/accounts/:uuid` | Conta, saldo atual e `transactions` |
| GET | `/api/accounts/:uuid/transactions` | Só lançamentos |
| GET | `/api/accounts/:uuid/balance-as-of?at=` | Saldo em instante ISO-8601 (ex.: `2026-05-06T15:00:00Z`). Use um instante **igual ou anterior** ao último commit visível no banco; instante “no futuro” em relação ao Datomic pode responder com erro. |
| POST | `/api/accounts/:uuid/transactions` | Depósito ou saque (`{"kind":"deposit"\|"withdraw","amount":...}`) |

Corpos JSON são validados com **Malli**; erros 400 retornam `details` legível.

Respostas HTTP **≥ 400** também geram uma **linha JSON** no stderr (`:ts`, `:evt`, etc.).

### JWT opcional (mutações)

Se a variável de ambiente **`BANKJURE_JWT_SECRET`** estiver definida (string não vazia), todo **POST**, **PUT**, **PATCH** e **DELETE** exige cabeçalho `Authorization: Bearer <jwt>`. O token deve ser assinado com **HS256** e a mesma secret (**buddy.sign.jwt**). **GET**, **HEAD** e **OPTIONS** não exigem token.

Exemplo de geração (Clojure REPL), apenas para testes:

```clojure
(require '[buddy.sign.jwt :as jwt])
(jwt/sign {:sub "demo"} "sua-secret-aqui" {:alg :hs256})
```

Na UI (`web/`), campo **JWT da API (opcional)** grava o token em `localStorage` (`bankjure.apiToken`) e envia `Authorization` nos POST.

### Uberjar

```bash
clojure -T:build uber
java -jar target/core-0.1.0-SNAPSHOT-standalone.jar
```

O nome do JAR segue o símbolo `bankjure/core` em `build.clj`.

### Atalhos Babashka

Com `bb` no PATH: `bb run api`, `bb run test`, `bb run uber` (veja `bb.edn`).

Dados locais do Datomic em `data/` (ver `.gitignore`). Contas e transações **persistem em disco** enquanto essa pasta existir. O cálculo de saldo usa agregação Datalog com `:with ?tx` para que vários lançamentos com o mesmo valor (ex.: dois depósitos de 50) entrem todos na soma.

### Última conta no navegador

A UI grava o UUID da conta em `localStorage` (`bankjure.lastAccountId`) para, ao recarregar, buscar de novo saldo e movimentos na API. Use **Esquecer esta conta** na barra lateral para limpar. Em outro computador ou após apagar `data/`, a conta deixa de existir no backend.

### Ver dados como no DBeaver

O Datomic **não é um banco SQL** acessível pelo DBeaver da mesma forma que PostgreSQL/MySQL. O “explorer” é a **API do Datomic** (Client/Peer): consultas em **Datalog** e histórico temporal.

Para uma visão rápida no terminal (titular, id, saldo):

```bash
clojure -M -e "(load-file \"print_accounts.clj\")"
```

No REPL você pode abrir a conexão com `(require '[ledger.core :as c] '[ledger.db :as db] '[datomic.client.api :as d])` e usar `(db/list-all-accounts (d/db (c/connect)))`, `db/list-transactions`, etc.

## Frontend

Em outro terminal:

```bash
cd web
npm install
npm run dev
```

O Vite encaminha `/api` para o backend em `8080` durante o desenvolvimento e o mesmo proxy vale para `npm run preview`. **Sem o Jetty em `8080`**, o proxy costuma responder **502**. Se aparecer **404** com “(from service worker)” no Chrome, em DevTools → **Aplicativo** → **Service workers** desregistre workers desse origin (ou desative “Offline” na aba Rede) e recarregue; um service worker pode devolver 404 sem ir ao Vite. JSON `{"error":"account not found"}` é 404 da API (conta inexistente no Datomic); `{"error":"not found"}` é rota não reconhecida.

Em produção, sirva `web/dist/` atrás de um proxy que aponte `/api` para o Jetty.

Build:

```bash
cd web && npm run build
```

Saída em `web/dist/`.

## Testes (Clojure)

```bash
clojure -M:test
```

Inclui testes de **`clojure.test.check`** sobre `ledger.logic` e integração com Datomic Local em diretório temporário.

## Yoga Layout

O site do Yoga exemplifica componentes `<Layout>` / `<Node>`; no npm, `yoga-layout` expõe a API imperativa. O `YogaDashboardLayout` monta a mesma árvore no WASM, chama `calculateLayout` e usa a **largura da sidebar** e a **altura do header** retornadas pelo Yoga como `flex-basis` no DOM; o restante do shell é **flexbox** no navegador (`minWidth: 0` / `minHeight: 0` para o scroll não vazar).

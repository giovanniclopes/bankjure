# bankjure

Ledger financeiro imutável em **Clojure** com **Datomic Local** e API HTTP. Frontend em **React**, **TypeScript** e **Vite** (tipografia Literata + Atkinson Hyperlegible, tema papel quente), shell com métricas do **Yoga Layout** WASM.

## Requisitos

- Clojure CLI (`clojure`)
- Node.js 20+ e npm (para `web/`)

## API (Ring + Jetty)

Na raiz do repositório:

```bash
clojure -M:api
```

Servidor em `http://127.0.0.1:8080`. Rotas: `GET /api/health`, `POST /api/accounts`, `GET /api/accounts/:uuid` (resposta inclui `balance` e `transactions`), `GET|POST /api/accounts/:uuid/transactions` (lista só de lançamentos ou POST de depósito/saque).

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

O Vite encaminha `/api` para o backend em `8080` durante o desenvolvimento. Em produção, sirva `web/dist/` atrás de um proxy que aponte `/api` para o Jetty.

Build:

```bash
cd web && npm run build
```

Saída em `web/dist/`.

## Testes (Clojure)

```bash
clojure -M:test
```

## Yoga Layout

O site do Yoga exemplifica componentes `<Layout>` / `<Node>`; no npm, `yoga-layout` expõe a API imperativa. O `YogaDashboardLayout` monta a mesma árvore no WASM, chama `calculateLayout` e usa a **largura da sidebar** e a **altura do header** retornadas pelo Yoga como `flex-basis` no DOM; o restante do shell é **flexbox** no navegador (`minWidth: 0` / `minHeight: 0` para o scroll não vazar).

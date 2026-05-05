# bankjure

Ledger financeiro imutável em **Clojure** com **Datomic Local** e API HTTP. Frontend em **React**, **TypeScript** e **Vite**, com shell de layout calculado por [**Yoga Layout**](https://www.yogalayout.dev/) (WASM via pacote `yoga-layout`).

## Requisitos

- Clojure CLI (`clojure`)
- Node.js 20+ e npm (para `web/`)

## API (Ring + Jetty)

Na raiz do repositório:

```bash
clojure -M:api
```

Servidor em `http://127.0.0.1:8080`. Rotas: `GET /api/health`, `POST /api/accounts`, `GET /api/accounts/:uuid`, `GET|POST /api/accounts/:uuid/transactions`.

Dados locais do Datomic em `data/` (ver `.gitignore`).

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

O site do Yoga exemplifica componentes `<Layout>` / `<Node>`; no npm, `yoga-layout` expõe a API imperativa. O componente `web/src/YogaDashboardLayout.tsx` monta a árvore (linha + coluna), chama `calculateLayout` e aplica `left` / `top` / `width` / `height` como estilo absoluto nos painéis.

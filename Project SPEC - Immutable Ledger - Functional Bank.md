## 1. Objetivo

Desenvolver um motor de contabilidade financeira imutável que gerencie contas, processe transações e permita auditoria histórica ("viagem no tempo") utilizando os pilares da programação funcional.

## 2. Stack Tecnológica

- Linguagem: Clojure 1.11+
- Banco de Dados: Datomic Local (dev-local)
- Gerenciamento de Dependências: Clojure CLI (`deps.edn`)
- Ambiente de Desenvolvimento: VS Code + Calva (ou IntelliJ + Cursive)

## 3. Modelo de Dados (Datomic Schema)

O esquema deve ser focado em fatos atômicos.

- Entidade `Account`
    
    - `:account/id`: UUID ou String (Unique)
    - `:account/owner`: String
    
- Entidade `Transaction`
    
    - `:tx/account`: Referência para conta
    - `:tx/type`: Enum (`:tx.type/deposit`, `:tx.type/withdraw`)
    - `:tx/amount`: BigDec ou Double
    - `:db/txInstant`: (Automático do Datomic) Data/hora do fato.
    

## 4. Requisitos Funcionais (Core)

## RF01: Criação de Contas

Função pura que gera a transação de criação de uma nova conta no Datomic.

- Input: Nome do proprietário.
- Output: Mapa de transação para o Datomic.

## RF02: Processamento de Transações (Regras de Negócio)

Função que valida se um saque pode ser efetuado.

- Regra: O saldo não pode ficar negativo.
- Abordagem: Deve consultar o saldo atual no banco antes de emitir o comando de escrita.

## RF03: Cálculo de Saldo (O "Reduce")

Função que soma todos os créditos e subtrai os débitos de uma conta específica.

- Tecnologia: Query Datalog.

## RF04: Auditoria "As-Of" (Viagem no Tempo)

Função que recebe um `account-id` e uma `data/hora` e retorna o saldo exato naquele momento específico.

## 5. Arquitetura de Código Sugerida

```bash
/src
  /ledger
    core.clj      # Inicialização do DB e conexão
    schema.clj    # Definição dos atributos Datomic
    db.clj        # Queries (Datalog) e transações
    logic.clj     # Funções puras de lógica (cálculos de juros, validações)
/test
  /ledger
    logic_test.clj # Testes unitários das funções puras
```

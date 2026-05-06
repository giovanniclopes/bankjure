import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type FormEvent,
} from 'react'
import { YogaDashboardLayout } from './YogaDashboardLayout'
import {
  clearStoredAccountId,
  readStoredAccountId,
  writeStoredAccountId,
} from './accountStorage'
import {
  createAccount,
  getAccount,
  postTransaction,
  type AccountRes,
  type TxWire,
} from './api'

function kindLabel(kind: string): string {
  if (kind === 'deposit' || kind === 'tx.type/deposit') return 'Depósito'
  if (kind === 'withdraw' || kind === 'tx.type/withdraw') return 'Saque'
  return kind
}

function kindClass(kind: string): string {
  const k = kind.toLowerCase()
  if (k.includes('deposit')) return 'deposit'
  if (k.includes('withdraw')) return 'withdraw'
  return ''
}

export default function App() {
  const [ownerInput, setOwnerInput] = useState('')
  const [accountId, setAccountId] = useState<string | null>(null)
  const [account, setAccount] = useState<AccountRes | null>(null)
  const [txList, setTxList] = useState<TxWire[]>([])
  const [amount, setAmount] = useState('50')
  const [msg, setMsg] = useState<string | null>(null)
  const refreshGen = useRef(0)
  const refreshAbortRef = useRef<AbortController | null>(null)

  const refresh = useCallback(async (id: string) => {
    refreshAbortRef.current?.abort()
    const ctrl = new AbortController()
    refreshAbortRef.current = ctrl
    const { signal } = ctrl
    const gen = ++refreshGen.current

    try {
      const acc = await getAccount(id, { signal })
      if (gen !== refreshGen.current) return
      setAccount(acc)
      if (acc.ok) setTxList(acc.transactions ?? [])
      else setTxList([])
    } catch (e) {
      if (e instanceof DOMException && e.name === 'AbortError') return
      return
    }
  }, [])

  useEffect(() => {
    const id = readStoredAccountId()
    if (!id) return
    setAccountId(id)
    void refresh(id)
    return () => {
      refreshAbortRef.current?.abort()
    }
  }, [refresh])

  useEffect(() => {
    if (accountId) writeStoredAccountId(accountId)
  }, [accountId])

  useEffect(() => {
    if (!accountId || account == null) return
    if (account.ok !== false) return
    clearStoredAccountId()
    setAccountId(null)
    setAccount(null)
    setTxList([])
    setMsg('Conta não encontrada no servidor (dados apagados ou outro ambiente).')
  }, [accountId, account])

  const forgetAccount = () => {
    clearStoredAccountId()
    setAccountId(null)
    setAccount(null)
    setTxList([])
    setMsg(null)
  }

  const onCreateAccount = async (e: FormEvent) => {
    e.preventDefault()
    setMsg(null)
    const owner = ownerInput.trim()
    if (!owner) {
      setMsg('Informe o titular.')
      return
    }
    const res = await createAccount(owner)
    if (!res.ok || !res.accountId) {
      setMsg(res.error ?? 'Falha ao criar conta.')
      return
    }
    setAccountId(res.accountId)
    await refresh(res.accountId)
  }

  const onDeposit = async (e: FormEvent) => {
    e.preventDefault()
    if (!accountId) return
    setMsg(null)
    const n = Number(amount)
    if (!Number.isFinite(n) || n <= 0) {
      setMsg('Valor inválido.')
      return
    }
    const res = await postTransaction(accountId, 'deposit', n)
    if (!res.ok) {
      setMsg(res.error ?? 'Depósito recusado.')
      return
    }
    await refresh(accountId)
  }

  const onWithdraw = async (e: FormEvent) => {
    e.preventDefault()
    if (!accountId) return
    setMsg(null)
    const n = Number(amount)
    if (!Number.isFinite(n) || n <= 0) {
      setMsg('Valor inválido.')
      return
    }
    const res = await postTransaction(accountId, 'withdraw', n)
    if (!res.ok) {
      setMsg(
        res.error
          ? `${res.error}${res.balance != null ? ` (saldo ${res.balance})` : ''}`
          : 'Saque recusado.',
      )
      return
    }
    await refresh(accountId)
  }

  return (
    <YogaDashboardLayout
      sidebar={
        <div className="panel pad">
          <div className="side-lede">
            <div className="brand">bankjure</div>
            <span className="brand-tag">Registro imutável</span>
          </div>
          <p className="hint">
            Contas e lançamentos ficam no Datomic em disco; esta página lembra
            a última conta neste aparelho.
          </p>
          <form className="stack" onSubmit={onCreateAccount}>
            <label className="lbl" htmlFor="owner">
              Titular
            </label>
            <input
              id="owner"
              className="inp"
              value={ownerInput}
              onChange={(e) => setOwnerInput(e.target.value)}
              placeholder="Nome completo"
              autoComplete="name"
            />
            <button className="btn primary" type="submit">
              Abrir conta
            </button>
          </form>
          {accountId ? (
            <>
              <p className="mono">
                <span className="muted">Identificador · </span>
                {accountId}
              </p>
              <button
                type="button"
                className="btn btn-ghost"
                onClick={forgetAccount}
              >
                Esquecer esta conta
              </button>
            </>
          ) : null}
        </div>
      }
      header={
        <div className="header-bar">
          <h1 className="title">Livro-razão</h1>
          {account?.ok ? (
            <div className="balance-block">
              <span className="balance-label">Saldo</span>
              <span className="bal">{account.balance ?? '—'}</span>
              {account.owner ? (
                <span className="balance-owner">{account.owner}</span>
              ) : null}
            </div>
          ) : null}
        </div>
      }
      main={
        <div className="main-scroll">
          {msg ? <div className="banner">{msg}</div> : null}
          {!accountId ? (
            <div className="card">
              <p className="empty-title">Nenhuma conta selecionada</p>
              <p className="muted empty-text">
                Abra uma conta à esquerda. Depois você registra depósitos e
                saques; o histórico permanece consultável.
              </p>
            </div>
          ) : null}
          {accountId ? (
            <>
              <div className="card actions">
                <form className="inline" onSubmit={onDeposit}>
                  <label className="lbl inline" htmlFor="amount">
                    Valor
                  </label>
                  <input
                    id="amount"
                    className="inp narrow"
                    inputMode="decimal"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    aria-label="Valor da movimentação"
                  />
                  <button className="btn primary" type="submit">
                    Depositar
                  </button>
                </form>
                <form className="inline" onSubmit={onWithdraw}>
                  <button className="btn" type="submit">
                    Sacar este valor
                  </button>
                </form>
              </div>
              <div className="card">
                <h2 className="h2">Movimentação</h2>
                <div className="tbl-wrap">
                  <table className="tbl">
                    <thead>
                      <tr>
                        <th>Tipo</th>
                        <th className="num">Valor</th>
                        <th>Instante</th>
                      </tr>
                    </thead>
                    <tbody>
                      {txList.length === 0 ? (
                        <tr>
                          <td colSpan={3} className="muted">
                            Sem lançamentos.
                          </td>
                        </tr>
                      ) : null}
                      {txList.map((t, i) => {
                        const kc = kindClass(t.kind)
                        return (
                          <tr key={`${t.at ?? ''}-${i}`}>
                            <td>
                              <span className={`kind-pill ${kc}`.trim()}>
                                {kindLabel(t.kind)}
                              </span>
                            </td>
                            <td className="num">{t.amount}</td>
                            <td className="cell-time mono">
                              {t.at ?? '—'}
                            </td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            </>
          ) : null}
        </div>
      }
    />
  )
}

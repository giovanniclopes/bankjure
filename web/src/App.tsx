import { useCallback, useState, type FormEvent } from 'react'
import {
  createAccount,
  getAccount,
  listTransactions,
  postTransaction,
  type AccountRes,
  type TxWire,
} from './api'
import { YogaDashboardLayout } from './YogaDashboardLayout'
import './App.css'

export default function App() {
  const [ownerInput, setOwnerInput] = useState('')
  const [accountId, setAccountId] = useState<string | null>(null)
  const [account, setAccount] = useState<AccountRes | null>(null)
  const [txList, setTxList] = useState<TxWire[]>([])
  const [amount, setAmount] = useState('50')
  const [msg, setMsg] = useState<string | null>(null)

  const refresh = useCallback(async (id: string) => {
    const [acc, txs] = await Promise.all([getAccount(id), listTransactions(id)])
    setAccount(acc)
    setTxList(txs.transactions ?? [])
  }, [])

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
          <div className="brand">bankjure</div>
          <p className="hint">Shell calculado com Yoga Layout (WASM).</p>
          <form className="stack" onSubmit={onCreateAccount}>
            <label className="lbl">
              Nova conta — titular
              <input
                className="inp"
                value={ownerInput}
                onChange={(e) => setOwnerInput(e.target.value)}
                placeholder="Ex.: Maria"
              />
            </label>
            <button className="btn primary" type="submit">
              Criar conta
            </button>
          </form>
          {accountId && (
            <p className="mono small">
              ID
              <br />
              {accountId}
            </p>
          )}
        </div>
      }
      header={
        <div className="header-bar">
          <h1 className="title">Ledger imutável</h1>
          {account?.ok && (
            <div className="balance-pill">
              <span className="muted">Saldo</span>
              <strong className="bal">{account.balance ?? '—'}</strong>
              <span className="muted small pad-l">
                {account.owner ? ` · ${account.owner}` : null}
              </span>
            </div>
          )}
        </div>
      }
      main={
        <div className="panel pad main-inner">
          {msg && <div className="banner">{msg}</div>}
          {!accountId && (
            <p className="muted">Crie uma conta na barra lateral para começar.</p>
          )}
          {accountId && (
            <>
              <div className="actions card">
                <form className="inline" onSubmit={onDeposit}>
                  <label className="lbl inline">
                    Valor
                    <input
                      className="inp narrow"
                      inputMode="decimal"
                      value={amount}
                      onChange={(e) => setAmount(e.target.value)}
                    />
                  </label>
                  <button className="btn primary" type="submit">
                    Depositar
                  </button>
                </form>
                <form className="inline" onSubmit={onWithdraw}>
                  <button className="btn" type="submit">
                    Sacar mesmo valor
                  </button>
                </form>
              </div>
              <div className="card">
                <h2 className="h2">Transações</h2>
                <table className="tbl">
                  <thead>
                    <tr>
                      <th>Tipo</th>
                      <th className="num">Valor</th>
                      <th>Quando</th>
                    </tr>
                  </thead>
                  <tbody>
                    {txList.length === 0 && (
                      <tr>
                        <td colSpan={3} className="muted">
                          Nenhuma movimentação.
                        </td>
                      </tr>
                    )}
                    {txList.map((t, i) => (
                      <tr key={`${t.at ?? ''}-${i}`}>
                        <td>{t.kind}</td>
                        <td className="num">{t.amount}</td>
                        <td className="mono small">{t.at ?? '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </div>
      }
    />
  )
}

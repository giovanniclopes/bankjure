async function readJson<T>(res: Response): Promise<T> {
  return (await res.json()) as T
}

export type CreateAccountRes = {
  ok: boolean
  accountId?: string
  owner?: string
  error?: string
}

export async function createAccount(owner: string): Promise<CreateAccountRes> {
  const res = await fetch('/api/accounts', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ owner }),
  })
  return readJson(res)
}

export type TxWire = { kind: string; amount: string; at?: string }

export type AccountRes = {
  ok: boolean
  accountId?: string
  owner?: string
  balance?: string
  transactions?: TxWire[]
  error?: string
}

export async function getAccount(
  accountId: string,
  init?: RequestInit,
): Promise<AccountRes> {
  const q = `_=${Date.now()}`
  const res = await fetch(`/api/accounts/${accountId}?${q}`, {
    cache: 'no-store',
    ...init,
  })
  return readJson(res)
}

export type ListTxRes = {
  ok: boolean
  transactions?: TxWire[]
  error?: string
}

export async function listTransactions(
  accountId: string,
  init?: RequestInit,
): Promise<ListTxRes> {
  const q = `_=${Date.now()}`
  const res = await fetch(`/api/accounts/${accountId}/transactions?${q}`, {
    cache: 'no-store',
    ...init,
  })
  return readJson(res)
}

export type PostTxRes = { ok: boolean; error?: string; balance?: string }

export async function postTransaction(
  accountId: string,
  kind: 'deposit' | 'withdraw',
  amount: number,
): Promise<PostTxRes> {
  const res = await fetch(`/api/accounts/${accountId}/transactions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ kind, amount }),
  })
  return readJson(res)
}

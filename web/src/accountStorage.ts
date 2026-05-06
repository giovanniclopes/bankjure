const KEY = 'bankjure.lastAccountId'

const UUID_RE =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

export function isAccountIdString(s: string): boolean {
  return UUID_RE.test(s.trim())
}

export function readStoredAccountId(): string | null {
  try {
    const v = localStorage.getItem(KEY)
    if (!v) return null
    const t = v.trim()
    return isAccountIdString(t) ? t : null
  } catch {
    return null
  }
}

export function writeStoredAccountId(id: string): void {
  try {
    if (isAccountIdString(id)) localStorage.setItem(KEY, id.trim())
  } catch {
    return
  }
}

export function clearStoredAccountId(): void {
  try {
    localStorage.removeItem(KEY)
  } catch {
    return
  }
}

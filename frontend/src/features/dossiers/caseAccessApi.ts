import { ApiError } from '../../shared/api'
import { authorizedFetch } from '../system/auth'

export type CaseAccessKind = 'STAFF' | 'CLIENT'
export type CaseGrant = { userId: string; kind: CaseAccessKind; grantedAt: string }

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers)
  headers.set('Accept', 'application/json')
  const response = await authorizedFetch(path, {
    ...init,
    headers,
  })
  if (!response.ok) throw new ApiError(response.status, response.headers.get('X-Correlation-Id'))
  return response.json() as Promise<T>
}

const base = (caseId: string) => `/api/dossiers/${encodeURIComponent(caseId)}/acces`

export function listCaseGrants(caseId: string, signal?: AbortSignal) {
  return request<CaseGrant[]>(base(caseId), { signal })
}

export function grantCaseAccess(caseId: string, userId: string, kind: CaseAccessKind) {
  return request<{ active: boolean }>(base(caseId), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, kind }),
  })
}

export function revokeCaseAccess(caseId: string, userId: string, kind: CaseAccessKind) {
  return request<{ active: boolean }>(`${base(caseId)}/${encodeURIComponent(userId)}?kind=${kind}`, {
    method: 'DELETE',
  })
}

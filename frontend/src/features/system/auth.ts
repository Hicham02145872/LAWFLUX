const base = import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8180'
const realm = 'lawflux'
const clientId = 'lawflux-web'
const redirectUri = `${window.location.origin}/connexion/retour`
let accessToken: string | null = null
let idToken: string | null = null
let profile: Profile | null = null
const listeners = new Set<() => void>()

export type Profile = { id: string; username: string | null; email: string | null; roles: string[] }
export const getProfile = () => profile
export async function authorizedFetch(input: RequestInfo | URL, init: RequestInit = {}) {
  if (!accessToken) throw new Error('Session expirée. Veuillez vous reconnecter.')
  const headers = new Headers(init.headers)
  headers.set('Authorization', `Bearer ${accessToken}`)
  const response = await fetch(input, { ...init, headers })
  if (response.status === 401) {
    accessToken = null
    idToken = null
    setProfile(null)
  }
  return response
}
export async function authorizedUpload(url: string, file: File, onProgress: (percent: number) => void): Promise<Response> {
  if (!accessToken) throw new Error('Session expirée. Veuillez vous reconnecter.')
  const token = accessToken
  const form = new FormData()
  form.append('file', file)
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('POST', url)
    xhr.setRequestHeader('Authorization', `Bearer ${token}`)
    xhr.upload.onprogress = event => {
      if (event.lengthComputable) onProgress(Math.round(event.loaded / event.total * 100))
    }
    xhr.onload = () => {
      if (xhr.status === 401) { accessToken = null; idToken = null; setProfile(null) }
      resolve(new Response(xhr.responseText, { status: xhr.status,
        headers: { 'Content-Type': xhr.getResponseHeader('Content-Type') ?? 'application/json' } }))
    }
    xhr.onerror = () => reject(new Error('Connexion au serveur impossible.'))
    xhr.send(form)
  })
}
export const subscribeToAuth = (listener: () => void) => {
  listeners.add(listener)
  return () => { listeners.delete(listener) }
}
function setProfile(value: Profile | null) {
  profile = value
  listeners.forEach(listener => listener())
}

function urlSafe(bytes: Uint8Array) {
  return btoa(String.fromCharCode(...bytes)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

export async function startLogin(returnTo = '/tableau-de-bord') {
  const verifier = urlSafe(crypto.getRandomValues(new Uint8Array(32)))
  const state = urlSafe(crypto.getRandomValues(new Uint8Array(16)))
  const challenge = urlSafe(new Uint8Array(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier))))
  sessionStorage.setItem('lawflux_oidc_verifier', verifier)
  sessionStorage.setItem('lawflux_oidc_state', state)
  sessionStorage.setItem('lawflux_return_to', returnTo)
  const url = new URL(`${base}/realms/${realm}/protocol/openid-connect/auth`)
  Object.entries({ client_id: clientId, redirect_uri: redirectUri, response_type: 'code', scope: 'openid profile email', code_challenge: challenge, code_challenge_method: 'S256', state }).forEach(([key, value]) => url.searchParams.set(key, value))
  window.location.assign(url.toString())
}

export async function finishLogin(search: string) {
  const params = new URLSearchParams(search)
  const code = params.get('code')
  const state = params.get('state')
  const savedState = sessionStorage.getItem('lawflux_oidc_state')
  const verifier = sessionStorage.getItem('lawflux_oidc_verifier')
  sessionStorage.removeItem('lawflux_oidc_state')
  sessionStorage.removeItem('lawflux_oidc_verifier')
  if (!code || !state || !savedState || state !== savedState || !verifier) throw new Error('Réponse de connexion invalide.')
  const body = new URLSearchParams({ grant_type: 'authorization_code', client_id: clientId, code, code_verifier: verifier, redirect_uri: redirectUri })
  const response = await fetch(`${base}/realms/${realm}/protocol/openid-connect/token`, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body })
  if (!response.ok) throw new Error('La connexion a échoué. Veuillez réessayer.')
  const token = await response.json() as { access_token: string; id_token: string }
  accessToken = token.access_token
  idToken = token.id_token
  const identity = await fetch('/api/auth/me', { headers: { Authorization: `Bearer ${accessToken}` } })
  if (!identity.ok) throw new Error('Jeton refusé par LAWFLUX.')
  const user = await identity.json() as Profile
  setProfile(user)
  const returnTo = sessionStorage.getItem('lawflux_return_to') ?? '/tableau-de-bord'
  sessionStorage.removeItem('lawflux_return_to')
  return returnTo.startsWith('/') && !returnTo.startsWith('//') ? returnTo : '/tableau-de-bord'
}

export function signOut() {
  const hint = idToken
  accessToken = null
  idToken = null
  setProfile(null)
  const url = new URL(`${base}/realms/${realm}/protocol/openid-connect/logout`)
  url.searchParams.set('client_id', clientId)
  url.searchParams.set('post_logout_redirect_uri', `${window.location.origin}/connexion`)
  if (hint) url.searchParams.set('id_token_hint', hint)
  window.location.assign(url.toString())
}

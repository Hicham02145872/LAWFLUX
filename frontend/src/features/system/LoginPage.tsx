import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { Page } from '../../shared/Page'
import { finishLogin, startLogin } from './auth'

export function LoginPage() {
  const [error, setError] = useState<string | null>(null)
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from ?? '/tableau-de-bord'
  return <Page title="Connexion au cabinet">
    <p>Accédez à votre espace LAWFLUX avec votre compte sécurisé.</p>
    <button type="button" onClick={() => void startLogin(from).catch(() => setError('Impossible de démarrer la connexion.'))}>Se connecter avec Keycloak</button>
    {error && <p role="alert">{error}</p>}
  </Page>
}

export function LoginCallbackPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const [message, setMessage] = useState('Connexion en cours…')
  const started = useRef(false)
  useEffect(() => {
    if (started.current) return
    started.current = true
    void finishLogin(location.search).then(destination => navigate(destination, { replace: true }))
      .catch(error => { setMessage(error instanceof Error ? error.message : 'Connexion impossible.') })
  }, [location.search, navigate])
  return <Page title="Connexion"><p role="status">{message}</p></Page>
}

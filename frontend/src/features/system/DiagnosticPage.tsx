import { useEffect, useState } from 'react'
import { ApiError, getJson } from '../../shared/api'
import { Page } from '../../shared/Page'

type Probe = { service: string; state: string; correlationId: string }
type Result = { probe: Probe } | { error: string }

function isProbe(value: unknown): value is Probe {
  return value !== null && typeof value === 'object'
    && 'service' in value && typeof value.service === 'string'
    && 'state' in value && typeof value.state === 'string'
    && 'correlationId' in value && typeof value.correlationId === 'string'
}

export function DiagnosticPage() {
  const [request, setRequest] = useState(0)
  const [result, setResult] = useState<Result | null>(null)

  useEffect(() => {
    if (request === 0) return
    const controller = new AbortController()
    void getJson('/api/system/services/cases', controller.signal)
      .then((value) => {
        if (!isProbe(value)) throw new Error('Réponse inattendue')
        if (!controller.signal.aborted) setResult({ probe: value })
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) return
        setResult({ error: error instanceof ApiError
          ? `Le diagnostic a échoué (HTTP ${error.status}).`
          : 'Réponse du service indisponible ou invalide.' })
      })
    return () => controller.abort()
  }, [request])

  const loading = request > 0 && result === null
  return (
    <Page title="Diagnostic">
      <p>Vérifie la connexion entre le portail, le BFF et le service Dossiers.</p>
      <button disabled={loading} onClick={() => { setResult(null); setRequest(request + 1) }}>
        {loading ? 'Vérification…' : 'Tester la connexion'}
      </button>
      <div role="status" aria-live="polite">
        {result && ('probe' in result
          ? <p>Connexion établie avec {result.probe.service}. Référence {result.probe.correlationId}.</p>
          : <p>{result.error}</p>)}
      </div>
    </Page>
  )
}

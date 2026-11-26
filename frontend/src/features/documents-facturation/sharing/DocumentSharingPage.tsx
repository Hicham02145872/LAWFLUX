import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { Page } from '../../../shared/Page'
import { authorizedFetch } from '../../system/auth'
import '../documents/documents.css'

type Sharing = {
  documentId: string
  name: string
  shared: boolean
  allowedActions: string[]
}

function errorMessage(status: number) {
  if (status === 401) return 'Session expirée. Reconnectez-vous.'
  if (status === 403 || status === 404) return 'Document inaccessible.'
  if (status === 503) return 'Service de documents indisponible.'
  return `Opération impossible (${status}).`
}

export function DocumentSharingPage() {
  const { id = '' } = useParams()
  const [sharing, setSharing] = useState<Sharing | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [refresh, setRefresh] = useState(0)
  const reload = useCallback(() => setRefresh(value => value + 1), [])

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(null)
    void authorizedFetch(`/api/documents/${encodeURIComponent(id)}/partage`)
      .then(async response => {
        if (!response.ok) throw new Error(errorMessage(response.status))
        return response.json() as Promise<Sharing>
      })
      .then(value => { if (active) setSharing(value) })
      .catch(reason => { if (active) setError(reason instanceof Error ? reason.message : 'Chargement impossible.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [id, refresh])

  async function changeShare(method: 'POST' | 'DELETE') {
    setBusy(true)
    setError(null)
    setNotice(null)
    try {
      const response = await authorizedFetch(`/api/documents/${encodeURIComponent(id)}/partage`, { method })
      if (!response.ok) throw new Error(errorMessage(response.status))
      setSharing(await response.json() as Sharing)
      setNotice(method === 'POST' ? 'Document partagé avec le client.' : 'Partage révoqué.')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Opération impossible.')
    } finally { setBusy(false) }
  }

  async function download() {
    setBusy(true)
    setError(null)
    try {
      const response = await authorizedFetch(`/api/documents/${encodeURIComponent(id)}/telechargement`)
      if (!response.ok) throw new Error(errorMessage(response.status))
      const blob = await response.blob()
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = sharing?.name ?? 'document'
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Téléchargement impossible.')
    } finally { setBusy(false) }
  }

  return <Page title="Partage du document">
    <p><Link to={`/documents/${encodeURIComponent(id)}/commentaires`}>Commentaires du document</Link></p>
    {loading && <p role="status">Chargement du partage…</p>}
    {error && <p role="alert">{error} {loading ? null : <button type="button" onClick={reload}>Réessayer</button>}</p>}
    {notice && <p role="status">{notice}</p>}
    {!loading && sharing && <section className="doc-sharing">
      <h2>{sharing.name}</h2>
      <p>Statut : <strong>{sharing.shared ? 'Partagé avec le client' : 'Privé au cabinet'}</strong></p>
      <div className="doc-sharing-actions">
        {sharing.allowedActions.includes('SHARE') && <button type="button" disabled={busy}
          onClick={() => void changeShare('POST')}>Partager avec le client</button>}
        {sharing.allowedActions.includes('UNSHARE') && <button type="button" disabled={busy}
          onClick={() => { if (window.confirm('Révoquer le partage de ce document ?')) void changeShare('DELETE') }}>Révoquer le partage</button>}
        {sharing.allowedActions.includes('DOWNLOAD') && <button type="button" disabled={busy}
          onClick={() => void download()}>Télécharger</button>}
      </div>
    </section>}
  </Page>
}

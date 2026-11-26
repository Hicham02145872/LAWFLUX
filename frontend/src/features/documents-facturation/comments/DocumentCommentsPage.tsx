import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { Page } from '../../../shared/Page'
import { authorizedFetch, getProfile } from '../../system/auth'
import '../documents/documents.css'

type Visibility = 'INTERNAL' | 'SHARED'
type Comment = {
  id: string
  documentId: string
  author: string
  visibility: Visibility
  body: string
  createdAt: string
}

async function message(response: Response): Promise<string> {
  if (response.status === 401) return 'Votre session a expiré. Reconnectez-vous.'
  if (response.status === 403 || response.status === 404) return 'Ce document est inaccessible.'
  if (response.status === 503) return 'Le service de documents est indisponible.'
  return `Erreur du serveur (${response.status}).`
}

export function DocumentCommentsPage() {
  const { id = '' } = useParams()
  const client = getProfile()?.roles.includes('client') ?? false
  const [comments, setComments] = useState<Comment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [body, setBody] = useState('')
  const [visibility, setVisibility] = useState<Visibility>('SHARED')
  const [sending, setSending] = useState(false)
  const [sendError, setSendError] = useState<string | null>(null)
  const [refresh, setRefresh] = useState(0)

  const reload = useCallback(() => setRefresh(value => value + 1), [])
  useEffect(() => {
    let active = true
    setLoading(true)
    setError(null)
    void authorizedFetch(`/api/documents/${encodeURIComponent(id)}/commentaires`)
      .then(async response => {
        if (!response.ok) throw new Error(await message(response))
        return response.json() as Promise<Comment[]>
      })
      .then(items => { if (active) setComments(items) })
      .catch(reason => { if (active) setError(reason instanceof Error ? reason.message : 'Chargement impossible.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [id, refresh])

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!body.trim()) { setSendError('Écrivez un commentaire.'); return }
    if ([...body.trim()].length > 5000) { setSendError('Le commentaire dépasse 5 000 caractères.'); return }
    setSending(true)
    setSendError(null)
    try {
      const response = await authorizedFetch(`/api/documents/${encodeURIComponent(id)}/commentaires`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ visibility: client ? 'SHARED' : visibility, body: body.trim() }),
      })
      if (!response.ok) throw new Error(await message(response))
      setBody('')
      reload()
    } catch (reason) {
      setSendError(reason instanceof Error ? reason.message : 'Enregistrement impossible.')
    } finally { setSending(false) }
  }

  return <Page title="Commentaires du document">
    <p><Link to={`/documents/${encodeURIComponent(id)}/versions`}>Voir les versions</Link></p>
    {loading && <p role="status">Chargement des commentaires…</p>}
    {error && <p role="alert">{error} <button type="button" onClick={reload}>Réessayer</button></p>}
    {!loading && !error && comments.length === 0 && <p>Aucun commentaire pour ce document.</p>}
    {!loading && !error && comments.length > 0 && <ol className="doc-comments">
      {comments.map(comment => <li key={comment.id}>
        <div className="doc-comment-head"><strong>{comment.author}</strong>
          <span className="doc-badge">{comment.visibility === 'INTERNAL' ? 'Interne' : 'Partagé avec le client'}</span>
          <time dateTime={comment.createdAt}>{new Date(comment.createdAt).toLocaleString('fr-FR')}</time>
        </div>
        <p>{comment.body}</p>
      </li>)}
    </ol>}
    {!error && <form className="doc-comment-form" onSubmit={submit}>
      <label htmlFor="comment-body">Nouveau commentaire</label>
      <textarea id="comment-body" value={body} onChange={event => setBody(event.target.value)}
        rows={5} maxLength={5000} required disabled={sending} />
      {!client && <label htmlFor="comment-visibility">Visibilité</label>}
      {!client && <select id="comment-visibility" value={visibility}
        onChange={event => setVisibility(event.target.value as Visibility)} disabled={sending}>
        <option value="SHARED">Partagé avec le client</option>
        <option value="INTERNAL">Interne au cabinet</option>
      </select>}
      <button type="submit" disabled={sending}>{sending ? 'Envoi…' : 'Publier le commentaire'}</button>
      {sendError && <p role="alert" className="doc-error">{sendError}</p>}
    </form>}
  </Page>
}

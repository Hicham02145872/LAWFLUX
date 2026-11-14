import { useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { useParams } from 'react-router'
import { Page } from '../../../shared/Page'
import '../documents/documents.css'
import { useAddVersion, useVersions } from './useVersions'

const ACCEPTED = /\.(pdf|docx)$/i

export function VersionListPage() {
  const { id = '' } = useParams()
  const { versions, loading, error, reload } = useVersions(id)
  const { addVersion, sending, error: sendError } = useAddVersion(id, reload)
  const [file, setFile] = useState<File | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const formRef = useRef<HTMLFormElement>(null)

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (!file) return setFormError('Sélectionnez un fichier PDF ou DOCX.')
    if (!ACCEPTED.test(file.name)) return setFormError('Format non pris en charge : PDF ou DOCX uniquement.')
    setFormError(null)
    await addVersion(file)
    formRef.current?.reset()
    setFile(null)
  }

  return (
    <Page title="Versions du document">
      <form ref={formRef} className="doc-upload" onSubmit={onSubmit} noValidate>
        <label htmlFor="version-file">Nouvelle version (PDF ou DOCX)</label>
        <input
          id="version-file"
          type="file"
          accept=".pdf,.docx"
          required
          disabled={sending}
          onChange={(e) => { setFile(e.target.files?.[0] ?? null); setFormError(null) }}
        />
        <button type="submit" disabled={sending}>
          {sending ? 'Ajout en cours…' : 'Ajouter une version'}
        </button>
        {(formError || sendError) && <p className="doc-error" role="alert">{formError ?? sendError}</p>}
      </form>

      <div role="status" aria-live="polite">
        {loading && <p className="doc-state">Chargement…</p>}
      </div>
      {error && (
        <p className="doc-state" role="alert">
          {error} <button onClick={reload}>Réessayer</button>
        </p>
      )}
      {!loading && !error && versions.length === 0 && (
        <p className="doc-state">Aucune version pour ce document.</p>
      )}
      {!loading && !error && versions.length > 0 && (
        <table className="doc-table">
          <thead>
            <tr><th>Version</th><th>Auteur</th><th>Date</th></tr>
          </thead>
          <tbody>
            {versions.map((v) => (
              <tr key={v.number}>
                <td>v{v.number}</td>
                <td>{v.author}</td>
                <td>{new Date(v.createdAt).toLocaleDateString('fr-FR')}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </Page>
  )
}
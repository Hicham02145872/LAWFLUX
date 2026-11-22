import './documents.css'
import { useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { useParams } from 'react-router'
import { Page } from '../../../shared/Page'
import { useDocuments, useUploadDocument } from './useDocuments'

const ACCEPTED = /\.(pdf|docx)$/i

export function DocumentListPage() {
  const { id } = useParams()
  const { documents, loading, error, reload } = useDocuments(id)
  const { upload, uploading, progress, error: uploadError } = useUploadDocument(reload)
  const [file, setFile] = useState<File | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const formRef = useRef<HTMLFormElement>(null)

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (!file) return setFormError('Sélectionnez un fichier PDF ou DOCX.')
    if (!ACCEPTED.test(file.name)) return setFormError('Format non pris en charge : PDF ou DOCX uniquement.')
    setFormError(null)
    await upload(file)
    formRef.current?.reset()
    setFile(null)
  }

  return (
    <Page title="Documents du dossier">
      <form ref={formRef} className="doc-upload" onSubmit={onSubmit} noValidate>
  <label htmlFor="document-file">Fichier (PDF ou DOCX)</label>
  <input
    id="document-file"
    type="file"
    accept=".pdf,.docx"
    required
    disabled={uploading}
    onChange={(e) => { setFile(e.target.files?.[0] ?? null); setFormError(null) }}
  />
  <button type="submit" disabled={uploading}>
    {uploading ? 'Import en cours…' : 'Importer'}
  </button>
  {uploading && <progress value={progress} max={100} aria-label="Progression de l'import" />}
  {(formError || uploadError) && <p className="doc-error" role="alert">{formError ?? uploadError}</p>}
</form>

<div role="status" aria-live="polite">
  {loading && <p className="doc-state">Chargement…</p>}
</div>
{error && (
  <p className="doc-state" role="alert">
    {error} <button onClick={reload}>Réessayer</button>
  </p>
)}
{!loading && !error && documents.length === 0 && (
  <p className="doc-state">Aucun document pour ce dossier.</p>
)}
{!loading && !error && documents.length > 0 && (
  <table className="doc-table">
    <thead>
      <tr><th>Nom</th><th>Type</th><th>Auteur</th><th>Date</th></tr>
    </thead>
    <tbody>
      {documents.map((d) => (
        <tr key={d.id}>
          <td>{d.name}</td>
          <td>
            <span className={`doc-badge doc-badge--${d.type.toLowerCase()}`}>{d.type}</span>
          </td>
          <td>{d.author}</td>
          <td>{new Date(d.createdAt).toLocaleDateString('fr-FR')}</td>
        </tr>
      ))}
    </tbody>
  </table>
)}
    </Page>
  )
}
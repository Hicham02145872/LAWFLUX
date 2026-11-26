import { useCallback, useEffect, useState } from 'react'
import { authorizedFetch, authorizedUpload } from '../../system/auth'
import type { DocumentItem } from './types'

export function useDocuments(dossierId: string | undefined) {
  const [documents, setDocuments] = useState<DocumentItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [version, setVersion] = useState(0)

  useEffect(() => {
    if (!dossierId) { setError('Dossier introuvable.'); setLoading(false); return }
    let active = true
    setLoading(true)
    setError(null)
    void authorizedFetch(`/api/dossiers/${encodeURIComponent(dossierId)}/documents`)
      .then(async response => {
        if (!response.ok) throw new Error(`Chargement impossible (${response.status}).`)
        return response.json() as Promise<DocumentItem[]>
      })
      .then(items => { if (active) setDocuments(items) })
      .catch(reason => { if (active) setError(reason instanceof Error ? reason.message : 'Chargement impossible.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [dossierId, version])

  const reload = useCallback(() => setVersion(value => value + 1), [])
  return { documents, loading, error, reload }
}

export function useUploadDocument(dossierId: string | undefined, onSuccess: () => void) {
  const [uploading, setUploading] = useState(false)
  const [progress, setProgress] = useState(0)
  const [error, setError] = useState<string | null>(null)

  async function upload(file: File): Promise<boolean> {
    if (!dossierId) { setError('Dossier introuvable.'); return false }
    setUploading(true)
    setProgress(0)
    setError(null)
    try {
      const response = await authorizedUpload(`/api/dossiers/${encodeURIComponent(dossierId)}/documents`, file, setProgress)
      if (!response.ok) throw new Error(`Import refusé (${response.status}).`)
      onSuccess()
      return true
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "L'import a échoué.")
      return false
    } finally { setUploading(false) }
  }
  return { upload, uploading, progress, error }
}

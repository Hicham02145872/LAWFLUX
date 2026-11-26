import { useCallback, useEffect, useState } from 'react'
import { authorizedFetch, authorizedUpload } from '../../system/auth'
import type { DocumentVersion } from './types'

export function useVersions(documentId: string) {
  const [versions, setVersions] = useState<DocumentVersion[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [refresh, setRefresh] = useState(0)

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(null)
    void authorizedFetch(`/api/documents/${encodeURIComponent(documentId)}/versions`)
      .then(async response => {
        if (!response.ok) throw new Error(`Chargement impossible (${response.status}).`)
        return response.json() as Promise<DocumentVersion[]>
      })
      .then(items => { if (active) setVersions(items) })
      .catch(reason => { if (active) setError(reason instanceof Error ? reason.message : 'Chargement impossible.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [documentId, refresh])

  const reload = useCallback(() => setRefresh(value => value + 1), [])
  return { versions, loading, error, reload }
}

export function useAddVersion(documentId: string, onSuccess: () => void) {
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function addVersion(file: File): Promise<boolean> {
    setSending(true)
    setError(null)
    try {
      const response = await authorizedUpload(`/api/documents/${encodeURIComponent(documentId)}/versions`, file, () => {})
      if (!response.ok) throw new Error(`Version refusée (${response.status}).`)
      onSuccess()
      return true
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "L'ajout a échoué.")
      return false
    } finally { setSending(false) }
  }
  return { addVersion, sending, error }
}

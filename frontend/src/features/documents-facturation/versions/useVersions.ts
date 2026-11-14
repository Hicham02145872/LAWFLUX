import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'
import type { DocumentVersion } from './types'

const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

const store = new Map<string, DocumentVersion[]>()

function versionsOf(documentId: string): DocumentVersion[] {
  if (!store.has(documentId)) {
    store.set(documentId, [
      { number: 2, author: 'Sara', createdAt: '2026-11-08' },
      { number: 1, author: 'Karim', createdAt: '2026-11-01' },
    ])
  }
  return store.get(documentId)!
}

// MOCK : à remplacer par le hook fourni par le chef.
// États de test : ?etat=vide ou ?etat=erreur
export function useVersions(documentId: string) {
  const [params] = useSearchParams()
  const etat = params.get('etat')
  const [versions, setVersions] = useState<DocumentVersion[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [refresh, setRefresh] = useState(0)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    void wait(600).then(() => {
      if (cancelled) return
      if (etat === 'erreur') setError('Impossible de charger les versions.')
      else setVersions(etat === 'vide' ? [] : [...versionsOf(documentId)])
      setLoading(false)
    })
    return () => { cancelled = true }
  }, [documentId, etat, refresh])

  const reload = useCallback(() => setRefresh((v) => v + 1), [])
  return { versions, loading, error, reload }
}

// MOCK : à remplacer par la fonction fournie par le chef.
// Le numéro de version sera attribué par le serveur (ici simulé).
export function useAddVersion(documentId: string, onSuccess: () => void) {
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function addVersion(_file: File) {
    setSending(true)
    setError(null)
    try {
      await wait(800)
      const list = versionsOf(documentId)
      list.unshift({
        number: (list[0]?.number ?? 0) + 1,
        author: 'Moi',
        createdAt: new Date().toISOString().slice(0, 10),
      })
      onSuccess()
    } catch {
      setError("L'ajout de la version a échoué. Réessayez.")
    } finally {
      setSending(false)
    }
  }

  return { addVersion, sending, error }
}
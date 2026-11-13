import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'
import { mockDocuments } from './mocks'
import type { DocumentItem } from './types'

const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))
const store: DocumentItem[] = [...mockDocuments]

// MOCK : à remplacer par le hook fourni par le chef.
// Pour tester les états : ?etat=vide ou ?etat=erreur (à supprimer ensuite).
export function useDocuments(dossierId: string | undefined) {
  const [params] = useSearchParams()
  const etat = params.get('etat')
  const [documents, setDocuments] = useState<DocumentItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [version, setVersion] = useState(0)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    void wait(600).then(() => {
      if (cancelled) return
      if (etat === 'erreur') setError('Impossible de charger les documents.')
        else setDocuments(etat === 'vide' ? [] : [...store])

      setLoading(false)
    })
    return () => { cancelled = true }
  }, [dossierId, etat, version])

  const reload = useCallback(() => setVersion((v) => v + 1), [])
  return { documents, loading, error, reload }
}

// MOCK : à remplacer par la fonction d'upload fournie par le chef.
export function useUploadDocument(onSuccess: () => void) {
  const [uploading, setUploading] = useState(false)
  const [progress, setProgress] = useState(0)
  const [error, setError] = useState<string | null>(null)

  async function upload(file: File) {
    setUploading(true)
    setProgress(0)
    setError(null)
    try {
      for (const p of [25, 50, 75, 100]) {
        await wait(300)
        setProgress(p)
      }
      store.unshift({
        id: crypto.randomUUID(),
        name: file.name,
        type: /\.pdf$/i.test(file.name) ? 'PDF' : 'DOCX',
        author: 'Moi',
        createdAt: new Date().toISOString().slice(0, 10),
      })
      onSuccess()
    } catch {
      setError("L'import a échoué. Réessayez.")
    } finally {
      setUploading(false)
    }
  }

  return { upload, uploading, progress, error }
}
import { useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { authorizedFetch } from '../system/auth'

type Client = {
  id: string
  name: string
  email: string
  phone: string
  address: string
  status: string
  createdAt: string
  updatedAt: string
  archivedAt: string
}

export function ClientDetailPage() {
  const { id } = useParams()
  const [client, setClient] = useState<Client | null>(null)
  const [editing, setEditing] = useState(false)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()

    async function loadClient() {
      try {
        const response = await authorizedFetch(`/api/clients/${id}`, {
          signal: controller.signal,
          headers: { Accept: 'application/json' },
        })

        if (!response.ok) throw new Error(`Erreur serveur (${response.status})`)

        setClient(await response.json() as Client)
      } catch (err) {
        if (!controller.signal.aborted) {
          setError(err instanceof Error ? err.message : 'Impossible de charger le client.')
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false)
      }
    }

    if (id) void loadClient()
    return () => controller.abort()
  }, [id])

  async function saveClient(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!client) return

    setSaving(true)
    setError('')

    try {
      const response = await authorizedFetch(`/api/clients/${client.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: client.name,
          email: client.email,
          phone: client.phone,
          address: client.address,
        }),
      })

      if (!response.ok) throw new Error(`Erreur serveur (${response.status})`)

      setClient(await response.json() as Client)
      setEditing(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Impossible de modifier le client.')
    } finally {
      setSaving(false)
    }
  }

  async function archiveClient() {
    if (!client || !window.confirm(`Archiver ${client.name} ?`)) return

    setError('')
    try {
      const response = await authorizedFetch(`/api/clients/${client.id}/archive`, {
        method: 'PATCH',
      })

      if (!response.ok) throw new Error(`Erreur serveur (${response.status})`)

      setClient({ ...client, status: 'ARCHIVED', archivedAt: new Date().toISOString() })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Impossible d’archiver le client.')
    }
  }

  function updateField(field: 'name' | 'email' | 'phone' | 'address', value: string) {
    setClient(current => current ? { ...current, [field]: value } : current)
  }

  if (loading) return <section className="page"><p>Chargement du client…</p></section>
  if (error && !client) return <section className="page"><p role="alert">{error}</p></section>
  if (!client) return <section className="page"><p>Client introuvable.</p></section>

  return (
    <section className="page">
      <p><Link to="/clients">Clients</Link> / Fiche client</p>

      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap' }}>
        <div>
          <p style={{ color: '#64748b', marginBottom: 6 }}>Fiche client</p>
          <h1>{client.name}</h1>
          <p style={{ color: '#64748b' }}>
            Statut : {client.status === 'ACTIVE' ? 'Actif' : 'Archivé'}
          </p>
        </div>

        {!editing && client.status === 'ACTIVE' && (
          <div style={{ display: 'flex', gap: 8 }}>
            <button type="button" onClick={() => setEditing(true)}>Modifier</button>
            <button type="button" onClick={() => void archiveClient()}>Archiver</button>
          </div>
        )}
      </div>

      {error && <p role="alert" style={{ color: '#b91c1c' }}>{error}</p>}

      {editing ? (
        <form onSubmit={saveClient} style={{ display: 'grid', gap: 16, maxWidth: 700, marginTop: 24 }}>
          <label>
            Nom *
            <input required value={client.name} onChange={event => updateField('name', event.target.value)} />
          </label>
          <label>
            Email
            <input type="email" value={client.email} onChange={event => updateField('email', event.target.value)} />
          </label>
          <label>
            Téléphone
            <input value={client.phone} onChange={event => updateField('phone', event.target.value)} />
          </label>
          <label>
            Adresse
            <textarea rows={3} value={client.address} onChange={event => updateField('address', event.target.value)} />
          </label>
          <div style={{ display: 'flex', gap: 10 }}>
            <button type="submit" disabled={saving}>
              {saving ? 'Enregistrement…' : 'Enregistrer'}
            </button>
            <button type="button" onClick={() => setEditing(false)}>Annuler</button>
          </div>
        </form>
      ) : (
        <div style={{ display: 'grid', gap: 16, maxWidth: 700, marginTop: 24 }}>
          <h2>Coordonnées</h2>
          <p><strong>Email :</strong> {client.email || 'Non renseigné'}</p>
          <p><strong>Téléphone :</strong> {client.phone || 'Non renseigné'}</p>
          <p><strong>Adresse :</strong> {client.address || 'Non renseignée'}</p>
          <p><strong>Créé le :</strong> {client.createdAt || '—'}</p>
        </div>
      )}
    </section>
  )
}
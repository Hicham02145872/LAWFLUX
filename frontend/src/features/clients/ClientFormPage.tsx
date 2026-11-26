import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { authorizedFetch } from '../system/auth'

export function ClientFormPage() {
  const navigate = useNavigate()
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [form, setForm] = useState({
    name: '',
    email: '',
    phone: '',
    address: '',
  })

  function updateField(field: keyof typeof form, value: string) {
    setForm(current => ({ ...current, [field]: value }))
  }

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaving(true)
    setError('')

    try {
      const response = await authorizedFetch('/api/clients', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(form),
      })

      if (!response.ok) throw new Error(`Erreur serveur (${response.status})`)

      navigate('/clients')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Impossible de créer le client.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="page">
      <p><Link to="/clients">Clients</Link> / Nouveau client</p>
      <h1>Enrôlement d’un nouveau client</h1>
      <p>Ajoutez les coordonnées principales du client au portefeuille du cabinet.</p>

      <form onSubmit={submit}>
        <div style={{ display: 'grid', gap: 18, maxWidth: 760, marginTop: 28 }}>
          <label>
            Nom du client *
            <input
              required
              value={form.name}
              onChange={event => updateField('name', event.target.value)}
              placeholder="Ex. Groupe Al Massira S.A."
              style={{ display: 'block', width: '100%', minHeight: 44, marginTop: 6, padding: 10 }}
            />
          </label>

          <label>
            Adresse email
            <input
              type="email"
              value={form.email}
              onChange={event => updateField('email', event.target.value)}
              placeholder="contact@entreprise.ma"
              style={{ display: 'block', width: '100%', minHeight: 44, marginTop: 6, padding: 10 }}
            />
          </label>

          <label>
            Téléphone
            <input
              value={form.phone}
              onChange={event => updateField('phone', event.target.value)}
              placeholder="+212 6 00 00 00 00"
              style={{ display: 'block', width: '100%', minHeight: 44, marginTop: 6, padding: 10 }}
            />
          </label>

          <label>
            Adresse
            <textarea
              value={form.address}
              onChange={event => updateField('address', event.target.value)}
              placeholder="Adresse complète"
              rows={3}
              style={{ display: 'block', width: '100%', marginTop: 6, padding: 10 }}
            />
          </label>
        </div>

        {error && <p role="alert" style={{ color: '#b91c1c' }}>{error}</p>}

        <div style={{ display: 'flex', gap: 12, marginTop: 24 }}>
          <Link to="/clients">Annuler</Link>
          <button type="submit" disabled={saving}>
            {saving ? 'Enregistrement…' : 'Enregistrer le client'}
          </button>
        </div>
      </form>
    </section>
  )
}
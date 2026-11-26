import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { authorizedFetch } from '../system/auth'
import './ClientsPage.css'

type Client = {
  id: string
  name: string
  email: string
  phone: string
  address: string
  status: string
}

type ClientPage = {
  items: Client[]
  page: number
  pageSize: number
  totalElements: number
  totalPages: number
}

export function ClientsPage() {
  const [clients, setClients] = useState<Client[]>([])
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('')
  const [selectedId, setSelectedId] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()

    async function loadClients() {
      setLoading(true)
      setError('')

      try {
        const params = new URLSearchParams({
          page: String(page),
          pageSize: '10',
        })
        if (status) params.set('status', status)

        const response = await authorizedFetch(`/api/clients?${params}`, {
          signal: controller.signal,
          headers: { Accept: 'application/json' },
        })

        if (!response.ok) throw new Error(`Erreur serveur (${response.status})`)

        const data = await response.json() as ClientPage
        setClients(data.items)
        setTotalPages(data.totalPages)
        setTotalElements(data.totalElements)
        setSelectedId(data.items[0]?.id ?? '')
      } catch (err) {
        if (!controller.signal.aborted) {
          setError(err instanceof Error ? err.message : 'Impossible de charger les clients.')
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false)
      }
    }

    void loadClients()
    return () => controller.abort()
  }, [page, status])

  const visibleClients = clients.filter(client =>
    `${client.name} ${client.email} ${client.phone} ${client.address}`
      .toLowerCase()
      .includes(search.toLowerCase()),
  )

  const selectedClient = clients.find(client => client.id === selectedId)
  const activeCount = clients.filter(client => client.status === 'ACTIVE').length
  const archivedCount = clients.filter(client => client.status !== 'ACTIVE').length

  return (
    <section className="clients-page">
      <div className="clients-heading">
        <div>
          <p className="clients-breadcrumb">Cabinet / Clients</p>
          <h1>Répertoire &amp; portefeuille des clients</h1>
          <p className="clients-subtitle">
            Consultez les clients du cabinet et leurs coordonnées.
          </p>
        </div>
        <Link className="clients-primary-button" to="/clients/nouveau">
          + Nouveau client
        </Link>
      </div>

      <div className="clients-stats">
        <article className="clients-stat-card">
          <span>Total clients</span>
          <strong>{totalElements}</strong>
          <small>Dans le portefeuille du cabinet</small>
        </article>
        <article className="clients-stat-card">
          <span>Clients actifs</span>
          <strong>{activeCount}</strong>
          <small>Sur cette page</small>
        </article>
        <article className="clients-stat-card">
          <span>Clients archivés</span>
          <strong>{archivedCount}</strong>
          <small>Sur cette page</small>
        </article>
      </div>

      <div className="clients-toolbar">
        <input
          aria-label="Rechercher un client"
          placeholder="Rechercher par nom, email ou téléphone…"
          value={search}
          onChange={event => setSearch(event.target.value)}
        />
        <select
          aria-label="Filtrer par statut"
          value={status}
          onChange={event => {
            setStatus(event.target.value)
            setPage(1)
          }}
        >
          <option value="">Tous les statuts</option>
          <option value="ACTIVE">Actifs</option>
          <option value="ARCHIVED">Archivés</option>
        </select>
      </div>

      {loading && <p role="status">Chargement des clients…</p>}
      {error && <p className="clients-error" role="alert">{error}</p>}

      {!loading && !error && (
        <div className="clients-content">
          <div className="clients-table-card">
            <div className="clients-table-title">
              <div>
                <h2>Clients du cabinet</h2>
                <p>{totalElements} client(s) au total</p>
              </div>
            </div>

            <div className="clients-table-scroll">
              <table className="clients-table">
                <thead>
                  <tr>
                    <th>Client</th>
                    <th>Coordonnées</th>
                    <th>Statut</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {visibleClients.map(client => (
                    <tr
                      key={client.id}
                      className={client.id === selectedId ? 'is-selected' : ''}
                      onClick={() => setSelectedId(client.id)}
                    >
                      <td>
                        <strong>{client.name}</strong>
                        <small>{client.address || 'Adresse non renseignée'}</small>
                      </td>
                      <td>
                        <span>{client.email || 'Email non renseigné'}</span>
                        <small>{client.phone || 'Téléphone non renseigné'}</small>
                      </td>
                      <td>
                        <span className={`clients-badge ${client.status === 'ACTIVE' ? 'active' : 'archived'}`}>
                          {client.status === 'ACTIVE' ? 'Actif' : 'Archivé'}
                        </span>
                      </td>
                      <td>
                        <Link
                          to={`/clients/${client.id}`}
                          onClick={event => event.stopPropagation()}
                        >
                          Voir
                        </Link>
                      </td>
                    </tr>
                  ))}

                  {visibleClients.length === 0 && (
                    <tr>
                      <td colSpan={4} className="clients-empty">
                        Aucun client trouvé.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>

            <div className="clients-pagination">
              <span>Page {page} / {Math.max(totalPages, 1)}</span>
              <div>
                <button disabled={page <= 1} onClick={() => setPage(page - 1)}>
                  Précédent
                </button>
                <button disabled={page >= totalPages} onClick={() => setPage(page + 1)}>
                  Suivant
                </button>
              </div>
            </div>
          </div>

          <aside className="clients-detail-card">
            {selectedClient ? (
              <>
                <div className="clients-detail-avatar">
                  {selectedClient.name.slice(0, 2).toUpperCase()}
                </div>
                <p className="clients-detail-label">CLIENT SÉLECTIONNÉ</p>
                <h2>{selectedClient.name}</h2>
                <span className={`clients-badge ${selectedClient.status === 'ACTIVE' ? 'active' : 'archived'}`}>
                  {selectedClient.status === 'ACTIVE' ? 'Actif' : 'Archivé'}
                </span>

                <div className="clients-detail-section">
                  <h3>Coordonnées</h3>
                  <p><strong>Email</strong><br />{selectedClient.email || 'Non renseigné'}</p>
                  <p><strong>Téléphone</strong><br />{selectedClient.phone || 'Non renseigné'}</p>
                  <p><strong>Adresse</strong><br />{selectedClient.address || 'Non renseignée'}</p>
                </div>

                <Link className="clients-detail-link" to={`/clients/${selectedClient.id}`}>
                  Consulter la fiche client
                </Link>
              </>
            ) : (
              <p>Sélectionne un client pour voir ses coordonnées.</p>
            )}
          </aside>
        </div>
      )}
    </section>
  )
}
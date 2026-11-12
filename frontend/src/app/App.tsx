import { Link, Navigate, NavLink, Outlet, useRoutes } from 'react-router'
import { dossierRoutes } from '../features/dossiers/routes'
import { documentRoutes } from '../features/documents-facturation/routes'
import { DiagnosticPage } from '../features/system/DiagnosticPage'
import { Page, PendingPage } from '../shared/Page'

const links = [
  ['/tableau-de-bord', 'Tableau de bord'],
  ['/clients', 'Clients'],
  ['/dossiers', 'Dossiers'],
  ['/taches', 'Tâches'],
  ['/temps', 'Temps'],
  ['/factures', 'Factures'],
  ['/portail/dossiers', 'Portail client'],
  ['/diagnostic', 'Diagnostic'],
] as const

function Layout() {
  return (
    <>
      <a className="skip-link" href="#contenu">Aller au contenu</a>
      <header>
        <div className="shell">
          <Link className="brand" to="/">LAWFLUX</Link>
          <nav aria-label="Navigation principale">
            {links.map(([path, label]) => <NavLink key={path} to={path}>{label}</NavLink>)}
          </nav>
        </div>
      </header>
      <main id="contenu" className="shell" tabIndex={-1}>
        <Outlet />
      </main>
    </>
  )
}

export function App() {
  return useRoutes([{
    element: <Layout />,
    children: [
      { index: true, element: <Navigate to="/tableau-de-bord" replace /> },
      ...dossierRoutes,
      ...documentRoutes,
      { path: 'diagnostic', element: <DiagnosticPage /> },
      { path: 'connexion', element: <PendingPage title="Connexion" /> },
      { path: 'notifications', element: <PendingPage title="Notifications" /> },
      { path: 'parametres/cabinet', element: <PendingPage title="Cabinet" /> },
      { path: 'parametres/membres', element: <PendingPage title="Membres" /> },
      { path: 'parametres/securite', element: <PendingPage title="Sécurité" /> },
      { path: 'temps/minuteur', element: <PendingPage title="Minuteur" /> },
      { path: 'factures/:id/correction', element: <PendingPage title="Correction de facture" /> },
      { path: '*', element: <Page title="Page introuvable"><Link to="/">Retour à l’accueil</Link></Page> },
    ],
  }])
}

import type { RouteObject } from 'react-router'
import { PendingPage } from '../../shared/Page'
import { ClientFormPage } from '../clients/ClientFormPage'
import { ClientsPage } from '../clients/ClientsPage'
import { ClientDetailPage } from '../clients/ClientDetailPage'


const pages = [
  ['tableau-de-bord', 'Tableau de bord'],
  ['dossiers', 'Dossiers'],
  ['dossiers/nouveau', 'Nouveau dossier'],
  ['dossiers/:id', 'Fiche dossier'],
  ['dossiers/:id/acces', 'Accès au dossier'],
  ['dossiers/:id/historique', 'Historique du dossier'],
  ['dossiers/:id/commentaires', 'Commentaires du dossier'],
  ['portail/dossiers', 'Portail client'],
  ['portail/dossiers/:id', 'Dossier du client'],
  ['taches', 'Tâches'],
  ['taches/nouvelle', 'Nouvelle tâche'],
] as const

export const dossierRoutes: RouteObject[] = [
  { path: 'clients', element: <ClientsPage /> },
  { path: 'clients/nouveau', element: <ClientFormPage /> },
  { path: 'clients/:id', element: <ClientDetailPage /> },
  ...pages.map(([path, title]) => ({
    path,
    element: <PendingPage title={title} />,
  })),
]
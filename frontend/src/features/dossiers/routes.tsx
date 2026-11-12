import type { RouteObject } from 'react-router'
import { PendingPage } from '../../shared/Page'

const pages = [
  ['tableau-de-bord', 'Tableau de bord'],
  ['clients', 'Clients'],
  ['clients/nouveau', 'Nouveau client'],
  ['clients/:id', 'Fiche client'],
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

export const dossierRoutes: RouteObject[] = pages.map(([path, title]) => ({
  path, element: <PendingPage title={title} />,
}))

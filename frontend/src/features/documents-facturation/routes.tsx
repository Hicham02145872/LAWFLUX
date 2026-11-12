import type { RouteObject } from 'react-router'
import { PendingPage } from '../../shared/Page'

const pages = [
  ['dossiers/:id/documents', 'Documents du dossier'],
  ['documents/:id/versions', 'Versions du document'],
  ['documents/:id/partage', 'Partage du document'],
  ['documents/:id/commentaires', 'Commentaires du document'],
  ['documents/:id/risques', 'Risques du document'],
  ['temps', 'Temps'],
  ['temps/nouveau', 'Nouvelle saisie de temps'],
  ['parametres/tarifs', 'Tarifs du cabinet'],
  ['dossiers/:id/tarif', 'Tarif du dossier'],
  ['factures', 'Factures'],
  ['factures/nouvelle', 'Nouvelle facture'],
  ['factures/:id', 'Détail de la facture'],
] as const

export const documentRoutes: RouteObject[] = pages.map(([path, title]) => ({
  path, element: <PendingPage title={title} />,
}))

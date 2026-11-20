# Raccordement des documents

Les interfaces de US-14 à US-16 livrées par Oumaima ont été reprises dans la copie de travail. Les composants communs sont dans `document-service/infrastructure` : `PrivateFileStorage`, `DocumentJwtIdentity`, `GrpcCaseAccessChecker` et `CaseBasedDocumentAccessPolicy`.

Le BFF accepte un jeton Keycloak dans `Authorization: Bearer …` et le transmet aux RPC gRPC. Routes REST disponibles pour le frontend :

| Méthode | Route | Résultat |
| --- | --- | --- |
| GET | `/api/dossiers/{caseId}/documents` | Liste de `DocumentItem` |
| POST | `/api/dossiers/{caseId}/documents` | Import `multipart/form-data`, champ `file`, retourne `DocumentItem` |
| GET | `/api/documents/{id}/versions` | Liste de `VersionItem` |
| POST | `/api/documents/{id}/versions` | Nouvelle version, champ `file`, retourne `VersionItem` |
| GET | `/api/documents/{id}/partage` | État `SharingItem` |
| POST | `/api/documents/{id}/partage` | Partager, retourne `SharingItem` |
| DELETE | `/api/documents/{id}/partage` | Révoquer, retourne `SharingItem` |
| GET | `/api/documents/{id}/telechargement` | Flux binaire avec nom de fichier |

`DocumentItem` contient `id`, `name`, `type`, `author`, `createdAt`. `VersionItem` contient `number`, `author`, `createdAt`, `type`, `sizeBytes`. `SharingItem` contient `documentId`, `name`, `shared`, `allowedActions` (`SHARE`, `UNSHARE`, `DOWNLOAD`). Les erreurs gRPC deviennent des réponses HTTP 400, 401, 403, 404 ou 503 selon le cas.

Pour un environnement de développement, chaque utilisateur Keycloak doit avoir un rôle du realm et un attribut utilisateur `cabinet_id` UUID. Le mapper de cet attribut figure dans `infra/keycloak/lawflux-realm.json`. **L'import du realm ne met pas à jour un realm déjà créé** : appliquer le mapper au client `lawflux-web` dans l'administration Keycloak si le volume existe déjà. L'appartenance au cabinet et l'accès au dossier doivent encore être validés par `case-service` ; le simple attribut du jeton ne suffit pas à donner accès.

`DOCUMENTS_BUSINESS_ENABLED` reste `false` par défaut. Le contrôle `CheckCaseAccess` du `case-service` est encore un contrat sans implémentation ; le client document refuse l'accès quand le RPC échoue ou répond `false`. Il faut implémenter ce contrôle côté dossiers avant de tester un parcours métier complet. Aucun contournement local du contrôle ne doit être activé.

Le frontend peut appeler ces routes avec `authorizedFetch` de `frontend/src/features/system/auth.ts`, préparer les états de chargement/erreur et poursuivre US-17. Le jeton reste en mémoire : un rechargement de page demande une nouvelle connexion. Le traitement des commentaires de US-17 reste dans le périmètre d'Oumaima.

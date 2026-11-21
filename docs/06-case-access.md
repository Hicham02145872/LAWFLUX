# Accès aux dossiers — US-10

`case-service` valide le jeton Keycloak transmis par le BFF ou `document-service`. Le sujet et le `cabinet_id` viennent du jeton signé ; ils ne sont jamais lus depuis la requête. Le RPC `CheckCaseAccess(case_id, action)` répond `allowed=false` pour un dossier absent, d'un autre cabinet, une action inconnue ou un accès révoqué. Un jeton absent ou invalide renvoie `UNAUTHENTICATED`. Une panne de la base ou de Keycloak ne donne aucun accès.

Dans Keycloak, définir `cabinet_id` dans **Realm settings → User profile** comme attribut éditable uniquement par l'administrateur (`view: [admin]`, `edit: [admin]`). Garder les attributs non gérés désactivés. Le mapper `cabinet-id` du client `lawflux-web` place cette valeur dans le jeton. Un utilisateur ne doit pas pouvoir choisir ou modifier son `cabinet_id` depuis son compte. Si le realm existe déjà, appliquer le mapper et ce profil dans la console Keycloak : l'import au démarrage ne remplace pas un realm existant.

| Rôle | Droit sur un dossier du même cabinet |
| --- | --- |
| `cabinet_manager` | Lecture et gestion de tous les dossiers du cabinet |
| `lawyer` | Lecture, écriture et partage documentaire si affectation `STAFF` active |
| `assistant` | Lecture et écriture documentaire si affectation `STAFF` active ; pas de partage |
| `client` | Lecture du dossier si autorisation `CLIENT` active ; téléchargement limité ensuite aux documents explicitement partagés |

Les actions reconnues sont `CASE_READ`, `DOCUMENT_READ`, `DOCUMENT_WRITE`, `DOCUMENT_SHARE`, `CASE_ASSIGN` et `CLIENT_PORTAL_READ`. Un dossier archivé reste lisible mais refuse les actions d'écriture et les nouvelles affectations. Les droits sont vérifiés côté serveur à chaque appel.
Le `document-service` transmet l'action précise à `CheckCaseAccess` : lecture pour les listes et téléchargements, écriture pour l'import et les versions, partage pour `SHARE` et `UNSHARE`.

La migration `V2__case_access.sql` crée `case_records` et `case_access_grants`. Une nouvelle affectation est idempotente ; une révocation ferme l'affectation active sans supprimer son historique. La table `case_records` fournit le socle minimal que les commandes de création de dossier US-09 devront alimenter.

Pour l'écran de Fatima `/dossiers/:id/acces`, le BFF expose :

| Méthode | Route | Corps ou réponse |
| --- | --- | --- |
| GET | `/api/dossiers/{id}/acces` | Liste `{userId, kind, grantedAt}` |
| POST | `/api/dossiers/{id}/acces` | Corps `{userId, kind}` avec `kind` = `STAFF` ou `CLIENT` ; réponse `{active:true}` |
| DELETE | `/api/dossiers/{id}/acces/{userId}?kind=STAFF` | Réponse `{active:false}` |

Ces trois routes exigent un jeton et le rôle `cabinet_manager` sur le dossier du même cabinet. Les fonctions React `listCaseGrants`, `grantCaseAccess` et `revokeCaseAccess` sont dans `frontend/src/features/dossiers/caseAccessApi.ts`. Le raccordement de l'écran et la liste des utilisateurs proposés relèvent encore de US-10 côté interface et de la gestion des membres US-04.

Exemple pour l'écran de Fatima :

```ts
const grants = await listCaseGrants(caseId)
await grantCaseAccess(caseId, selectedUserId, 'STAFF')
await revokeCaseAccess(caseId, selectedUserId, 'STAFF')
```

Pour une démonstration technique avant US-09, un dossier de test peut être inséré dans la base `lawflux_cases` avec un UUID de cabinet correspondant à l'attribut `cabinet_id` du compte Keycloak :

```sql
INSERT INTO case_records (id, cabinet_id, title, status)
VALUES ('11111111-1111-4111-8111-111111111111',
        '22222222-2222-4222-8222-222222222222',
        'Dossier de démonstration', 'OPEN');
```

Utiliser de vrais UUID de test cohérents avec les comptes créés localement. Aucun dossier de démonstration n'est inséré automatiquement dans un environnement partagé.

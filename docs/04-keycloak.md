# Keycloak dans LAWFLUX

Keycloak fournit l'authentification OpenID Connect. Le realm `lawflux` et le client public `lawflux-web` (Authorization Code + PKCE S256) sont importés au premier démarrage. L'interface d'administration locale est `http://localhost:8180`.

Avant le démarrage, ajouter `KEYCLOAK_ADMIN_PASSWORD` avec une valeur locale forte dans `.env` (voir `.env.example`). Puis :

```powershell
docker compose -f compose.yaml -f compose.load-balancing.yaml --profile apps up -d keycloak
```

La route React `/connexion` lance Authorization Code avec PKCE, puis `/connexion/retour` échange le code et appelle `GET /api/auth/me`. Le jeton est gardé en mémoire dans l'onglet et perdu au rechargement. Le BFF valide la signature, l'émetteur et l'expiration du JWT sur cette route. Il charge les clés publiques depuis Keycloak par son adresse Docker interne ; l'émetteur reste `http://localhost:8180/realms/lawflux` afin de correspondre aux jetons obtenus depuis le navigateur. L'endpoint renvoie `sub`, nom d'utilisateur, email et rôles du realm. Aucun mot de passe de compte de démonstration n'est livré dans le dépôt.

Dans l'interface Keycloak, créer les utilisateurs du cabinet et leur attribuer un rôle `cabinet_manager`, `lawyer`, `assistant` ou `client`. Les rôles du jeton servent à l'interface et aux contrôles généraux ; l'appartenance au cabinet et l'accès à un dossier restent des données et vérifications LAWFLUX. Ne pas déduire `cabinet_id` d'un champ fourni par le navigateur.

Le service de documents valide les jetons sur ses RPC lorsque `DOCUMENTS_BUSINESS_ENABLED=true`. Il exige un `cabinet_id` dans le jeton, vérifie les rôles et appelle `CheckCaseAccess` avec le jeton d'origine. Le BFF expose les routes REST documentées dans [05-documents-integration.md](05-documents-integration.md). Le contrôle du dossier et ses affectations sont décrits dans [06-case-access.md](06-case-access.md). L'authentification des autres RPC métier et la conservation de session au rechargement restent à intégrer.

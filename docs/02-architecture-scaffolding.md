# LAWFLUX — Architecture cible du scaffolding

## Choix proposés

- Spring Boot pour le BFF, les quatre services métier et le worker.
- Maven multi-module et Java LTS ; versions précises à fixer après vérification de compatibilité Spring/gRPC et de l'environnement.
- React pour le portail, REST/JSON entre navigateur et BFF, gRPC pour les appels synchrones internes.
- RabbitMQ pour les événements asynchrones, indépendamment de gRPC.
- PostgreSQL avec une base et un utilisateur propres à chaque service persistant ; un serveur local peut héberger ces bases.
- Stockage privé compatible S3 ou volume dédié pour les fichiers. Le BFF peut relayer l'upload HTTP en streaming ; éviter d'encoder les fichiers en base64 dans les messages protobuf.
- Docker Compose pour reproduire l'environnement local.

Il n'est pas nécessaire d'ajouter Eureka, Kubernetes ou un serveur de configuration pour ce prototype. La résolution DNS de Compose suffit au démarrage local.

## Services et propriété des données

| Composant | Données possédées | Responsable |
|---|---|---|
| gateway-bff | Aucune donnée métier ; exposition REST et composition des réponses | P1, routes métier contribuées par P2/P3 |
| identity-service | Cabinets, utilisateurs, rôles, appartenances et sessions/jetons | P1 |
| case-service | Clients, dossiers, affectations, autorisations dossier, tâches, historique et commentaires de dossier | P2 |
| document-service | Métadonnées, versions, clés de stockage, partages, commentaires de document et risques manuels | P3 |
| billing-service | Temps, tarifs, brouillons, factures, versions/avoirs simulés et paiement déclaré | P3 |
| notification-worker | Notifications applicatives et suivi des événements consommés | P1 |

Les services se référencent par identifiants ; aucune jointure SQL directe entre bases. Le portail client est une vue React avec permissions dédiées, pas un microservice supplémentaire. Le tableau de bord est composé au BFF à partir d'indicateurs autorisés.

## Contrats à préparer

| Fichier proto envisagé | API interne | Producteur / consommateurs |
|---|---|---|
| identity/v1/identity.proto | Identité et appartenances, administration cabinet/membres | P1 / BFF |
| cases/v1/cases.proto | Clients, dossiers, affectations et CheckCaseAccess | P2 / BFF, Documents, Facturation |
| cases/v1/tasks.proto | Tâches, échéances et indicateurs | P2 / BFF |
| documents/v1/documents.proto | Métadonnées, versions, partage et commentaires | P3 / BFF |
| billing/v1/billing.proto | Temps, tarifs, factures et indicateurs | P3 / BFF |
| notifications/v1/notifications.proto | Consultation et lecture des notifications | P1 / BFF |

Ce tableau décrit des contrats cibles, pas des fichiers générés. Prévoir packages versionnés, pagination, identifiants, timestamps, montants en unités mineures avec devise explicite et erreurs gRPC traduites en réponses HTTP cohérentes. Ne pas partager d'entités JPA entre services.

Événements initiaux : dossier.created, document.shared et task.due_soon. Enveloppe commune proposée : event_id, event_type, schema_version, occurred_at, tenant_id, actor_id, correlation_id et payload minimal. Les messages ne doivent pas exposer le contenu confidentiel des documents.

## Sécurité à intégrer dès le socle

Le contexte de cabinet provient d'une identité vérifiée et de son appartenance au cabinet sélectionné. Ne jamais faire confiance à un tenant_id librement envoyé par le navigateur. Vérifier signature, durée de validité et claims des jetons dans chaque service ; propager l'identité dans les métadonnées gRPC avec un contexte de confiance explicite.

RBAC seul ne suffit pas : contrôler aussi l'affectation de l'avocat, les autorisations de cabinet et les partages explicites du client. Documents et Facturation consultent les droits dossier via gRPC lorsque nécessaire. Si cette vérification échoue ou si son service est indisponible, refuser l'accès métier plutôt que l'autoriser implicitement.

L'administrateur de plateforme n'a pas d'accès métier par défaut. Documents privés, validations d'upload, hachage des mots de passe, jetons limités dans le temps, audit et suppression logique font partie du périmètre. Le mécanisme de déconnexion doit être défini avec la stratégie de sessions/refresh tokens ; effacer seulement le jeton du navigateur ne révoque pas un jeton encore valide.

En local, ne pas publier PostgreSQL, RabbitMQ ou les ports gRPC sur toutes les interfaces. Prévoir TLS pour les accès réels et un transport interne authentifié. Les secrets réels ne vont pas dans Git.

## Résilience et exploitation

- Délais explicites pour chaque appel gRPC ; tentatives limitées aux opérations sûres ou munies d'une clé d'idempotence.
- Worker à accusé de réception après persistance, notifications dédupliquées par event_id et destinataire, tentatives bornées et DLQ.
- Le scénario D demande au minimum une file durable et des messages persistants : l'arrêt du consommateur ne doit pas perdre les événements.
- La panne du broker pendant une écriture constitue un cas distinct. Pour la couvrir, proposer une outbox transactionnelle dans les services producteurs. Le PDF la place en extension : décider explicitement de l'inclure ou documenter cette limite ; ne pas promettre une livraison garantie avec une simple double écriture base/broker.
- Le service Dossiers produit les rappels d'échéance, le worker les transforme en notifications. Le worker n'interroge pas directement la base Dossiers.
- Health checks, logs structurés, correlation_id propagé dans HTTP/gRPC/événements et métriques de latence/erreur.
- Migrations par service et jeux de données fictives comprenant deux cabinets.

## Arborescence prévue

```text
LAWFLUX/
├── pom.xml
├── contracts/
│   ├── pom.xml
│   └── src/main/proto/lawflux/{identity,cases,documents,billing,notifications}/v1/
├── services/
│   ├── gateway-bff/
│   ├── identity-service/
│   ├── case-service/
│   ├── document-service/
│   ├── billing-service/
│   └── notification-worker/
├── frontend/
├── infra/
│   ├── compose.yaml
│   ├── postgres/
│   └── rabbitmq/
├── tests/
│   ├── acceptance/
│   └── load/
├── .github/workflows/
└── docs/
```

Chaque service métier contiendra ses couches API/adaptateurs, application, domaine et infrastructure, ses migrations et ses tests. Le module contracts génère les messages et stubs gRPC ; il ne contient aucune logique métier.

## Critères de fin du prochain scaffolding

1. Le build racine compile les stubs protobuf et tous les modules.
2. Compose démarre PostgreSQL, RabbitMQ, le stockage et les applications avec health checks.
3. Un appel REST du BFF atteint un service via gRPC et propage son identifiant de corrélation.
4. Un événement de démonstration est persisté puis consommé, y compris après arrêt/reprise du worker.
5. Les paramètres sont externalisés, les secrets sont absents de Git, et les migrations appartiennent à leur service.
6. La CI exécute les vérifications disponibles et le README décrit les commandes et prérequis exacts.

Ces critères ne sont pas encore réalisés : cette première étape livre l'analyse, l'organisation et la cible technique avant la génération du code.

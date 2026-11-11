# LAWFLUX

Scaffolding Spring Boot / gRPC pour le SaaS multi-cabinet LAWFLUX. Ce dépôt contient le socle technique ; les fonctionnalités métier décrites dans le cahier des charges restent à développer.

## Stack et modules

Java 21, Spring Boot 3.5.14, Spring gRPC 0.12.0, gRPC 1.76.0, Protobuf 4.32.1 et Maven Wrapper 3.9.11.

| Module | Rôle | HTTP local | gRPC local |
|---|---|---:|---:|
| `contracts` | Contrats `.proto` et génération des stubs | — | — |
| `platform-common` | Configuration technique, corrélation, limites de sécurité du scaffold | — | — |
| `gateway-bff` | REST vers gRPC ; point d’entrée du futur portail | 8080 | client |
| `identity-service` | Futurs cabinets, membres, rôles et sessions | 8081 | 9091 |
| `case-service` | Futurs clients, dossiers, affectations et tâches | 8082 | 9092 |
| `document-service` | Futurs documents, versions et partages | 8083 | 9093 |
| `billing-service` | Futurs temps, tarifs et factures | 8084 | 9094 |
| `notification-worker` | Consommation RabbitMQ ; future gestion des notifications | 8085 | — |

Les services utilisent `ma.lawflux` et sont organisés en `api`, `application`, `domain` et `infrastructure`. Le module partagé ne contient aucune entité métier. Chaque service persistant possède sa base, son utilisateur PostgreSQL et ses migrations Flyway.

## Prérequis

- JDK 21 ou compatible, `JAVA_HOME` configuré.
- Docker Desktop avec moteur Linux lancé et Docker Compose v2 pour l’environnement complet.
- Premier build avec accès à Maven Central. Maven n’a pas besoin d’être installé : le wrapper est fourni.

## Compiler et vérifier

Depuis la racine sous PowerShell :

```powershell
.\mvnw.cmd -B -ntp verify
```

Linux/macOS :

```bash
chmod +x mvnw
./mvnw -B -ntp verify
```

Les sources protobuf sont générées dans `contracts/target/generated-sources/protobuf`. Importer le **pom.xml racine** dans IntelliJ et sélectionner le JDK 21.

## Démarrer tout avec Docker

```powershell
Copy-Item .env.example .env
# Choisir les mots de passe dans .env avant le démarrage.
docker compose --profile apps up --build -d
docker compose --profile apps ps
```

Seul le BFF est exposé pour les applications conteneurisées. PostgreSQL et RabbitMQ sont accessibles sur l’interface locale pour le développement ; les ports gRPC restent sur le réseau Compose. Les fichiers sont conservés dans le volume privé `document_data` ; l’API d’upload reste à implémenter.

Les bases et utilisateurs sont créés au premier démarrage du volume PostgreSQL. Modifier les mots de passe de `.env` ne modifie pas les rôles d’un volume déjà initialisé : effectuer la rotation dans PostgreSQL. Les commandes de démarrage ne suppriment aucun volume.

## Démarrer les applications dans l’IDE

```powershell
docker compose up -d postgres rabbitmq
.\mvnw.cmd -B -ntp install -DskipTests
```

Lancer les classes `*Application` dans des configurations distinctes. Pour chaque service persistant, définir `DB_PASSWORD` avec la valeur correspondante de `.env`. Le BFF et le worker demandent `RABBITMQ_PASSWORD`. Les identifiants de base et ports par défaut sont dans chaque `application.yml`. Les variables de `.env` sont chargées par Compose, pas automatiquement par Java ou l’IDE.

Préférer Compose si les ports gRPC doivent rester internes. Avec Spring gRPC 0.12, le serveur Netty lancé directement peut écouter sur toutes les interfaces malgré le paramètre d’hôte.

Exemple pour Dossiers après installation des modules :

```powershell
$env:DB_PASSWORD = '<valeur de CASES_DB_PASSWORD>'
.\mvnw.cmd -pl services/case-service spring-boot:run
```

## Vérifier REST vers gRPC

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8080/api/system/services/cases -Headers @{ 'X-Correlation-Id' = 'demo-42' }
```

Noms disponibles : `identity`, `cases`, `documents`, `billing`. Réponse attendue : `service`, `state: SCAFFOLD_READY`, `correlationId`. Ce diagnostic ne lit aucune donnée métier. Service absent : 503 ou 504 selon la cause ; nom inconnu : 404.

Juste après le démarrage, répéter la sonde si le premier appel expire pendant l’établissement de la connexion gRPC.

## Vérifier la sonde RabbitMQ

La sonde est désactivée par défaut. Pour ce test local, mettre `SCAFFOLD_EVENTS_ENABLED=true` dans `.env`, puis recréer le BFF avec `docker compose --profile apps up -d gateway-bff`. Cet endpoint ne publie que des messages techniques sans donnée client.

```powershell
docker compose --profile apps stop notification-worker
$event = Invoke-RestMethod -Method Post http://localhost:8080/api/system/events
Invoke-RestMethod http://localhost:8080/api/system/services/cases
docker compose --profile apps start notification-worker
docker compose --profile apps logs --tail 30 notification-worker
docker compose exec postgres psql -U lawflux_admin -d lawflux_notifications -c 'SELECT * FROM scaffold_event_receipt;'
```

Le message confirmé par le broker attend dans une file durable et le worker le persiste après reprise. Une contrainte unique sur `event_id` évite les doublons. Erreurs de consommation : trois tentatives puis DLQ `lawflux.scaffold.notifications.dlq`. Les messages invalides sont rejetés. Cette sonde ne constitue pas encore les notifications métier ni une garantie transactionnelle base/broker : les événements `dossier.created`, `document.shared`, `task.due_soon` et l’arbitrage outbox restent au backlog.

## Sécurité et fonctionnalités restantes

HTTP : seuls les health checks et diagnostics techniques sont ouverts ; le reste est refusé. gRPC : seul `ServiceProbe/Ping` est autorisé ; les appels métier sont refusés. Les contrats métier sont réservés et ne renvoient pas de données fictives. Aucun login, jeton de démonstration ou tenant fourni par le navigateur n’est considéré comme une authentification.

À implémenter : identité/JWT, RBAC, contexte cabinet vérifié, droits dossier, modèles et migrations métier, CRUD, stockage avec validation d’upload, notifications métier, factures/PDF et interfaces React. Ajouter ensuite la vérification d’identité dans chaque service avant d’ouvrir les API. Ce réseau local en plaintext n’est pas une configuration de production ; TLS et authentification interservices restent à mettre en place.

Les health checks et tests du scaffold ne prouvent pas la recette métier A–E ni l’isolation de données qui n’existent pas encore.

## Travail de l’équipe

- Chef de projet : socle, backend, intégrations, sécurité, calculs et traitements complexes — [user stories](docs/user-stories/P1-chef-de-projet.md).
- Fatima : interfaces clients, dossiers, tâches et tableau de bord — [user stories et écrans](docs/user-stories/P2-dossiers-collaboration.md).
- Oumaima : interfaces documents, temps et factures — [user stories et écrans](docs/user-stories/P3-documents-facturation.md).

Le dossier `frontend` documente les points d’entrée prévus ; l’application React n’est pas générée dans cette étape Spring. Les anciens documents de cadrage décrivent la cible ; le présent README décrit ce qui est réellement présent.

## Références techniques

- [Spring Boot 3.5 — prérequis](https://docs.spring.io/spring-boot/3.5/system-requirements.html).
- [Spring gRPC 0.12 — compatibilité et intégration](https://github.com/spring-projects/spring-grpc/blob/v0.12.0/spring-grpc-docs/src/main/antora/modules/ROOT/pages/getting-started.adoc).

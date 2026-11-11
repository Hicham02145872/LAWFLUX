# LAWFLUX — Cadrage et répartition pour trois personnes

## Base de travail

Analyse du cahier des charges version 1.0 du 3 novembre 2026, lu intégralement (8 pages). Ce document prépare le scaffolding ; aucun service applicatif n'est encore implémenté.

La demande du chef de projet ajoute Spring Boot et gRPC. Le PDF propose REST pour les échanges synchrones : le passage à gRPC en interne est donc une adaptation technique proposée, à faire valider dans le cadrage académique. La répartition ci-dessous est une proposition de travail, et non une modification du PDF source.

## Périmètre à conserver

SaaS multi-cabinet pour cabinets d'avocats marocains, interface française, données de démonstration fictives.

| Bloc | Contenu attendu |
|---|---|
| F01 — Identité | Cabinets, utilisateurs, connexion/déconnexion, changement de mot de passe, rôles et isolation par cabinet |
| F02 — Clients et dossiers | Création, modification, archivage, affectations, statuts, recherche et historique |
| F03 — Documents | PDF/DOCX, contrôle de taille/type, versions, téléchargements autorisés, commentaires et risques saisis manuellement |
| F04 — Collaboration | Commentaires internes/partagés, tâches, responsable, priorité, échéance et notifications applicatives |
| F05 — Facturation | Temps manuel/minuteur, corrections auditées, tarifs, brouillon, validation humaine, PDF et statut payé/non payé |
| F06 — Tableaux de bord | Dossiers par statut, retards, heures et impayés, filtrés par rôle et cabinet |

Hors périmètre : IA, analyse automatique de clauses, conseil juridique automatique, paiement bancaire réel, signature qualifiée. Messagerie temps réel, OCR, calendrier externe et interface arabe restent des extensions.

## Répartition proposée

Le chef de projet est **la personne 1**. Les identifiants P2/P3 seront remplacés par les noms de vos collègues. Les trois personnes développent, testent et documentent leur périmètre.

| Personne | Responsabilité principale | Backend et infrastructure | Frontend | Preuve de livraison |
|---|---|---|---|---|
| **P1 — Toi, chef de projet** | Architecture, identité, sécurité et intégration | Socle Maven, contrats communs, BFF, service identité, RBAC, Docker Compose, CI, RabbitMQ et worker notifications | Connexion, changement de mot de passe, administration cabinet/membres, navigation commune et centre de notifications | Scénario A côté identité ; scénario D ; tests d'isolation transversaux avec P2/P3 |
| **P2 — Dossiers et collaboration** | Fonctionnement du cabinet et accès avocat/client | Clients, dossiers, affectations, permissions dossier, tâches, historique et commentaires de dossier | Écrans clients/dossiers/tâches, portail avocat/client et assemblage du tableau de bord | Scénario A côté dossiers ; permissions d'affectation et de partage ; tâches et retards |
| **P3 — Documents et facturation** | Pièces et suivi financier | Uploads, stockage, versions, commentaires de document, risques manuels, temps, tarifs, factures et PDF | Écrans documents, versions, annotations manuelles, temps et factures | Scénarios B et C ; tests d'accès fichier et de facture figée |

### Arbitrage de charge

Le PDF confie également le worker notifications au membre documents/finances. Ici, P1 le reprend pour alléger P3, qui possède deux services métier. P1 prépare l'infrastructure et les conventions ; chaque propriétaire implémente ses événements et ses endpoints BFF associés. P2 assemble le tableau de bord à partir des indicateurs livrés par chaque propriétaire. P3 n'a donc pas à réaliser seul le frontend ou les tests d'intégration.

Si la charge dépasse les capacités, réduire d'abord la richesse des écrans et les options avancées. Ne pas retirer l'isolation des cabinets, les contrôles d'accès ou les scénarios de recette.

## Ton rôle de chef de projet

- Maintenir un backlog priorisé avec un responsable, une échéance et des critères d'acceptation par ticket.
- Fixer les contrats REST, protobuf et événements avant les développements dépendants ; faire relire chaque changement par le service consommateur.
- Organiser deux points courts par semaine et une démonstration intégrée hebdomadaire.
- Suivre les blocages et arbitrer le périmètre ; tu n'es pas responsable de coder les fonctionnalités de tout le monde.
- Intégrer régulièrement les branches, maintenir Compose et CI, coordonner la recette et la soutenance.
- Faire relire tes propres PR par P2 ou P3. Toute PR doit être revue par une autre personne.

## Planning aligné sur le PDF

| Dates 2026 | Résultat attendu | Responsables |
|---|---|---|
| 7–15 novembre | Architecture, schémas par service, contrats API/proto/événements, maquettes et socle démarrable | P1 coordonne ; chacun définit son domaine |
| 16–29 novembre | Parcours intégré identité → dossier → document ; CI et isolation vérifiées | Tous, par périmètre |
| 30 novembre–6 décembre | Tâches, temps, facture PDF, notifications et tableau de bord minimal | Tous, par périmètre |
| 7–12 décembre | Recette A–E, panne simulée, charge, rapport de 20 pages maximum et présentation PDF | Tous ; P1 coordonne |
| 13 décembre | Dépôt GitHub et remise Classroom selon le support de l'enseignant | P1 vérifie la remise, tous vérifient les livrables |

Le jalon de validation du sujet annoncé « avant le 06/11 » dans le PDF est déjà passé au 7 novembre : son statut reste à vérifier, sans supposer qu'il a été validé.

## Backlog initial

| ID | Responsable | Travail | Dépendance | Acceptation |
|---|---|---|---|---|
| SOC-01 | P1 | Générer le monorepo, modules, configuration et Compose | Architecture | Build racine et démarrage documentés |
| API-01 | Tous, pilotage P1 | Définir REST, protobuf, erreurs et événements | Cadrage | Contrats relus par producteurs et consommateurs |
| IAM-01 | P1 | Cabinet, membres, authentification, déconnexion et mot de passe | SOC-01 | Responsable, avocat et client de démonstration utilisables |
| SEC-01 | P1 + P2/P3 | Identité propagée, RBAC et isolation tenant | IAM-01 | Requêtes inter-cabinets refusées sur API et fichiers |
| DOS-01 | P2 | Clients, dossiers, statuts, archivage et recherche | API-01 | CRUD autorisé, historique et affectation opérationnels |
| DOS-02 | P2 | Permissions dossier et accès client explicite | DOS-01 | Avocat non affecté et client non autorisé refusés |
| DOC-01 | P3 | Upload, métadonnées, versions et téléchargement privé | DOS-02 | Type/taille contrôlés ; version et auteur conservés |
| DOC-02 | P3 | Partage, commentaires, risques manuels | DOC-01 | Client voit seulement les éléments partagés |
| COL-01 | P2 | Tâches, priorités, responsables et échéances | DOS-01 | Retards identifiables et droits vérifiés |
| BIL-01 | P3 | Temps manuel/minuteur, corrections auditées et tarifs | DOS-02 | Deux heures enregistrées et montant calculable |
| BIL-02 | P3 | Brouillon, validation, PDF et état de paiement | BIL-01 | Facture validée figée, correction par version/avoir simulé |
| EVT-01 | P1 + producteurs P2/P3 | Événements et notifications persistantes | API-01 | dossier.created, document.shared et rappel d'échéance traités sans doublon visible |
| UI-01 | Tous | Écrans de chaque périmètre | Contrats | Parcours utilisables en français avec états chargement/erreur |
| DASH-01 | P2 + indicateurs P1/P3 | Tableau de bord | DOS/COL/BIL | Statuts, retards, heures et impayés limités aux droits |
| QA-01 | Tous, pilotage P1 | Recette, panne et charge | Parcours intégrés | Résultats reproductibles et limites documentées |
| LIV-01 | Tous, pilotage P1 | README, rapport, présentation et démo | QA-01 | Livrables présents et procédure de démarrage vérifiée |

## Recette et responsabilités

| Scénario du PDF | Pilote | Vérification |
|---|---|---|
| A — Cabinet, avocat, client et dossier affecté | P1 + P2 | Identité créée, affectation et accès corrects |
| B — Version, commentaire et partage d'un document | P3 + P2 | Client limité aux dossiers et documents autorisés |
| C — Deux heures et facture PDF | P3 | Tarif appliqué, validation humaine et PDF cohérent |
| D — Arrêt puis reprise du worker | P1 | Dossiers disponibles ; notifications traitées après reprise |
| E — Deux cabinets isolés | Tous | Lectures et écritures croisées refusées, y compris via gRPC et fichiers |

Objectifs mesurables : scénarios critiques exécutables de bout en bout ; aucune lecture inter-cabinet dans les tests ; P95 cible inférieur à une seconde pour consultation de dossier en local hors transfert de fichiers. Ce sont des cibles à mesurer, pas des résultats acquis.

Une tâche est terminée quand son code est intégré après revue, ses tests utiles passent, son écran et son API sont raccordés si applicables, ses droits sont vérifiés et sa documentation est à jour.

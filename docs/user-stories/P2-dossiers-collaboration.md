# Fatima — Interfaces clients, dossiers, collaboration et tableau de bord

Périmètre : composants React, listes, formulaires, affichage de résultats et vérifications manuelles. Utiliser le modèle de page, les composants, les hooks et les fonctions API préparés par le chef de projet.

Le chef prend en charge les bases de données, le backend, gRPC, la sécurité, les règles de permissions, les calculs et les traitements complexes. Tu affiches les données et les actions autorisées renvoyées par le serveur. Commencer avec les données fictives fournies, puis remplacer par le hook préparé ; signaler au chef tout contrat manquant.

Les routes proposées ci-dessous servent à identifier les écrans à développer. Cette répartition remplace les anciennes affectations.

## US-08 — Clients — interface

En tant que responsable de cabinet, je veux créer, modifier et archiver mes clients afin de centraliser les informations du cabinet.

**Écrans à développer**

- Liste des clients ; formulaire créer/modifier ; fiche client.
- Routes proposées : /clients ; /clients/nouveau ; /clients/:id.

**Sous-stories / tâches**

- [ ] **US-08.UI-1** — Afficher nom, coordonnées et état actif/archivé dans un tableau.
- [ ] **US-08.UI-2** — Créer les champs et les messages requis du formulaire fourni.
- [ ] **US-08.UI-3** — Brancher lister/créer/modifier/archiver avec les fonctions API préparées par le chef.
- [ ] **US-08.UI-4** — Vérifier avec la fiche de test du chef : affichage, champs requis, chargement, liste vide et message d’erreur ; joindre une capture et remonter les anomalies.

**Préparé par le chef :** Fournir les endpoints sécurisés de liste, création, modification et archivage des clients. Les composants partagés et les appels API sont également fournis par le chef.

## US-09 — Dossiers — interface

En tant que responsable de cabinet, je veux créer, modifier et archiver un dossier afin de suivre les affaires du cabinet.

**Écrans à développer**

- Liste et fiche dossier ; formulaire créer/modifier.
- Routes proposées : /dossiers ; /dossiers/nouveau ; /dossiers/:id.

**Sous-stories / tâches**

- [ ] **US-09.UI-1** — Afficher titre, client et statut avec les composants de tableau fournis.
- [ ] **US-09.UI-2** — Créer formulaire dossier et liste de statuts à partir des options fournies.
- [ ] **US-09.UI-3** — Relier enregistrer/archiver et afficher le résultat renvoyé par l’API.
- [ ] **US-09.UI-4** — Vérifier avec la fiche de test du chef : affichage, champs requis, chargement, liste vide et message d’erreur ; joindre une capture et remonter les anomalies.

**Préparé par le chef :** Implémenter les règles de statuts et de référence client, les transactions et les APIs dossier. Les composants partagés et les appels API sont également fournis par le chef.

## US-10 — Dossiers — interface

En tant que responsable de cabinet, je veux affecter les avocats et définir les accès dossier afin de contrôler qui peut consulter une affaire.

**Écrans à développer**

- Section affectations et accès client de la fiche dossier.
- Routes proposées : /dossiers/:id/acces.

**Sous-stories / tâches**

- [ ] **US-10.UI-1** — Afficher les avocats et clients proposés par l’API.
- [ ] **US-10.UI-2** — Ajouter une sélection d’avocat et une case de partage explicite au client.
- [ ] **US-10.UI-3** — Appeler les fonctions affecter/retirer/autoriser fournies, sans écrire les règles de permission.
- [ ] **US-10.UI-4** — Vérifier avec la fiche de test du chef : affichage, champs requis, chargement, liste vide et message d’erreur ; joindre une capture et remonter les anomalies.

**Préparé par le chef :** Concevoir et implémenter CheckCaseAccess, affectations et autorisations explicites ; aucun code de permission à déléguer. Les composants partagés et les appels API sont également fournis par le chef.

## US-11 — Dossiers — interface

En tant qu’avocat, je veux rechercher et filtrer mes dossiers autorisés afin de retrouver rapidement une affaire.

**Écrans à développer**

- Barre de recherche et filtres de la liste des dossiers.
- Routes proposées : /dossiers.

**Sous-stories / tâches**

- [ ] **US-11.UI-1** — Ajouter un champ de recherche et un filtre de statut.
- [ ] **US-11.UI-2** — Relier recherche, réinitialisation et boutons précédent/suivant au hook fourni.
- [ ] **US-11.UI-3** — Afficher seulement les résultats et la pagination renvoyés par le serveur.
- [ ] **US-11.UI-4** — Vérifier avec la fiche de test du chef : affichage, champs requis, chargement, liste vide et message d’erreur ; joindre une capture et remonter les anomalies.

**Préparé par le chef :** Implémenter recherche, pagination, index et filtrage serveur des résultats autorisés. Les composants partagés et les appels API sont également fournis par le chef.

## US-12 — Dossiers — interface

En tant qu’avocat autorisé, je veux consulter l’historique d’un dossier afin de comprendre les changements effectués.

**Écrans à développer**

- Onglet historique du dossier.
- Routes proposées : /dossiers/:id/historique.

**Sous-stories / tâches**

- [ ] **US-12.UI-1** — Afficher une liste chronologique avec action, auteur et date fournis.
- [ ] **US-12.UI-2** — Ajouter une présentation lisible des changements et des dates.
- [ ] **US-12.UI-3** — Brancher le chargement de l’historique avec la fonction préparée.
- [ ] **US-12.UI-4** — Vérifier avec la fiche de test du chef : affichage, champs requis, chargement, liste vide et message d’erreur ; joindre une capture et remonter les anomalies.

**Préparé par le chef :** Implémenter la collecte et la consultation de l’historique append-only. Les composants partagés et les appels API sont également fournis par le chef.

## US-13 — Portail client — interface

En tant que client, je veux consulter uniquement mes dossiers et pièces autorisés afin de suivre mes affaires en toute confidentialité.

**Écrans à développer**

- Portail client : liste et détail des dossiers autorisés.
- Routes proposées : /portail/dossiers ; /portail/dossiers/:id.

**Sous-stories / tâches**

- [ ] **US-13.UI-1** — Créer une liste des dossiers renvoyés pour le client connecté.
- [ ] **US-13.UI-2** — Afficher statut et pièces partagées dans une fiche en lecture seule.
- [ ] **US-13.UI-3** — Utiliser le composant de liste des pièces d’Oumaima et les liens sécurisés fournis.
- [ ] **US-13.UI-4** — Vérifier avec la fiche de test du chef : affichage, champs requis, chargement, liste vide et message d’erreur ; joindre une capture et remonter les anomalies.

**Préparé par le chef :** Composer les données du portail et garantir les permissions côté services et BFF. Les composants partagés et les appels API sont également fournis par le chef.

## US-19 — Collaboration — interface

En tant qu’avocat ou client autorisé, je veux commenter un dossier avec une visibilité adaptée afin de collaborer sans exposer les échanges internes.

**Écrans à développer**

- Onglet commentaires du dossier.
- Routes proposées : /dossiers/:id/commentaires ; section commentaires du portail client.

**Sous-stories / tâches**

- [ ] **US-19.UI-1** — Afficher les commentaires déjà filtrés avec auteur, date et badge de visibilité.
- [ ] **US-19.UI-2** — Créer un champ de commentaire et un choix de visibilité selon les options fournies.
- [ ] **US-19.UI-3** — Brancher envoyer et rafraîchir avec les fonctions API préparées.
- [ ] **US-19.UI-4** — Vérifier avec la fiche de test du chef : affichage, champs requis, chargement, liste vide et message d’erreur ; joindre une capture et remonter les anomalies.

**Préparé par le chef :** Implémenter visibilité interne/partagée et droits de commentaire dossier côté serveur. Les composants partagés et les appels API sont également fournis par le chef.

## US-20 — Tâches — interface

En tant qu’avocat, je veux créer et suivre les tâches d’un dossier afin de respecter les échéances.

**Écrans à développer**

- Liste et formulaire des tâches.
- Routes proposées : /taches ; /taches/nouvelle ; section tâches du dossier.

**Sous-stories / tâches**

- [ ] **US-20.UI-1** — Afficher titre, responsable, priorité, échéance et état fournis.
- [ ] **US-20.UI-2** — Créer formulaire tâche avec sélection de responsable et date.
- [ ] **US-20.UI-3** — Afficher le badge de retard calculé par le serveur et brancher les changements d’état.
- [ ] **US-20.UI-4** — Vérifier avec la fiche de test du chef : affichage, champs requis, chargement, liste vide et message d’erreur ; joindre une capture et remonter les anomalies.

**Préparé par le chef :** Implémenter responsable autorisé, échéances, retards et événements de rappel. Les composants partagés et les appels API sont également fournis par le chef.

## US-28 — Tableau de bord — interface

En tant que responsable ou avocat, je veux consulter mes indicateurs autorisés afin de piloter les dossiers et l’activité.

**Écrans à développer**

- Tableau de bord du cabinet ou de l’avocat.
- Routes proposées : /tableau-de-bord.

**Sous-stories / tâches**

- [ ] **US-28.UI-1** — Afficher quatre cartes : dossiers par statut, tâches en retard, heures et impayés.
- [ ] **US-28.UI-2** — Utiliser les valeurs du endpoint de synthèse sans agréger plusieurs services.
- [ ] **US-28.UI-3** — Ajouter des liens vers les listes et des messages si un indicateur est indisponible.
- [ ] **US-28.UI-4** — Vérifier avec la fiche de test du chef : affichage, champs requis, chargement, liste vide et message d’erreur ; joindre une capture et remonter les anomalies.

**Préparé par le chef :** Implémenter les agrégations backend et la composition BFF avec dégradation contrôlée et filtrage par droits. Les composants partagés et les appels API sont également fournis par le chef.

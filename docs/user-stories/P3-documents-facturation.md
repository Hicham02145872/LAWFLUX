# Oumaima — Interfaces documents, temps manuel et affichage des factures

Périmètre : composants React, listes, formulaires, affichage de résultats et vérifications manuelles. Utiliser le modèle de page, les composants, les hooks et les fonctions API préparés par le chef de projet.

Le chef prend en charge les bases de données, le backend, gRPC, la sécurité, les règles de permissions, les calculs et les traitements complexes. Tu affiches les données et les actions autorisées renvoyées par le serveur. Commencer avec les données fictives fournies, puis remplacer par le hook préparé ; signaler au chef tout contrat manquant.

Les routes proposées ci-dessous servent à identifier les écrans à développer. Cette répartition remplace les anciennes affectations.

## US-14 — Documents — interface

En tant qu’avocat autorisé, je veux importer un document PDF ou DOCX afin de centraliser les pièces du dossier.

**Écrans à développer**

- Liste des documents ; formulaire d’import.
- Routes proposées : /dossiers/:id/documents.

**Sous-stories / tâches**

- [ ] **US-14.UI-1** — Afficher nom, type, auteur et date des documents renvoyés.
- [ ] **US-14.UI-2** — Créer un champ de sélection PDF/DOCX et un bouton Importer.
- [ ] **US-14.UI-3** — Utiliser la fonction d’upload fournie avec ses états de progression et erreurs.
- [ ] **US-14.UI-4** — Vérifier avec la fiche de test du chef : affichage, champs requis, chargement, liste vide et message d’erreur ; joindre une capture et remonter les anomalies.

**Préparé par le chef :** Implémenter l’upload, la validation serveur du contenu/type/taille et le stockage privé avec gestion des échecs. Les composants partagés et les appels API sont également fournis par le chef.

## US-15 — Documents — interface

En tant qu’avocat autorisé, je veux ajouter et consulter les versions d’un document afin de conserver la traçabilité des pièces.

**Écrans à développer**

- Onglet versions d’un document.
- Routes proposées : /documents/:id/versions.

**Sous-stories / tâches**

- [ ] **US-15.UI-1** — Afficher numéro, auteur et date dans une liste de versions.
- [ ] **US-15.UI-2** — Ajouter le champ fichier et le bouton Ajouter une version.
- [ ] **US-15.UI-3** — Appeler la fonction fournie et recharger la liste après réussite.
- [ ] **US-15.UI-4** — Vérifier avec la fiche de test du chef : affichage, champs requis, chargement, liste vide et message d’erreur ; joindre une capture et remonter les anomalies.

**Préparé par le chef :** Implémenter numérotation et concurrence des versions sans écrasement. Les composants partagés et les appels API sont également fournis par le chef.

## US-16 — Documents — interface

En tant qu’avocat autorisé, je veux partager une pièce au client et contrôler son téléchargement afin de transmettre les documents autorisés.

**Écrans à développer**

- Section partage et téléchargement du document.
- Routes proposées : /documents/:id/partage ; liste des pièces du portail client.

**Sous-stories / tâches**

- [ ] **US-16.UI-1** — Afficher l’état partagé/non partagé renvoyé par le backend.
- [ ] **US-16.UI-2** — Ajouter boutons partager, retirer le partage et télécharger selon les actions autorisées fournies.
- [ ] **US-16.UI-3** — Utiliser les fonctions de partage et téléchargement sécurisé sans créer d’URL publique.
- [ ] **US-16.UI-4** — Vérifier avec la fiche de test du chef : affichage, champs requis, chargement, liste vide et message d’erreur ; joindre une capture et remonter les anomalies.

**Préparé par le chef :** Implémenter partage/révocation, contrôle des droits et téléchargement privé ; ne jamais exposer une clé de stockage publique. Les composants partagés et les appels API sont également fournis par le chef.

## US-17 — Documents — interface

En tant qu’avocat ou client autorisé, je veux commenter une pièce accessible afin d’échanger dans le contexte du document.

**Écrans à développer**

- Onglet commentaires du document.
- Routes proposées : /documents/:id/commentaires.

**Sous-stories / tâches**

- [ ] **US-17.UI-1** — Afficher la liste reçue avec auteur, date et visibilité.
- [ ] **US-17.UI-2** — Ajouter un champ de texte et les options de visibilité autorisées.
- [ ] **US-17.UI-3** — Relier envoyer et actualiser au client API fourni.
- [ ] **US-17.UI-4** — Vérifier avec la fiche de test du chef : affichage, champs requis, chargement, liste vide et message d’erreur ; joindre une capture et remonter les anomalies.

**Préparé par le chef :** Implémenter règles de visibilité et accès aux commentaires document côté serveur. Les composants partagés et les appels API sont également fournis par le chef.

## US-18 — Documents — interface

En tant qu’avocat, je veux signaler manuellement un risque dans une pièce afin d’attirer l’attention sur un passage.

**Écrans à développer**

- Formulaire et liste des risques manuels du document.
- Routes proposées : /documents/:id/risques.

**Sous-stories / tâches**

- [ ] **US-18.UI-1** — Créer les champs description, priorité et page/clause.
- [ ] **US-18.UI-2** — Afficher la liste des signalements avec mention Saisie manuelle.
- [ ] **US-18.UI-3** — Appeler la fonction d’enregistrement fournie sans calcul juridique ni IA.
- [ ] **US-18.UI-4** — Vérifier avec la fiche de test du chef : affichage, champs requis, chargement, liste vide et message d’erreur ; joindre une capture et remonter les anomalies.

**Préparé par le chef :** Implémenter persistance et validations des risques manuels sans analyse automatique. Les composants partagés et les appels API sont également fournis par le chef.

## US-22 — Temps — interface

En tant qu’avocat, je veux saisir et corriger mon temps avec audit afin de suivre le travail facturable.

**Écrans à développer**

- Liste des temps ; formulaire de saisie manuelle.
- Routes proposées : /temps ; /temps/nouveau.

**Sous-stories / tâches**

- [ ] **US-22.UI-1** — Afficher dossier, date, durée et état facturable fournis.
- [ ] **US-22.UI-2** — Créer un formulaire de saisie/correction et afficher les erreurs renvoyées.
- [ ] **US-22.UI-3** — Brancher enregistrer et afficher l’historique d’audit retourné par le serveur.
- [ ] **US-22.UI-4** — Vérifier avec la fiche de test du chef : affichage, champs requis, chargement, liste vide et message d’erreur ; joindre une capture et remonter les anomalies.

**Préparé par le chef :** Implémenter durées et corrections auditées ainsi que les règles de modification du temps déjà facturé. Les composants partagés et les appels API sont également fournis par le chef.

## US-24 — Facturation — interface

En tant que responsable de cabinet, je veux définir les tarifs du cabinet ou d’un dossier afin de valoriser les heures facturables.

**Écrans à développer**

- Formulaire de tarif cabinet et section tarif du dossier.
- Routes proposées : /parametres/tarifs ; /dossiers/:id/tarif.

**Sous-stories / tâches**

- [ ] **US-24.UI-1** — Afficher le tarif et la devise renvoyés par le serveur.
- [ ] **US-24.UI-2** — Créer un champ montant et un choix cabinet/dossier à partir du modèle fourni.
- [ ] **US-24.UI-3** — Brancher enregistrer et afficher le tarif applicable calculé par le backend.
- [ ] **US-24.UI-4** — Vérifier avec la fiche de test du chef : affichage, champs requis, chargement, liste vide et message d’erreur ; joindre une capture et remonter les anomalies.

**Préparé par le chef :** Implémenter priorité tarif dossier/cabinet, devise, précision et contrôles de modification. Les composants partagés et les appels API sont également fournis par le chef.

## US-25 — Facturation — interface

En tant que responsable de cabinet, je veux préparer une facture à partir du temps facturable afin de contrôler les montants avant validation.

**Écrans à développer**

- Préparation et aperçu du brouillon de facture.
- Routes proposées : /factures/nouvelle ; /factures/:id.

**Sous-stories / tâches**

- [ ] **US-25.UI-1** — Afficher les entrées facturables fournies avec une case de sélection.
- [ ] **US-25.UI-2** — Appeler la fonction Préparer le brouillon avec les identifiants sélectionnés.
- [ ] **US-25.UI-3** — Afficher lignes et total calculés par le serveur sans formule financière dans le navigateur.
- [ ] **US-25.UI-4** — Vérifier avec la fiche de test du chef : affichage, champs requis, chargement, liste vide et message d’erreur ; joindre une capture et remonter les anomalies.

**Préparé par le chef :** Implémenter calculs monétaires, arrondis, instantanés et protection contre la double facturation. Les composants partagés et les appels API sont également fournis par le chef.

## US-26 — Facturation — interface

En tant que responsable de cabinet, je veux valider une facture et exporter son PDF afin de disposer d’une pièce stable et partageable.

**Écrans à développer**

- Détail de facture avec validation et export.
- Routes proposées : /factures/:id.

**Sous-stories / tâches**

- [ ] **US-26.UI-1** — Afficher les lignes, le total et le statut fournis.
- [ ] **US-26.UI-2** — Ajouter les boutons Confirmer la validation et Télécharger le PDF avec les fonctions fournies.
- [ ] **US-26.UI-3** — Afficher les erreurs et la lecture seule renvoyées ; orienter les corrections vers l’écran du chef.
- [ ] **US-26.UI-4** — Vérifier avec la fiche de test du chef : affichage, champs requis, chargement, liste vide et message d’erreur ; joindre une capture et remonter les anomalies.

**Préparé par le chef :** Implémenter validation humaine, gel, PDF et workflow de correction par version/avoir simulé ; développer son écran. Les composants partagés et les appels API sont également fournis par le chef.

## US-27 — Facturation — interface

En tant que responsable de cabinet, je veux marquer une facture payée ou non payée afin de suivre les impayés.

**Écrans à développer**

- Liste des factures et section statut de paiement.
- Routes proposées : /factures ; /factures/:id.

**Sous-stories / tâches**

- [ ] **US-27.UI-1** — Afficher numéro, montant et statut payé/non payé fournis.
- [ ] **US-27.UI-2** — Ajouter un filtre payé/non payé branché sur l’API préparée.
- [ ] **US-27.UI-3** — Ajouter l’action de déclaration de paiement selon les actions autorisées par le serveur.
- [ ] **US-27.UI-4** — Vérifier avec la fiche de test du chef : affichage, champs requis, chargement, liste vide et message d’erreur ; joindre une capture et remonter les anomalies.

**Préparé par le chef :** Implémenter mise à jour du statut déclaré, autorisations et audit sans paiement bancaire réel. Les composants partagés et les appels API sont également fournis par le chef.

## Limite du périmètre temps et facturation

Le minuteur US-23, sa reprise après rechargement, les calculs financiers, les arrondis, la génération PDF et le workflow de correction des factures sont développés par le chef. Tu utilises les résultats et les fonctions fournis dans les écrans listés ci-dessus.

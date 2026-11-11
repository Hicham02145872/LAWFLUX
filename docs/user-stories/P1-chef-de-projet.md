# P1 — Toi, chef de projet

Tu prends en charge tous les développements complexes : architecture, backend des quatre services métier, gRPC, BFF, sécurité, données, stockage, événements, calculs financiers, CI et tests techniques. Fatima et Oumaima réalisent les interfaces avec les composants, hooks et contrats que tu fournis.

Les écrans et routes ci-dessous sont à développer. Cette répartition remplace celle des premiers documents et de l’Excel. Les mêmes ID Story peuvent apparaître chez deux personnes : toi pour le backend ou la partie complexe, elles pour les sous-tâches UI explicitement indiquées.

## US-01 — Socle

En tant que membre de l’équipe technique, je veux démarrer une architecture Spring Boot reproductible afin de développer les services indépendamment.

**Sous-stories / tâches à ta charge**

- [ ] **US-01.TECH-1** — Définir les frontières des services, les modèles et les bases propriétaires.
- [ ] **US-01.TECH-2** — Créer le parent Maven, les modules Spring Boot, le socle React et les profils locaux.
- [ ] **US-01.TECH-3** — Créer Compose, PostgreSQL, RabbitMQ et stockage privé avec configuration sans secrets.
- [ ] **US-01.TECH-4** — Fournir à Fatima/Oumaima composants, hooks API, fixtures, démarrage documenté et exemples de formulaires.

**Écrans à développer**

- Aucun écran métier dédié ; travail technique ou de coordination.

## US-02 — Contrats

En tant que développeur, je veux disposer de contrats REST, gRPC et événements versionnés afin d’intégrer les services sans ambiguïté.

**Sous-stories / tâches à ta charge**

- [ ] **US-02.TECH-1** — Concevoir tous les contrats protobuf, routes REST du BFF, erreurs et pagination.
- [ ] **US-02.TECH-2** — Générer les stubs et implémenter les clients/serveurs gRPC.
- [ ] **US-02.TECH-3** — Préparer les fonctions frontend typées et les hooks métier consommés par Fatima/Oumaima.
- [ ] **US-02.TECH-4** — Définir événements, délais, propagation du contexte et compatibilité des contrats.

**Écrans à développer**

- Aucun écran métier dédié ; travail technique ou de coordination.

## US-03 — Identité

En tant que responsable de cabinet, je veux créer un cabinet de démonstration afin d’organiser mon espace de travail.

**Sous-stories / tâches à ta charge**

- [ ] **US-03.TECH-1** — Créer migrations cabinets et appartenances.
- [ ] **US-03.TECH-2** — Implémenter création contrôlée du cabinet et de son responsable.
- [ ] **US-03.TECH-3** — Créer écran de configuration du cabinet et validation des champs.
- [ ] **US-03.TECH-4** — Tester création valide, doublon et absence d’accès métier implicite de l’admin plateforme.

**Écrans à développer**

- Configuration du cabinet — /parametres/cabinet.

## US-04 — Identité

En tant que responsable de cabinet, je veux gérer les membres et leurs rôles afin d’attribuer les responsabilités du cabinet.

**Sous-stories / tâches à ta charge**

- [ ] **US-04.TECH-1** — Modéliser utilisateurs, rôles et liens cabinet-utilisateur.
- [ ] **US-04.TECH-2** — Créer APIs membres et contrôle des actions réservées au responsable.
- [ ] **US-04.TECH-3** — Créer liste et formulaire membres avec rôles autorisés.
- [ ] **US-04.TECH-4** — Tester création avocat/client, élévation de privilèges et accès inter-cabinet refusés.

**Écrans à développer**

- Gestion des membres et rôles — /parametres/membres.

## US-05 — Identité

En tant qu’utilisateur, je veux me connecter et me déconnecter afin d’accéder à mon espace de manière sécurisée.

**Sous-stories / tâches à ta charge**

- [ ] **US-05.TECH-1** — Choisir durée des jetons et stratégie session/refresh et révocation.
- [ ] **US-05.TECH-2** — Implémenter authentification, vérification des identifiants et déconnexion.
- [ ] **US-05.TECH-3** — Créer connexion, navigation selon rôle et gestion de session expirée.
- [ ] **US-05.TECH-4** — Tester mauvais mot de passe, jeton expiré et impossibilité de renouveler une session révoquée.

**Écrans à développer**

- Connexion — /connexion ; navigation commune et écran session expirée.

## US-06 — Identité

En tant qu’utilisateur connecté, je veux changer mon mot de passe afin de protéger mon compte.

**Sous-stories / tâches à ta charge**

- [ ] **US-06.TECH-1** — Définir règles de validation et vérification de l’ancien mot de passe.
- [ ] **US-06.TECH-2** — Créer commande de changement et appliquer la politique de révocation des sessions.
- [ ] **US-06.TECH-3** — Créer formulaire ancien/nouveau mot de passe et confirmation.
- [ ] **US-06.TECH-4** — Vérifier ancien mot de passe incorrect, confirmation incohérente et nouvelle connexion.

**Écrans à développer**

- Changement de mot de passe — /parametres/securite.

## US-07 — Sécurité

En tant que responsable de cabinet, je veux garantir l’isolation de mon cabinet afin de préserver la confidentialité des données.

**Sous-stories / tâches à ta charge**

- [ ] **US-07.TECH-1** — Implémenter personnellement les vérifications tenant, RBAC et permissions métier dans tous les services.
- [ ] **US-07.TECH-2** — Sécuriser HTTP/gRPC et les accès directs aux fichiers ; refuser par défaut en cas de vérification indisponible.
- [ ] **US-07.TECH-3** — Préparer deux cabinets fictifs et les tests techniques d’isolation.
- [ ] **US-07.TECH-4** — Fournir les actions autorisées et composants de session ; vérifier que masquer un bouton ne remplace jamais une autorisation serveur.

**Écrans à développer**

- Composants Accès refusé et Session expirée ; toutes les routes protégées.

## US-08 — Clients — backend et intégration

En tant que responsable de cabinet, je veux créer, modifier et archiver mes clients afin de centraliser les informations du cabinet.

**Sous-stories / tâches à ta charge**

- [ ] **US-08.TECH-1** — Créer entité client et migration avec tenant_id et archivage.
- [ ] **US-08.TECH-2** — Implémenter commandes et liste paginée des clients.
- [ ] **US-08.TECH-3** — Tester saisie invalide, archivage et refus d’accès aux clients d’un autre cabinet.
- [ ] **US-08.TECH-4** — Fournir les endpoints sécurisés de liste, création, modification et archivage des clients.
- [ ] **US-08.TECH-5** — Préparer le client API frontend et un exemple d’utilisation pour Fatima.

**Écrans à développer**

- Aucun écran supplémentaire pour toi : fournir APIs, composants et intégration pour « Liste des clients ; formulaire créer/modifier ; fiche client », développé par Fatima.

**Livrable pour Fatima :** fonctions frontend prêtes à appeler, exemples de réponses, erreurs affichables et données fictives. Le chef reste responsable des règles métier et de la sécurité serveur.

## US-09 — Dossiers — backend et intégration

En tant que responsable de cabinet, je veux créer, modifier et archiver un dossier afin de suivre les affaires du cabinet.

**Sous-stories / tâches à ta charge**

- [ ] **US-09.TECH-1** — Créer dossier, statuts, lien client et champs d’archivage.
- [ ] **US-09.TECH-2** — Implémenter commandes dossier et validation des références client.
- [ ] **US-09.TECH-3** — Tester cycle de vie, client d’un autre cabinet et archivage logique.
- [ ] **US-09.TECH-4** — Implémenter les règles de statuts et de référence client, les transactions et les APIs dossier.
- [ ] **US-09.TECH-5** — Préparer le client API frontend et un exemple d’utilisation pour Fatima.

**Écrans à développer**

- Aucun écran supplémentaire pour toi : fournir APIs, composants et intégration pour « Liste et fiche dossier ; formulaire créer/modifier », développé par Fatima.

**Livrable pour Fatima :** fonctions frontend prêtes à appeler, exemples de réponses, erreurs affichables et données fictives. Le chef reste responsable des règles métier et de la sécurité serveur.

## US-10 — Dossiers — backend et intégration

En tant que responsable de cabinet, je veux affecter les avocats et définir les accès dossier afin de contrôler qui peut consulter une affaire.

**Sous-stories / tâches à ta charge**

- [ ] **US-10.TECH-1** — Créer affectations et autorisations explicites de dossier.
- [ ] **US-10.TECH-2** — Implémenter affectation et RPC CheckCaseAccess avec refus par défaut.
- [ ] **US-10.TECH-3** — Tester avocat affecté/non affecté, exception cabinet et client non autorisé.
- [ ] **US-10.TECH-4** — Concevoir et implémenter CheckCaseAccess, affectations et autorisations explicites ; aucun code de permission à déléguer.
- [ ] **US-10.TECH-5** — Préparer le client API frontend et un exemple d’utilisation pour Fatima.

**Écrans à développer**

- Aucun écran supplémentaire pour toi : fournir APIs, composants et intégration pour « Section affectations et accès client de la fiche dossier », développé par Fatima.

**Livrable pour Fatima :** fonctions frontend prêtes à appeler, exemples de réponses, erreurs affichables et données fictives. Le chef reste responsable des règles métier et de la sécurité serveur.

## US-11 — Dossiers — backend et intégration

En tant qu’avocat, je veux rechercher et filtrer mes dossiers autorisés afin de retrouver rapidement une affaire.

**Sous-stories / tâches à ta charge**

- [ ] **US-11.TECH-1** — Définir filtres et pagination avec limites explicites.
- [ ] **US-11.TECH-2** — Créer requêtes et index adaptés au cabinet et aux filtres.
- [ ] **US-11.TECH-3** — Tester filtres combinés, page vide et absence de fuite de dossiers non autorisés.
- [ ] **US-11.TECH-4** — Implémenter recherche, pagination, index et filtrage serveur des résultats autorisés.
- [ ] **US-11.TECH-5** — Préparer le client API frontend et un exemple d’utilisation pour Fatima.

**Écrans à développer**

- Aucun écran supplémentaire pour toi : fournir APIs, composants et intégration pour « Barre de recherche et filtres de la liste des dossiers », développé par Fatima.

**Livrable pour Fatima :** fonctions frontend prêtes à appeler, exemples de réponses, erreurs affichables et données fictives. Le chef reste responsable des règles métier et de la sécurité serveur.

## US-12 — Dossiers — backend et intégration

En tant qu’avocat autorisé, je veux consulter l’historique d’un dossier afin de comprendre les changements effectués.

**Sous-stories / tâches à ta charge**

- [ ] **US-12.TECH-1** — Créer historique append-only des actions dossier.
- [ ] **US-12.TECH-2** — Journaliser création, modifications, statuts et affectations.
- [ ] **US-12.TECH-3** — Vérifier auteur/date et conservation des changements après plusieurs mises à jour.
- [ ] **US-12.TECH-4** — Implémenter la collecte et la consultation de l’historique append-only.
- [ ] **US-12.TECH-5** — Préparer le client API frontend et un exemple d’utilisation pour Fatima.

**Écrans à développer**

- Aucun écran supplémentaire pour toi : fournir APIs, composants et intégration pour « Onglet historique du dossier », développé par Fatima.

**Livrable pour Fatima :** fonctions frontend prêtes à appeler, exemples de réponses, erreurs affichables et données fictives. Le chef reste responsable des règles métier et de la sécurité serveur.

## US-13 — Portail client — backend et intégration

En tant que client, je veux consulter uniquement mes dossiers et pièces autorisés afin de suivre mes affaires en toute confidentialité.

**Sous-stories / tâches à ta charge**

- [ ] **US-13.TECH-1** — Tester client sans partage, retrait de partage et accès par URL devinée.
- [ ] **US-13.TECH-2** — Composer les données du portail et garantir les permissions côté services et BFF.
- [ ] **US-13.TECH-3** — Préparer le client API frontend et un exemple d’utilisation pour Fatima.

**Écrans à développer**

- Aucun écran supplémentaire pour toi : fournir APIs, composants et intégration pour « Portail client : liste et détail des dossiers autorisés », développé par Fatima.

**Livrable pour Fatima :** fonctions frontend prêtes à appeler, exemples de réponses, erreurs affichables et données fictives. Le chef reste responsable des règles métier et de la sécurité serveur.

## US-14 — Documents — backend et intégration

En tant qu’avocat autorisé, je veux importer un document PDF ou DOCX afin de centraliser les pièces du dossier.

**Sous-stories / tâches à ta charge**

- [ ] **US-14.TECH-1** — Créer métadonnées, clés de stockage et liens dossier/tenant.
- [ ] **US-14.TECH-2** — Implémenter upload avec contrôle de contenu/type/taille et stockage privé.
- [ ] **US-14.TECH-3** — Tester fichier interdit/trop grand, stockage privé et dossier non autorisé.
- [ ] **US-14.TECH-4** — Implémenter l’upload, la validation serveur du contenu/type/taille et le stockage privé avec gestion des échecs.
- [ ] **US-14.TECH-5** — Préparer le client API frontend et un exemple d’utilisation pour Oumaima.

**Écrans à développer**

- Aucun écran supplémentaire pour toi : fournir APIs, composants et intégration pour « Liste des documents ; formulaire d’import », développé par Oumaima.

**Livrable pour Oumaima :** fonctions frontend prêtes à appeler, exemples de réponses, erreurs affichables et données fictives. Le chef reste responsable des règles métier et de la sécurité serveur.

## US-15 — Documents — backend et intégration

En tant qu’avocat autorisé, je veux ajouter et consulter les versions d’un document afin de conserver la traçabilité des pièces.

**Sous-stories / tâches à ta charge**

- [ ] **US-15.TECH-1** — Créer modèle de version et numérotation cohérente.
- [ ] **US-15.TECH-2** — Créer ajout/liste des versions sans écraser le fichier précédent.
- [ ] **US-15.TECH-3** — Vérifier deux versions distinctes, auteurs et versionnement concurrent sans écrasement.
- [ ] **US-15.TECH-4** — Implémenter numérotation et concurrence des versions sans écrasement.
- [ ] **US-15.TECH-5** — Préparer le client API frontend et un exemple d’utilisation pour Oumaima.

**Écrans à développer**

- Aucun écran supplémentaire pour toi : fournir APIs, composants et intégration pour « Onglet versions d’un document », développé par Oumaima.

**Livrable pour Oumaima :** fonctions frontend prêtes à appeler, exemples de réponses, erreurs affichables et données fictives. Le chef reste responsable des règles métier et de la sécurité serveur.

## US-16 — Documents — backend et intégration

En tant qu’avocat autorisé, je veux partager une pièce au client et contrôler son téléchargement afin de transmettre les documents autorisés.

**Sous-stories / tâches à ta charge**

- [ ] **US-16.TECH-1** — Implémenter partage/révocation avec validation de l’autorisation client dossier.
- [ ] **US-16.TECH-2** — Créer téléchargement privé après vérification des droits via gRPC.
- [ ] **US-16.TECH-3** — Tester document non partagé, partage révoqué et indisponibilité de CheckCaseAccess : refus sûr.
- [ ] **US-16.TECH-4** — Implémenter partage/révocation, contrôle des droits et téléchargement privé ; ne jamais exposer une clé de stockage publique.
- [ ] **US-16.TECH-5** — Préparer le client API frontend et un exemple d’utilisation pour Oumaima.

**Écrans à développer**

- Aucun écran supplémentaire pour toi : fournir APIs, composants et intégration pour « Section partage et téléchargement du document », développé par Oumaima.

**Livrable pour Oumaima :** fonctions frontend prêtes à appeler, exemples de réponses, erreurs affichables et données fictives. Le chef reste responsable des règles métier et de la sécurité serveur.

## US-17 — Documents — backend et intégration

En tant qu’avocat ou client autorisé, je veux commenter une pièce accessible afin d’échanger dans le contexte du document.

**Sous-stories / tâches à ta charge**

- [ ] **US-17.TECH-1** — Modéliser commentaires document, auteur, date et visibilité.
- [ ] **US-17.TECH-2** — Créer ajout/lecture avec permissions document et partage.
- [ ] **US-17.TECH-3** — Vérifier auteur/date et impossibilité pour un client de lire un commentaire interne.
- [ ] **US-17.TECH-4** — Implémenter règles de visibilité et accès aux commentaires document côté serveur.
- [ ] **US-17.TECH-5** — Préparer le client API frontend et un exemple d’utilisation pour Oumaima.

**Écrans à développer**

- Aucun écran supplémentaire pour toi : fournir APIs, composants et intégration pour « Onglet commentaires du document », développé par Oumaima.

**Livrable pour Oumaima :** fonctions frontend prêtes à appeler, exemples de réponses, erreurs affichables et données fictives. Le chef reste responsable des règles métier et de la sécurité serveur.

## US-18 — Documents — backend et intégration

En tant qu’avocat, je veux signaler manuellement un risque dans une pièce afin d’attirer l’attention sur un passage.

**Sous-stories / tâches à ta charge**

- [ ] **US-18.TECH-1** — Créer annotation avec priorité, page/clause et appréciation manuelle.
- [ ] **US-18.TECH-2** — Créer commandes de signalement et validations des champs.
- [ ] **US-18.TECH-3** — Tester champs requis, droits et absence d’annotation créée automatiquement.
- [ ] **US-18.TECH-4** — Implémenter persistance et validations des risques manuels sans analyse automatique.
- [ ] **US-18.TECH-5** — Préparer le client API frontend et un exemple d’utilisation pour Oumaima.

**Écrans à développer**

- Aucun écran supplémentaire pour toi : fournir APIs, composants et intégration pour « Formulaire et liste des risques manuels du document », développé par Oumaima.

**Livrable pour Oumaima :** fonctions frontend prêtes à appeler, exemples de réponses, erreurs affichables et données fictives. Le chef reste responsable des règles métier et de la sécurité serveur.

## US-19 — Collaboration — backend et intégration

En tant qu’avocat ou client autorisé, je veux commenter un dossier avec une visibilité adaptée afin de collaborer sans exposer les échanges internes.

**Sous-stories / tâches à ta charge**

- [ ] **US-19.TECH-1** — Créer commentaires dossier avec visibilité, auteur et date.
- [ ] **US-19.TECH-2** — Créer ajout/lecture et contrôle du droit de commentaire selon rôle.
- [ ] **US-19.TECH-3** — Tester séparation des fils et refus d’accès aux échanges d’un autre client.
- [ ] **US-19.TECH-4** — Implémenter visibilité interne/partagée et droits de commentaire dossier côté serveur.
- [ ] **US-19.TECH-5** — Préparer le client API frontend et un exemple d’utilisation pour Fatima.

**Écrans à développer**

- Aucun écran supplémentaire pour toi : fournir APIs, composants et intégration pour « Onglet commentaires du dossier », développé par Fatima.

**Livrable pour Fatima :** fonctions frontend prêtes à appeler, exemples de réponses, erreurs affichables et données fictives. Le chef reste responsable des règles métier et de la sécurité serveur.

## US-20 — Tâches — backend et intégration

En tant qu’avocat, je veux créer et suivre les tâches d’un dossier afin de respecter les échéances.

**Sous-stories / tâches à ta charge**

- [ ] **US-20.TECH-1** — Créer tâche avec responsable, priorité, échéance et état de suivi.
- [ ] **US-20.TECH-2** — Implémenter commandes, filtres de retard et production task.due_soon.
- [ ] **US-20.TECH-3** — Tester responsable hors cabinet, changement d’échéance et passage d’une tâche à terminée.
- [ ] **US-20.TECH-4** — Implémenter responsable autorisé, échéances, retards et événements de rappel.
- [ ] **US-20.TECH-5** — Préparer le client API frontend et un exemple d’utilisation pour Fatima.

**Écrans à développer**

- Aucun écran supplémentaire pour toi : fournir APIs, composants et intégration pour « Liste et formulaire des tâches », développé par Fatima.

**Livrable pour Fatima :** fonctions frontend prêtes à appeler, exemples de réponses, erreurs affichables et données fictives. Le chef reste responsable des règles métier et de la sécurité serveur.

## US-21 — Notifications

En tant qu’utilisateur autorisé, je veux recevoir des notifications applicatives afin de connaître les événements qui me concernent.

**Sous-stories / tâches à ta charge**

- [ ] **US-21.TECH-1** — Produire les événements Dossiers/Documents et les rappels de tâches ; prendre en charge le broker.
- [ ] **US-21.TECH-2** — Créer worker, notifications persistées, destinataires et déduplication.
- [ ] **US-21.TECH-3** — Développer le centre de notifications et les opérations lu/non lu.
- [ ] **US-21.TECH-4** — Tester chaque événement, les doublons et l’isolation des destinataires.

**Écrans à développer**

- Centre de notifications — /notifications.

## US-22 — Temps — backend et intégration

En tant qu’avocat, je veux saisir et corriger mon temps avec audit afin de suivre le travail facturable.

**Sous-stories / tâches à ta charge**

- [ ] **US-22.TECH-1** — Créer entrées de temps, caractère facturable et journal de correction.
- [ ] **US-22.TECH-2** — Implémenter saisie et correction avec règles de facturation.
- [ ] **US-22.TECH-3** — Tester saisie de 2 heures, durée invalide et traçabilité d’une correction.
- [ ] **US-22.TECH-4** — Implémenter durées et corrections auditées ainsi que les règles de modification du temps déjà facturé.
- [ ] **US-22.TECH-5** — Préparer le client API frontend et un exemple d’utilisation pour Oumaima.

**Écrans à développer**

- Aucun écran supplémentaire pour toi : fournir APIs, composants et intégration pour « Liste des temps ; formulaire de saisie manuelle », développé par Oumaima.

**Livrable pour Oumaima :** fonctions frontend prêtes à appeler, exemples de réponses, erreurs affichables et données fictives. Le chef reste responsable des règles métier et de la sécurité serveur.

## US-23 — Temps

En tant qu’avocat, je veux utiliser un minuteur pour mon dossier afin d’éviter de calculer manuellement ma durée.

**Sous-stories / tâches à ta charge**

- [ ] **US-23.TECH-1** — Définir règle de minuteur actif et comportement après rechargement.
- [ ] **US-23.TECH-2** — Implémenter démarrage/arrêt idempotent et calcul de durée.
- [ ] **US-23.TECH-3** — Créer commandes du minuteur et affichage de la durée.
- [ ] **US-23.TECH-4** — Tester double arrêt, rechargement et absence de durée négative ou doublon.

**Écrans à développer**

- Minuteur et reprise après rechargement — /temps/minuteur ; composant du dossier.

## US-24 — Facturation — backend et intégration

En tant que responsable de cabinet, je veux définir les tarifs du cabinet ou d’un dossier afin de valoriser les heures facturables.

**Sous-stories / tâches à ta charge**

- [ ] **US-24.TECH-1** — Créer tarifs cabinet/dossier avec montant précis et devise.
- [ ] **US-24.TECH-2** — Implémenter édition réservée au responsable et résolution du tarif applicable.
- [ ] **US-24.TECH-3** — Tester héritage cabinet, tarif dossier et montants invalides sans erreur d’arrondi.
- [ ] **US-24.TECH-4** — Implémenter priorité tarif dossier/cabinet, devise, précision et contrôles de modification.
- [ ] **US-24.TECH-5** — Préparer le client API frontend et un exemple d’utilisation pour Oumaima.

**Écrans à développer**

- Aucun écran supplémentaire pour toi : fournir APIs, composants et intégration pour « Formulaire de tarif cabinet et section tarif du dossier », développé par Oumaima.

**Livrable pour Oumaima :** fonctions frontend prêtes à appeler, exemples de réponses, erreurs affichables et données fictives. Le chef reste responsable des règles métier et de la sécurité serveur.

## US-25 — Facturation — backend et intégration

En tant que responsable de cabinet, je veux préparer une facture à partir du temps facturable afin de contrôler les montants avant validation.

**Sous-stories / tâches à ta charge**

- [ ] **US-25.TECH-1** — Créer brouillon et lignes avec instantané des durées et tarifs.
- [ ] **US-25.TECH-2** — Créer calcul et sélection des entrées sans double facturation.
- [ ] **US-25.TECH-3** — Vérifier 2 heures × tarif applicable, arrondis et refus d’entrée déjà facturée.
- [ ] **US-25.TECH-4** — Implémenter calculs monétaires, arrondis, instantanés et protection contre la double facturation.
- [ ] **US-25.TECH-5** — Préparer le client API frontend et un exemple d’utilisation pour Oumaima.

**Écrans à développer**

- Aucun écran supplémentaire pour toi : fournir APIs, composants et intégration pour « Préparation et aperçu du brouillon de facture », développé par Oumaima.

**Livrable pour Oumaima :** fonctions frontend prêtes à appeler, exemples de réponses, erreurs affichables et données fictives. Le chef reste responsable des règles métier et de la sécurité serveur.

## US-26 — Facturation — backend et intégration

En tant que responsable de cabinet, je veux valider une facture et exporter son PDF afin de disposer d’une pièce stable et partageable.

**Sous-stories / tâches à ta charge**

- [ ] **US-26.TECH-1** — Créer transition brouillon/validée et gel des données validées.
- [ ] **US-26.TECH-2** — Générer le PDF à partir de l’instantané de facture.
- [ ] **US-26.TECH-3** — Comparer PDF et lignes ; refuser mutation d’une facture validée ; vérifier correction traçable.
- [ ] **US-26.TECH-4** — Implémenter validation humaine, gel, PDF et workflow de correction par version/avoir simulé ; développer son écran.
- [ ] **US-26.TECH-5** — Préparer le client API frontend et un exemple d’utilisation pour Oumaima.

**Écrans à développer**

- Correction d’une facture par nouvelle version ou avoir simulé — /factures/:id/correction. Détail et boutons : Oumaima.

**Livrable pour Oumaima :** fonctions frontend prêtes à appeler, exemples de réponses, erreurs affichables et données fictives. Le chef reste responsable des règles métier et de la sécurité serveur.

## US-27 — Facturation — backend et intégration

En tant que responsable de cabinet, je veux marquer une facture payée ou non payée afin de suivre les impayés.

**Sous-stories / tâches à ta charge**

- [ ] **US-27.TECH-1** — Ajouter statut déclaré de paiement et audit de changement.
- [ ] **US-27.TECH-2** — Implémenter changement de statut limité au responsable.
- [ ] **US-27.TECH-3** — Tester changement payé/non payé et refus de modification par un client.
- [ ] **US-27.TECH-4** — Implémenter mise à jour du statut déclaré, autorisations et audit sans paiement bancaire réel.
- [ ] **US-27.TECH-5** — Préparer le client API frontend et un exemple d’utilisation pour Oumaima.

**Écrans à développer**

- Aucun écran supplémentaire pour toi : fournir APIs, composants et intégration pour « Liste des factures et section statut de paiement », développé par Oumaima.

**Livrable pour Oumaima :** fonctions frontend prêtes à appeler, exemples de réponses, erreurs affichables et données fictives. Le chef reste responsable des règles métier et de la sécurité serveur.

## US-28 — Tableau de bord — backend et intégration

En tant que responsable ou avocat, je veux consulter mes indicateurs autorisés afin de piloter les dossiers et l’activité.

**Sous-stories / tâches à ta charge**

- [ ] **US-28.TECH-1** — Composer les résultats via BFF/gRPC avec délais et contrôle d’accès.
- [ ] **US-28.TECH-2** — Comparer compteurs aux données fictives pour deux cabinets et différents rôles.
- [ ] **US-28.TECH-3** — Implémenter les agrégations backend et la composition BFF avec dégradation contrôlée et filtrage par droits.
- [ ] **US-28.TECH-4** — Préparer le client API frontend et un exemple d’utilisation pour Fatima.

**Écrans à développer**

- Aucun écran supplémentaire pour toi : fournir APIs, composants et intégration pour « Tableau de bord du cabinet ou de l’avocat », développé par Fatima.

**Livrable pour Fatima :** fonctions frontend prêtes à appeler, exemples de réponses, erreurs affichables et données fictives. Le chef reste responsable des règles métier et de la sécurité serveur.

## US-29 — Résilience

En tant qu’utilisateur du cabinet, je veux continuer à utiliser les dossiers pendant une panne du worker afin d’éviter l’interruption du travail.

**Sous-stories / tâches à ta charge**

- [ ] **US-29.TECH-1** — Configurer files durables, messages persistants et accusés après persistance.
- [ ] **US-29.TECH-2** — Configurer retries bornés, DLQ et déduplication des traitements.
- [ ] **US-29.TECH-3** — Documenter la limite de double écriture ; décider séparément de l’extension outbox pour panne broker.
- [ ] **US-29.TECH-4** — Arrêter le worker, créer un dossier, le consulter et vérifier la notification après reprise.

**Écrans à développer**

- Aucun écran métier dédié ; travail technique ou de coordination.

## US-30 — Observabilité

En tant que membre de l’équipe technique, je veux observer les appels et la santé des services afin de diagnostiquer les incidents.

**Sous-stories / tâches à ta charge**

- [ ] **US-30.TECH-1** — Configurer logs structurés et exclusion des secrets et contenus sensibles.
- [ ] **US-30.TECH-2** — Propager correlation_id dans BFF, appels gRPC et RabbitMQ.
- [ ] **US-30.TECH-3** — Exposer health/readiness et métriques dans le réseau approprié.
- [ ] **US-30.TECH-4** — Tracer un parcours et une erreur de dépendance dans les différents services.

**Écrans à développer**

- Aucun écran métier dédié ; travail technique ou de coordination.

## US-31 — CI et qualité

En tant que membre de l’équipe technique, je veux vérifier automatiquement chaque contribution afin d’intégrer un code compatible et relu.

**Sous-stories / tâches à ta charge**

- [ ] **US-31.TECH-1** — Créer les workflows backend/protobuf et frontend.
- [ ] **US-31.TECH-2** — Exécuter les tests utiles et contrôles de build en CI.
- [ ] **US-31.TECH-3** — Préparer la procédure Git/PR et coordonner les contributions de Fatima et Oumaima.
- [ ] **US-31.TECH-4** — Organiser la revue croisée et vérifier qu’un job échoue bien en cas d’erreur.

**Écrans à développer**

- Aucun écran métier dédié ; travail technique ou de coordination.

## US-32 — Recette

En tant que chef de projet, je veux valider les parcours A à E de bout en bout afin de démontrer que le MVP répond au cahier des charges.

**Sous-stories / tâches à ta charge**

- [ ] **US-32.TECH-1** — Préparer et automatiser les vérifications critiques A–E.
- [ ] **US-32.TECH-2** — Prendre en charge les scénarios techniques de panne et d’isolation ainsi que les contrôles de facturation.
- [ ] **US-32.TECH-3** — Donner à Fatima/Oumaima des fiches de recette manuelle pour leurs écrans.
- [ ] **US-32.TECH-4** — Corriger les défauts backend/intégration et coordonner les corrections d’interface avec leurs propriétaires.

**Écrans à développer**

- Aucun écran métier dédié ; travail technique ou de coordination.

## US-33 — Performance

En tant que chef de projet, je veux mesurer les performances du prototype afin d’évaluer l’objectif de consultation de dossier.

**Sous-stories / tâches à ta charge**

- [ ] **US-33.TECH-1** — Définir données, parcours, nombre d’utilisateurs et durée du test.
- [ ] **US-33.TECH-2** — Créer et exécuter personnellement le script de charge reproductible.
- [ ] **US-33.TECH-3** — Analyser P50/P95, débit, erreurs et configuration.
- [ ] **US-33.TECH-4** — Comparer à la cible et documenter limites et optimisations.

**Écrans à développer**

- Aucun écran métier dédié ; travail technique ou de coordination.

## US-34 — Livraison

En tant que chef de projet, je veux livrer un projet documenté et démontrable afin de réussir la remise et la soutenance.

**Sous-stories / tâches à ta charge**

- [ ] **US-34.TECH-1** — Rédiger les parties architecture, sécurité, API, exploitation et résultats techniques.
- [ ] **US-34.TECH-2** — Consolider le rapport de 20 pages maximum et la présentation avec les captures/commentaires d’écran des collègues.
- [ ] **US-34.TECH-3** — Préparer et répéter la démonstration A–E puis vérifier GitHub et les consignes Classroom.
- [ ] **US-34.TECH-4** — Vérifier démarrage depuis clone vierge et présence de tous les livrables.

**Écrans à développer**

- Aucun écran métier dédié ; travail technique ou de coordination.

## US-35 — Pilotage

En tant que chef de projet, je veux piloter le backlog et les dépendances afin de coordonner le travail des trois membres.

**Sous-stories / tâches à ta charge**

- [ ] **US-35.TECH-1** — Confirmer Fatima sur les interfaces dossiers/collaboration et Oumaima sur les interfaces documents/facturation.
- [ ] **US-35.TECH-2** — Découper les tickets en tâches avec un exemple et un résultat attendu.
- [ ] **US-35.TECH-3** — Planifier deux points courts par semaine et une démonstration intégrée hebdomadaire.
- [ ] **US-35.TECH-4** — Suivre charge et blocages ; conserver personnellement les travaux complexes et accompagner l’intégration.

**Écrans à développer**

- Aucun écran métier dédié ; travail technique ou de coordination.

## US-36 — Sauvegarde et audit

En tant que responsable de cabinet, je veux disposer de sauvegardes et de traces des actions sensibles afin de restaurer les données et expliquer les changements.

**Sous-stories / tâches à ta charge**

- [ ] **US-36.TECH-1** — Implémenter sauvegarde et restauration des bases et du stockage privé.
- [ ] **US-36.TECH-2** — Implémenter personnellement audit des actions sensibles et suppression logique dans les services concernés.
- [ ] **US-36.TECH-3** — Documenter restauration et politique de conservation restant à définir avec le cabinet.
- [ ] **US-36.TECH-4** — Tester une restauration cohérente des métadonnées, versions de fichiers et traces d’audit.

**Écrans à développer**

- Aucun écran métier dédié ; travail technique ou de coordination.

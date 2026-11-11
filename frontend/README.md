# Portail React — points d’entrée prévus

Ce dossier réserve la place de l’application React. Aucun écran ni client API métier n’est encore implémenté. Le navigateur utilisera uniquement le REST du BFF, jamais directement les ports gRPC.

Les écrans et responsabilités figurent dans les [stories Fatima](../docs/user-stories/P2-dossiers-collaboration.md), les [stories Oumaima](../docs/user-stories/P3-documents-facturation.md) et les [stories du chef de projet](../docs/user-stories/P1-chef-de-projet.md).

Avant le travail des interfaces, le chef prépare navigation, composants partagés, gestion de session, fonctions API et exemples de données. La sonde `/api/system/services/{name}` sert uniquement au diagnostic technique ; ce n’est pas l’API des dossiers.

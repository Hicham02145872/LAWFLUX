# Portail React LAWFLUX

Le socle utilise React, TypeScript, Vite et React Router. Il fournit une navigation, des routes pour les écrans prévus et un composant de page partagé. Les routes métier affichent une page d’attente à remplacer par l’écran correspondant.

Les écrans et responsabilités figurent dans les [stories Fatima](../docs/user-stories/P2-dossiers-collaboration.md), les [stories Oumaima](../docs/user-stories/P3-documents-facturation.md) et les [stories du chef de projet](../docs/user-stories/P1-chef-de-projet.md).

## Démarrage local

Prérequis Node.js 22.12 ou plus récent. Depuis le dossier `frontend` :

```powershell
npm ci
npm run dev
```

Ouvrir `http://127.0.0.1:5173`. Le proxy Vite transmet les requêtes `/api` au BFF sur `http://127.0.0.1:8080`. Pour utiliser une autre adresse, copier `.env.example` vers `.env.local` et modifier `LAWFLUX_BFF_URL`.

La page `/diagnostic` teste le trajet React → BFF → service Dossiers. Elle fonctionne avec le backend démarré et affiche une erreur lisible lorsque le service est indisponible. `npm run build` vérifie les types et génère les fichiers dans `dist/`.

## Où travailler

- Fatima ajoute ses écrans dans `src/features/dossiers/`.
- Oumaima ajoute ses écrans dans `src/features/documents-facturation/`.
- Hicham maintient la navigation dans `src/app/`, les éléments réutilisables et le client HTTP dans `src/shared/`.

Pour raccorder un écran, remplacer son composant d’attente dans le fichier `routes.tsx` de la fonctionnalité. Le serveur reste responsable de l’autorisation. La gestion des sessions et les opérations métier seront raccordées quand leurs API seront disponibles.

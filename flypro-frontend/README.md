# FlyPro Analytics — Frontend

Application React 19 (Vite + TypeScript) pour la plateforme FlyPro Analytics.

## Stack

- React 19 + TypeScript + Vite
- React Router DOM
- TanStack Query + Axios
- React Hook Form + Zod
- Tailwind CSS v4
- Recharts + Lucide + React Hot Toast

## Démarrage

```bash
cd flypro-frontend
npm install
npm run dev
```

Ouvrir [http://localhost:5173](http://localhost:5173)

Compte démo backend : `admin` / `Admin@123`

## Variables d'environnement

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_AI_BASE_URL=http://localhost:8000
```

## Scripts

- `npm run dev` — serveur de développement
- `npm run build` — build production
- `npm run test` — Vitest
- `npm run preview` — preview du build

## Modules

Dashboard, Employés, Départements, Badges/Affectations, Zones, Profils d'accès, Points d'accès, Événements, Imports, Alertes, Analytics AI, Utilisateurs/Rôles (lecture session), Paramètres, Profil.

## Architecture

```
src/
  api/          # Axios clients + services
  features/     # Modules métier
  components/   # UI + DataTable + Charts
  layouts/      # Auth + Dashboard
  contexts/     # Auth + Theme
  routes/       # Protection RBAC
```

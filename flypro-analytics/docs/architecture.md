# Documentation architecture – vue d'ensemble
# Rôle : décrire Clean Architecture / DDD de FlyPro Analytics.
# Contenu détaillé à rédiger au fur et à mesure des modules.

## Principes

1. **Indépendance des modules métier** — packages Java isolés par domaine.
2. **Dépendances vers l'intérieur** — controllers → services → repositories/domain.
3. **Contrats explicites** — DTO en bordure API ; Entity en persistance.
4. **Frontend feature-based** — chaque domaine UI encapsule ses pages/hooks/services.
5. **AI découplé** — microservice FastAPI ; communication HTTP depuis `analytics`.

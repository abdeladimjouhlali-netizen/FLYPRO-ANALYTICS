# Diagrammes UML – FlyPro Analytics

Sources PlantUML pour la Phase 5 – Step 17.

## Fichiers

| Fichier | Diagramme |
|---------|-----------|
| [usecase.puml](usecase.puml) | Cas d’utilisation |
| [class.puml](class.puml) | Classes domaine |
| [sequence.puml](sequence.puml) | Séquences métier |
| [component.puml](component.puml) | Composants |
| [deployment.puml](deployment.puml) | Déploiement Docker |
| [package.puml](package.puml) | Packages |
| [erd.puml](erd.puml) | ERD |
| [activity.puml](activity.puml) | Activité import |
| [activity-alerts.puml](activity-alerts.puml) | Activité cycle alerte |
| [auth-flow.puml](auth-flow.puml) | Authentification JWT |
| [ai-prediction.puml](ai-prediction.puml) | Prédiction IA |

## Rendu

```bash
# Depuis la racine du projet
java -jar plantuml.jar -tpng docs/UML/*.puml -o diagrams
```

Extensions IDE : « PlantUML » (VS Code / IntelliJ).

Les sources sont conçues pour compiler avec PlantUML standard (syntaxe `@startuml` / `@enduml`).

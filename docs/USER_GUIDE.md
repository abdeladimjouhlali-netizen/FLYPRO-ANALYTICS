# Manuel utilisateur – FlyPro Analytics

Public : administrateurs, analystes sécurité / RH, démonstration PFE.

Compte de démo : **admin** / **Admin@123**

---

## 1. Connexion (Login)

1. Ouvrir l’application (ex. http://localhost/)  
2. Saisir identifiant et mot de passe  
3. Valider  

En cas d’échec répété, un verrouillage temporaire peut s’appliquer (message d’erreur 429).

Après connexion, redirection vers le **Dashboard**.

---

## 2. Dashboard

Vue de synthèse :

- KPI (accès, refus, alertes, …)  
- Graphiques et tendances  
- Tops zones / points d’accès / employés  
- Heures de pointe  

Réservé aux profils autorisés (Admin / Analyste / Super Admin).

---

## 3. Employés

- **Liste** : recherche, pagination, export CSV éventuel  
- **Création / édition** : matricule, identité, département, profil d’accès, poste  
- **Détail** : statut, badges liés  
- Actions : activer, désactiver, archiver, supprimer (selon droits)

---

## 4. Départements

Gestion de l’organigramme (code unique, nom, description, actif).  
La suppression d’un département avec employés le **désactive** plutôt que de le détruire.

---

## 5. Badges

Cycle de vie :

- Création (numéro, type, dates)  
- Affectation à un employé  
- Renouvellement, remplacement  
- Suspension, activation, révocation, perte  
- Historique des affectations  

Page **Badge assignments** pour le suivi des liaisons.

---

## 6. Zones & contrôle d’accès

### Zones

Définir bâtiments / zones (sensibles ou non).

### Points d’accès

Lecteurs rattachés à une zone ; états actif / inactif / maintenance.

### Profils d’accès

Plages horaires, niveau de sécurité, association aux zones autorisées.  
Les employés héritent d’un profil.

---

## 7. Événements d’accès

Journal des passages (horodatage, badge, employé, zone, résultat GRANTED/DENIED, direction).  
Consultation détaillée + création manuelle pour les admins.

---

## 8. Imports

Réservé **Admin / Super Admin** :

1. **Upload** fichier CSV / Excel / JSON  
2. **Preview** colonnes et qualité  
3. **Execute** pour importer les événements  
4. Consulter le **rapport** (issues de qualité)

Formats et colonnes obligatoires documentés dans l’UI / messages d’erreur API.

---

## 9. Alertes

- Liste filtrable (statut, sévérité, type)  
- Détail lié éventuel à employé / badge / événement  
- Workflow : **Acknowledge** → **Resolve** → **Close** (admins)

Les analystes consultent ; les admins traitent.

---

## 10. Analytics (IA)

Écran dédié à la détection d’anomalies :

- Santé du modèle  
- Entraînement / rechargement (selon droits UI)  
- Prédiction unitaire ou lot  
- Affichage risk score, confidence, raisons, actions recommandées  

Voir [AI.md](AI.md) pour le détail scientifique.

---

## 11. Paramètres & administration

- **Profil** : informations du compte connecté  
- **Settings** : préférences UI (thème, …)  
- **Users / Roles** : réservé Super Admin (écrans dédiés)

---

## 12. Rapports

Les données opérationnelles sont exploitables via :

- Dashboard (KPI / charts)  
- Exports CSV des tableaux  
- Rapports d’import  
- Scores d’anomalie / alertes  

Pour un reporting décisionnel avancé (BI), exporter vers un outil externe à partir des API dashboard.

---

## 13. Conseils d’usage

- Utiliser un compte Analyste pour la consultation pure  
- Vérifier les alertes quotidiennes  
- Réentraîner le modèle IA après import massif de données représentatives  
- Ne pas partager le compte `admin` en démonstration publique sans changer le mot de passe

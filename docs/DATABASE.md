# Base de données – FlyPro Analytics

## 1. Généralités

| Élément | Valeur |
|---------|--------|
| SGBD | MySQL 8.4 (InnoDB, utf8mb4) |
| Migrations | Flyway `classpath:db/migration` |
| Versions | V1 → V10 |
| Hibernate | `ddl-auto=validate`, dialecte MySQL |
| Timezone JDBC | UTC |

## 2. Historique Flyway

| Version | Contenu |
|---------|---------|
| V1 | `roles`, `users`, `refresh_tokens` |
| V2 | Seed rôles + super admin |
| V3 | Modèle domaine (départements, employés, badges, zones, événements, alertes, …) |
| V4 | Flag `active` départements + indexes |
| V5 | `employees.position` NOT NULL |
| V6 | Relation N-N profils ↔ zones |
| V7 | Enrichissement access points |
| V8 | Enrichissement access events (`source`, …) |
| V9 | Enrichissement alerts |
| V10 | Indexes de performance composites |

## 3. Catalogue des entités

### 3.1 `roles`

**Purpose :** Rôles RBAC applicatifs.

| Champ | Type | Contraintes |
|-------|------|-------------|
| id | BIGINT PK | AI |
| name | VARCHAR(50) | UNIQUE NOT NULL |
| description | VARCHAR(255) | |
| created_at / updated_at | DATETIME(6) | audit |
| created_by / updated_by | VARCHAR(100) | |

**Relations :** 1-N → `users`  
**Business rules :** Noms seed `ROLE_SUPER_ADMIN`, `ROLE_ADMIN`, `ROLE_ANALYST`, `ROLE_EMPLOYEE`.

---

### 3.2 `users`

**Purpose :** Comptes de connexion à la plateforme.

| Champ | Type | Contraintes |
|-------|------|-------------|
| id | BIGINT PK | |
| username | VARCHAR(50) | UNIQUE |
| email | VARCHAR(255) | UNIQUE |
| password | VARCHAR(255) | BCrypt hash |
| enabled | BOOLEAN | défaut true |
| account_locked | BOOLEAN | défaut false |
| role_id | BIGINT FK | → roles |
| audit | … | |

**Indexes :** `idx_users_role_id`, `idx_users_email`  
**Business rules :** Mot de passe jamais en clair ; refresh tokens liés.

---

### 3.3 `refresh_tokens`

**Purpose :** Refresh JWT persistés / révocables.

| Champ | Contraintes |
|-------|-------------|
| token | UNIQUE |
| user_id | FK CASCADE |
| expires_at | NOT NULL |
| revoked | BOOLEAN |

**Indexes :** `idx_refresh_tokens_user_id`

---

### 3.4 `departments`

**Purpose :** Organisation RH.

| Champ | Contraintes |
|-------|-------------|
| name | UNIQUE |
| code | UNIQUE |
| description | optionnel |
| active | BOOLEAN (V4) |

**Relations :** 1-N → `employees`  
**Indexes :** code, name, active, created_at  
**Business rules :** Suppression : hard delete si aucun employé, sinon soft-deactivate.

---

### 3.5 `access_profiles`

**Purpose :** Profils horaires / niveau de sécurité d’accès.

| Champ | Notes |
|-------|-------|
| name | UNIQUE |
| security_level | VARCHAR |
| start_time / end_time | TIME |

**Relations :** N-N zones via `access_profile_zones` ; 1-N employés.

---

### 3.6 `employees`

**Purpose :** Employés physiques contrôlés par badge.

| Champ | Contraintes |
|-------|-------------|
| employee_number | UNIQUE |
| email | UNIQUE |
| first_name, last_name | NOT NULL |
| position | NOT NULL (V5) |
| status | NOT NULL |
| department_id | FK NOT NULL |
| access_profile_id | FK nullable |

**Indexes :** employee_number, email, department_id, access_profile_id  
**Business rules :** Activation / désactivation / archive via API.

---

### 3.7 `badges`

**Purpose :** Supports physiques/virtuels d’accès.

| Champ | Notes |
|-------|-------|
| badge_number | UNIQUE |
| status | ACTIVE, SUSPENDED, … |
| type | |
| issue_date / expiration_date | |

**Relations :** N-N employés via `badge_assignments`.

---

### 3.8 `badge_assignments`

**Purpose :** Historique d’affectation badge ↔ employé.

| Champ | Notes |
|-------|-------|
| badge_id, employee_id | FK |
| assigned_at / unassigned_at | |
| active | bool |

**Indexes :** badge_id, employee_id, active  
**Business rules :** Une affectation active par badge en régime nominal.

---

### 3.9 `zones`

**Purpose :** Zones physiques (bâtiment, sensibilité).

Champs typiques : name, building, floor, sensitive, audit.  
**Relations :** 1-N access_points ; N-N access_profiles.

---

### 3.10 `access_points`

**Purpose :** Lecteurs / portes rattachés à une zone.

| Champ | Notes |
|-------|-------|
| code | UNIQUE |
| zone_id | FK |
| status | ACTIVE / MAINTENANCE / … |
| name | (V7) |

**Business rules :** activate / deactivate / maintenance.

---

### 3.11 `access_profile_zones`

**Purpose :** Autorisations de zones par profil (V6).

PK composite / ids + FK profile + zone.  
Indexes sur `access_profile_id`, `zone_id`.

---

### 3.12 `access_events`

**Purpose :** Journal des passages (cœur analytics).

| Champ | Notes |
|-------|-------|
| event_timestamp | indexé |
| direction | IN/OUT |
| result | GRANTED/DENIED… |
| badge_id, employee_id, access_point_id, zone_id | FK |
| source | (V8) |

**Indexes (dont V10) :**

- `(event_timestamp)`, `(result)`, FKs
- `(event_timestamp, result)`
- `(employee_id, event_timestamp)`
- `(zone_id, event_timestamp)`

---

### 3.13 `import_batches` / `data_quality_issues`

**Purpose :** Lots d’import CSV/XLSX/JSON et anomalies de qualité.

Relations : batch 1-N issues.  
Indexes : import_batch_id, severity.

---

### 3.14 `alerts`

**Purpose :** Alertes opérationnelles / anomalies.

| Champ | Notes |
|-------|-------|
| status | OPEN, ACK, RESOLVED, CLOSED… |
| severity | |
| type | (V9) |
| employee_id, badge_id, access_event_id | FK optionnels |
| created_date | |

**Indexes V10 :** `(status, severity)`, `(created_date, status)`

---

### 3.15 `anomaly_scores`

**Purpose :** Scores IA liés à un événement.

FK `access_event_id`, `algorithm`, score.

---

### 3.16 `audit_logs`

**Purpose :** Traçabilité actions (username, entity, timestamps).

Indexes : username, (entity_name, entity_id), created_at.

---

## 4. Diagramme ERD

Voir `UML/erd.puml`.

## 5. Règles transverses

- Clés étrangères InnoDB
- Colonnes d’audit `created_at` / `updated_at` / `created_by` / `updated_by` sur le domaine
- Pas de changement de schéma hors Flyway
- Indexes additifs uniquement pour la performance (V10)

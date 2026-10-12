# Support de présentation – FlyPro Analytics (PFE)

## 1. Executive Summary

**FlyPro Analytics** est une plateforme web professionnelle de gestion des badges et accès employés, enrichie par un service d’intelligence artificielle de détection d’anomalies. Elle couvre le cycle de vie opérationnel (employés, badges, zones, événements), le pilotage (dashboard, alertes) et l’analyse prédictive (Isolation Forest), le tout déployable via Docker Compose.

## 2. Project Objectives

- Centraliser la gestion des accès physiques (badges, zones, profils)  
- Historiser et analyser les événements d’accès  
- Détecter automatiquement les comportements anormaux  
- Fournir des tableaux de bord décisionnels  
- Livrer une solution **production-ready** (sécurité, monitoring, docs)

## 3. Problem Statement

Les systèmes de contrôle d’accès génèrent un volume important d’événements difficiles à exploiter manuellement. Sans plateforme unifiée et sans aide à la détection, les équipes RH / sécurité risquent :

- des retards dans la détection d’intrusions ou d’usages frauduleux  
- une mauvaise gouvernance des badges (perdus, non révoqués)  
- l’absence de vision temps réel sur les flux d’accès  

## 4. Solution Overview

Application full-stack :

1. **Frontend React** — UX métier  
2. **Backend Spring Boot** — API sécurisée JWT + persistance MySQL  
3. **Service AI FastAPI** — anomalies Isolation Forest + règles  
4. **Nginx + Docker** — déploiement reproductible  

## 5. Technology Stack

| Couche | Technologies |
|--------|----------------|
| Frontend | React 19, TypeScript, Vite, Tailwind, TanStack Query, Recharts |
| Backend | Spring Boot 3.5, Security JWT, JPA, Flyway, MapStruct, Actuator |
| AI | FastAPI, Scikit-Learn Isolation Forest, Joblib |
| Data | MySQL 8.4 |
| Infra | Docker, Compose, Nginx, GitHub Actions CI |

## 6. Architecture Summary

Architecture multi-services derrière une gateway Nginx.  
Le backend détient la vérité métier ; l’AI est un microservice indépendant consommé par l’UI et/ou le backend.  
Schémas détaillés : [ARCHITECTURE.md](ARCHITECTURE.md), `UML/`.

## 7. Key Features

- Authentification JWT + RBAC (Super Admin, Admin, Analyst)  
- CRUD employés, départements, badges, zones, profils, points d’accès  
- Journal des événements + imports CSV/XLSX/JSON  
- Alertes avec workflow acknowledge / resolve / close  
- Dashboard KPI, tendances, tops, peak hours  
- Rate limiting & brute-force protection  
- Healthchecks, Prometheus, docs techniques complètes  

## 8. AI Features

- Entraînement / réentraînement Isolation Forest  
- Prédiction unitaire et batch  
- Risk score + confidence + explications (règles métier)  
- Gestion de versions de modèles sur volume persistant  

## 9. Challenges

- Cohérence multi-modules (sécurité, pagination, validation)  
- Performance des agrégations dashboard (indexes V10)  
- Intégration AI sans coupler le schéma SQL  
- Durcissement production (headers, secrets, Actuator)  
- Écart brief PostgreSQL vs implémentation MySQL (documenté)

## 10. Future Improvements

- TLS bout-en-bout, WAF  
- Rate-limit Redis (scalabilité horizontale)  
- OpenTelemetry / Grafana  
- Authn API AI (mTLS / JWT)  
- Migration PostgreSQL si exigence ops  
- Explainability avancée (SHAP)  

## 11. Conclusion

FlyPro Analytics démontre la capacité à concevoir et livrer une solution d’ingénierie complète : métier, data, IA, sécurité et DevOps. Le projet est adapté à une soutenance PFE et à une démonstration client, avec une documentation enterprise et des artefacts UML prêts à l’emploi.

---

### Plan de slides suggéré (10–12)

1. Titre & contexte  
2. Problématique  
3. Objectifs  
4. Stack  
5. Architecture (diagramme)  
6. Modules métier  
7. Sécurité  
8. IA  
9. Déploiement Docker  
10. Démo live  
11. Résultats / tests  
12. Perspectives & conclusion  

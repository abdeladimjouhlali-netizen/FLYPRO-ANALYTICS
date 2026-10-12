# Disaster Recovery Guide – FlyPro Analytics

---

## 1. Objectifs RTO / RPO (cibles)

| Métrique | Cible démo / PME | Cible enterprise |
|----------|------------------|------------------|
| RPO | ≤ 24 h (backup nightly) | ≤ 15–60 min (PITR managé) |
| RTO | ≤ 2–4 h | ≤ 30–60 min |

---

## 2. Scénarios

### A. Pod / nœud down

- Kubernetes reschedule + readiness  
- HPA / replicas ≥ 2  
- **Action :** aucune si PDB/replicas OK  

### B. Déploiement défectueux

```bash
kubectl -n flypro rollout undo deployment/backend
kubectl -n flypro rollout undo deployment/ai-service
kubectl -n flypro rollout undo deployment/frontend
```

Le workflow CD (`cd-k8s.yml`) tente un rollback automatique.

### C. Corruption / perte base

1. Mettre l’Ingress en maintenance (scale frontend 0 ou page statique)  
2. Identifier dernier backup sain (`backups/*.meta.json`)  
3. `./k8s/restore.sh <file>`  
4. Restart backend  
5. Vérifier `/actuator/health` + login smoke  
6. Rouvrir le trafic  

### D. Perte cluster

1. Nouveau cluster + Ingress + secrets  
2. `kubectl apply -k k8s/`  
3. Restore MySQL depuis offsite  
4. Restore / sync modèles AI  
5. Rejouer smoke tests  
6. Basculer DNS  

### E. Secret compromis

1. Rotater `JWT_SECRET`, mots de passe DB  
2. `kubectl apply -f k8s/secret.yaml` + rollout restart  
3. Invalider sessions (JWT)  
4. Audit logs OpenSearch  

---

## 3. Checklist recovery

- [ ] Backup récent vérifié (sha256)  
- [ ] Secrets disponibles (vault)  
- [ ] Images registry accessibles  
- [ ] DNS / TLS prêts  
- [ ] Monitoring up (Prometheus/Grafana)  
- [ ] Smoke : health backend, health AI, login admin  
- [ ] Communication stakeholders  

---

## 4. Améliorations DR

- Base managée avec PITR  
- Multi-AZ / multi-région  
- Velero + tests de restore automatisés  
- Runbooks PagerDuty / Opsgenie  
- Game days trimestriels  

Voir aussi : [BACKUP.md](BACKUP.md) · [KUBERNETES.md](KUBERNETES.md)

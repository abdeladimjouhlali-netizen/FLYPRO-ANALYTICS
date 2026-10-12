# Backup Guide – FlyPro Analytics

---

## 1. Périmètre

| Donnée | Méthode |
|--------|---------|
| MySQL `flypro_analytics` | `mysqldump` logique (gzip) |
| Modèles AI | Volume `ai-models-pvc` / snapshot CSI |
| Config | Git (`k8s/`, `.env.example`) — pas de secrets en clair |

> Runtime = **MySQL 8.4** (malgré le nom de fichier `postgres-deployment.yaml`).

---

## 2. Scripts

| Script | Rôle |
|--------|------|
| `k8s/backup.sh` | Dump + checksum + rétention |
| `k8s/restore.sh` | Restore interactif |
| `k8s/backup-cronjob.yaml` | Cron K8s 02:00 UTC |

### Backup manuel

```bash
chmod +x k8s/backup.sh k8s/restore.sh
./k8s/backup.sh
# BACKUP_MODE=kubectl NAMESPACE=flypro ./k8s/backup.sh
# BACKUP_MODE=docker ./k8s/backup.sh
```

Sortie : `backups/flypro_<db>_<timestamp>.sql.gz` + `.meta.json` (sha256).

### Restore

```bash
./k8s/restore.sh backups/flypro_flypro_analytics_YYYYMMDDTHHMMSSZ.sql.gz
# Confirmer en tapant RESTORE
kubectl -n flypro rollout restart deployment/backend
```

---

## 3. CronJob cluster

```bash
kubectl apply -f k8s/backup-cronjob.yaml
kubectl -n flypro get cronjob mysql-backup
```

PVC `mysql-backup-pvc` (50 Gi) — en prod : sync vers S3/GCS (sidecar `rclone` / Velero).

---

## 4. Politique recommandée

| Paramètre | Valeur |
|-----------|--------|
| Fréquence | Quotidienne + avant release |
| Rétention locale | 14 jours |
| Offsite | 30–90 jours (objet storage) |
| Test restore | Mensuel |

---

## 5. Snapshots volumes

Utiliser le CSI du cloud :

```bash
# Exemple Velero
velero backup create flypro-daily --include-namespaces flypro
```

Couvrir PVC MySQL + AI models + backups.

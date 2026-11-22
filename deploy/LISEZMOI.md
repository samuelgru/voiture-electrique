# Déploiement de « Ma Zoé » sur le Synology DS220+

Fichiers :
- `zoe-backend-1.0.0.tar.gz`, `zoe-frontend-1.0.0.tar.gz` : images de l'appli (linux/amd64)
- `postgres-16-alpine.tar.gz` : image PostgreSQL, utile seulement si le NAS ne peut pas la télécharger depuis Docker Hub
- `docker-compose.yml` et `.env.example` (à renommer en `.env`) : le projet Container Manager

## 1. Copier les fichiers

Créer le dossier `/volume1/docker/zoe` (File Station) et y copier `docker-compose.yml` et `.env.example` renommé en `.env`.
Créer aussi les sous-dossiers `data/postgres` (Container Manager ne les crée pas tout seul) :
dans File Station, `zoe` > Créer un dossier `data`, puis dans `data` > Créer un dossier `postgres`.
Ou en SSH : `sudo mkdir -p /volume1/docker/zoe/data/postgres`
Modifier `DB_PASSWORD` dans `.env` avant le premier démarrage.

## 2. Importer les images

**Container Manager** > Image > Importer > Ajouter à partir d'un fichier : importer chaque `.tar.gz`.

Ou en SSH :
```bash
sudo docker load -i zoe-backend-1.0.0.tar.gz
sudo docker load -i zoe-frontend-1.0.0.tar.gz
sudo docker load -i postgres-16-alpine.tar.gz
```

## 3. Démarrer

**Container Manager** > Projet > Créer : chemin `/volume1/docker/zoe`, source « Utiliser le docker-compose.yml existant ».

Ou en SSH : `cd /volume1/docker/zoe && sudo docker compose up -d`

L'appli est sur `http://<ip-du-nas>:8090`. Pour reprendre l'historique : Paramètres > Import du fichier Excel.

## Sauvegarde

Les données sont dans `/volume1/docker/zoe/data/postgres` (incluses dans Hyper Backup si le dossier `docker` est sauvegardé).
Dump SQL : `sudo docker exec zoe-db pg_dump -U zoe zoe > /volume1/docker/zoe/zoe-$(date +%F).sql`

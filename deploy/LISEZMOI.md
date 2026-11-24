# Déploiement de « Ma Zoé » sur le Synology DS220+

Version 2.2.0 (export Excel) : Spring Boot 4.2 / Java 25, Angular 22, PostgreSQL 19, connexion par identifiant / mot de passe.

Les images sont publiées sur GitHub Container Registry : `ghcr.io/samuelgru/zoe-backend` et `ghcr.io/samuelgru/zoe-frontend`.

Fichiers :
- `docker-compose.yml` et `.env.example` (à renommer en `.env`) : le projet Container Manager

## Publier une version (sur le PC)

1. Mettre à jour la version dans `backend/pom.xml` et dans `deploy/docker-compose.yml` (les trois doivent être identiques), puis commiter.
2. Poser et pousser le tag :
   ```bash
   git tag v2.2.0 && git push origin main v2.2.0
   ```
3. Le workflow GitHub Actions « Publish » construit et publie les deux images (`:2.2.0` et `:latest`). Suivi dans l'onglet Actions.

## Se connecter à GHCR (une seule fois sur le NAS)

Les images sont privées par défaut. Créer sur GitHub un *Personal access token (classic)* avec uniquement le droit `read:packages`, puis en SSH :
```bash
echo <TOKEN> | sudo docker login ghcr.io -u samuelgru --password-stdin
```
(Inutile si les images sont rendues publiques dans GitHub > Packages > Package settings.)

## Mettre à jour le NAS

Dans `/volume1/docker/zoe`, après avoir mis la nouvelle version dans `docker-compose.yml` :
```bash
sudo docker compose pull && sudo docker compose up -d
```

## Solution de repli : archives d'images

Si le NAS ne peut pas joindre GHCR, les archives se génèrent sur le PC (Docker + buildx) :
```bash
./deploy/build-images.sh                  # zoe-backend et zoe-frontend, version lue dans backend/pom.xml
./deploy/build-images.sh --avec-postgres  # + l'image postgres:19-alpine
```
Les fichiers `zoe-backend-<version>.tar.gz` et `zoe-frontend-<version>.tar.gz` (non versionnés dans git) s'importent dans Container Manager > Image
(ou `sudo docker load -i <fichier>.tar.gz`). Dans `docker-compose.yml`, utiliser alors `zoe-backend:<version>` et `zoe-frontend:<version>` à la place des images `ghcr.io/...`.

Les commandes SSH ci-dessous se lancent depuis `/volume1/docker/zoe` (`cd /volume1/docker/zoe`).

---

## Comptes de connexion

Deux comptes, définis dans `.env` :

| Compte | Variables | Accès |
|---|---|---|
| Administrateur | `ADMIN_USERNAME`, `ADMIN_PASSWORD` (obligatoire) | tout |
| Utilisateur | `USER_USERNAME`, `USER_PASSWORD` (vide = pas de compte) | Synthèse, Recharges, Kilométrage (lecture et saisie) ; ni Tarifs, ni Paramètres, ni import |

- Sans `ADMIN_PASSWORD`, le projet refuse de démarrer.
- Pour changer un mot de passe : modifier `.env` puis redémarrer le projet (Arrêter / Démarrer).
- 5 mots de passe faux de suite bloquent l'identifiant pendant 15 minutes.
- La connexion reste active 30 jours sur l'appareil, même après un redémarrage du NAS.

---

## Mise à jour depuis la version 2.0.0

1. Ajouter dans `/volume1/docker/zoe/.env` les comptes (choisir de vrais mots de passe) :
   ```
   ADMIN_USERNAME=admin
   ADMIN_PASSWORD=...
   USER_USERNAME=utilisateur
   USER_PASSWORD=...
   ```
2. Récupérer les images : `sudo docker compose pull` (ou importer les archives, voir « Solution de repli »).
3. Remplacer `docker-compose.yml` par celui de ce dossier.
4. Container Manager > Projet > zoe > Arrêter, puis Construire / Démarrer (ou `sudo docker compose up -d`).

La base n'a rien à migrer à la main : la table des sessions est créée automatiquement au démarrage.

---

## Mise à jour depuis la version 1.0.0 (PostgreSQL 16)

Suivre d'abord la procédure ci-dessous, en ajoutant directement les comptes dans `.env` (voir « Comptes de connexion »).

PostgreSQL 19 ne sait pas lire les fichiers d'une base PostgreSQL 16 : il faut exporter les données puis les restaurer.
L'ancienne base (`data/postgres`) n'est pas modifiée, elle sert de solution de repli.

1. **Exporter les données** (l'ancienne stack doit tourner) :
   ```bash
   sudo docker exec zoe-db pg_dump -U zoe -d zoe -Fc > zoe-pg16.dump
   ```
2. **Arrêter le projet** : Container Manager > Projet > zoe > Arrêter (ou `sudo docker compose down`).
3. **Récupérer les nouvelles images** (`sudo docker compose pull`, ou importer les archives : voir « Solution de repli »).
4. **Remplacer `docker-compose.yml`** par celui de ce dossier. Garder le `.env` existant (même mot de passe).
5. **Créer le dossier de la nouvelle base** :
   ```bash
   sudo mkdir -p /volume1/docker/zoe/data/pg
   ```
6. **Démarrer la base seule et restaurer** :
   ```bash
   sudo docker compose up -d db
   sleep 10
   sudo docker exec -i zoe-db pg_restore -U zoe -d zoe --no-owner < zoe-pg16.dump
   ```
7. **Démarrer le reste** : `sudo docker compose up -d` (ou Container Manager > Projet > zoe > Démarrer).
   Si Container Manager signale que le projet a changé, choisir « Construire » / « Recréer ».
8. Vérifier l'appli. Une fois satisfait, supprimer l'ancien dossier `data/postgres` et `zoe-pg16.dump`.

Alternative sans SSH : à l'étape 6, démarrer tout le projet sur une base vide, puis réimporter `electricite.xlsx`
(Paramètres > Import). Les saisies faites dans l'appli depuis le dernier import Excel sont alors perdues.

Retour arrière : remettre l'ancien `docker-compose.yml` (images 1.0.0, `postgres:16-alpine`, `./data/postgres`) et redémarrer.

---

## Première installation

1. Créer `/volume1/docker/zoe` (File Station), y copier `docker-compose.yml` et `.env.example` renommé en `.env`.
   Modifier `DB_PASSWORD` et les mots de passe des comptes dans `.env` avant le premier démarrage.
2. Créer le dossier de la base (Container Manager ne le crée pas tout seul) : dans File Station, `zoe` > `data` > `pg`,
   ou `sudo mkdir -p /volume1/docker/zoe/data/pg`.
3. Se connecter à GHCR (voir plus haut) puis `sudo docker compose pull`
   (ou importer les archives : Container Manager > Image > Importer, voir « Solution de repli »).
4. Container Manager > Projet > Créer : chemin `/volume1/docker/zoe`, « Utiliser le docker-compose.yml existant ».
5. Ouvrir `http://<ip-du-nas>:8090`, puis Paramètres > Import du fichier Excel pour reprendre l'historique.

---

## Sauvegarde

Les données sont dans `/volume1/docker/zoe/data/pg` (incluses dans Hyper Backup si le dossier `docker` est sauvegardé).
Dump SQL :
```bash
sudo docker exec zoe-db pg_dump -U zoe -d zoe -Fc > /volume1/docker/zoe/zoe-$(date +%F).dump
```

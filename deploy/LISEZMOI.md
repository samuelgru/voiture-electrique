# Déploiement de « Ma Zoé » sur le Synology DS220+

Version 2.1.0 : Spring Boot 4.2 / Java 25, Angular 22, PostgreSQL 19, connexion par identifiant / mot de passe.

Fichiers :
- `zoe-backend-2.1.0.tar.gz`, `zoe-frontend-2.1.0.tar.gz` : images de l'appli (linux/amd64)
- `postgres-19-alpine.tar.gz` : image PostgreSQL, utile seulement si le NAS ne peut pas la télécharger depuis Docker Hub
- `docker-compose.yml` et `.env.example` (à renommer en `.env`) : le projet Container Manager

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
2. Importer les images `zoe-backend-2.1.0.tar.gz` et `zoe-frontend-2.1.0.tar.gz`.
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
3. **Importer les nouvelles images** (Container Manager > Image > Importer, ou `sudo docker load -i <fichier>.tar.gz`).
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
3. Importer les images : Container Manager > Image > Importer > Ajouter à partir d'un fichier (chaque `.tar.gz`),
   ou `sudo docker load -i <fichier>.tar.gz`.
4. Container Manager > Projet > Créer : chemin `/volume1/docker/zoe`, « Utiliser le docker-compose.yml existant ».
5. Ouvrir `http://<ip-du-nas>:8090`, puis Paramètres > Import du fichier Excel pour reprendre l'historique.

---

## Sauvegarde

Les données sont dans `/volume1/docker/zoe/data/pg` (incluses dans Hyper Backup si le dossier `docker` est sauvegardé).
Dump SQL :
```bash
sudo docker exec zoe-db pg_dump -U zoe -d zoe -Fc > /volume1/docker/zoe/zoe-$(date +%F).dump
```

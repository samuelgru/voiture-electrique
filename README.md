# Ma Zoé – suivi de consommation électrique

Remplace le classeur `electricite.xlsx` : Angular 18 (nginx) + Spring Boot 3 (Java 21) + PostgreSQL 16, le tout dans Docker.

| Écran | Équivalent Excel |
|---|---|
| **Synthèse** | onglet « Synthèse » (contrat, kilométrage, conso, comparaison essence, tableau mensuel) + 2 graphiques |
| **Recharges** | onglet « Zoe » (heures HC/HP, coût HC, coût HP et total calculés automatiquement) |
| **Kilométrage** | colonne « Kilométrage » de la synthèse + km actuel (un relevé au 1er de chaque mois) |
| **Tarifs** | onglet « Prix Electricité » (+ plage d'heures creuses par tarif) |
| **Paramètres** | contrat (durée, km, compteur de départ, pénalité), conso essence, **import Excel** |

## Déploiement sur le NAS

```bash
cp .env.example .env      # puis changer DB_PASSWORD, le port, le dossier des données
docker compose up -d --build
```

L'appli est ensuite sur `http://<ip-du-nas>:8090`. Les données PostgreSQL sont dans `DATA_DIR` (par défaut `./data`).

Pour reprendre l'historique : **Paramètres → Import du fichier Excel** avec `electricite.xlsx`. L'import remplace toutes les données.

Si le NAS ne peut pas construire les images (trop peu de RAM), construis-les sur le PC pour la bonne architecture puis transfère-les :

```bash
docker buildx build --platform linux/amd64 -t zoe-backend:latest --load backend   # linux/arm64 pour un NAS ARM
docker buildx build --platform linux/amd64 -t zoe-frontend:latest --load frontend
docker save zoe-backend:latest zoe-frontend:latest | gzip > zoe-images.tar.gz
# sur le NAS : docker load < zoe-images.tar.gz && docker compose up -d
```

Sauvegarde de la base :

```bash
docker compose exec db pg_dump -U zoe zoe > zoe-$(date +%F).sql
```

## Règles de calcul

- **Coût d'une recharge** : puissance moyenne (kWh ÷ durée) × heures HC × prix HC + puissance × heures HP × prix HP. Au parc relais, toutes les heures sont au prix « parc relais ». Le tarif retenu est le dernier dont la date d'effet est ≤ date de début (comme le `XLOOKUP(..., -1)`).
- **Heures creuses** : la plage (ex. 22:00 → 06:20) est portée par chaque tarif ; le calcul gère les recharges sur plusieurs nuits.
- **Mois** : une recharge compte dans le mois de sa date de début ; km mensuel = compteur du 1er du mois suivant (ou dernier relevé pour le mois en cours) − compteur du 1er du mois.
- **Écart au contrat** : positif = km en trop. La pénalité s'applique à l'écart des mois clos.
- **Simulation essence** : km du mois ÷ 100 × conso (L/100) × prix du litre du tarif en vigueur ce mois-là.

## Développement

```bash
docker compose up -d db                       # base seule, port non exposé : ajouter "5432:5432" si besoin
cd backend && mvn spring-boot:run             # API sur :8080
cd frontend && npm install && npx ng serve    # http://localhost:4200, proxy /api -> :8080
```

-- Paramètres du contrat de location et de la comparaison essence (ligne unique)
CREATE TABLE parametres (
    id                    BIGINT PRIMARY KEY,
    contrat_debut         DATE             NOT NULL,
    contrat_duree_ans     INTEGER          NOT NULL,
    contrat_km            INTEGER          NOT NULL,
    km_debut              INTEGER          NOT NULL,
    penalite_par_km       DOUBLE PRECISION NOT NULL,
    conso_essence_l100    DOUBLE PRECISION NOT NULL
);

INSERT INTO parametres (id, contrat_debut, contrat_duree_ans, contrat_km, km_debut, penalite_par_km, conso_essence_l100)
VALUES (1, DATE '2025-09-01', 3, 30000, 16203, 0.0943, 4.5);

-- Données de référence : prix de l'électricité / essence, applicables à partir de date_effet
CREATE TABLE tarif (
    id                BIGSERIAL PRIMARY KEY,
    date_effet        DATE             NOT NULL UNIQUE,
    prix_hc           DOUBLE PRECISION NOT NULL,
    prix_hp           DOUBLE PRECISION NOT NULL,
    prix_parc_relai   DOUBLE PRECISION NOT NULL DEFAULT 0,
    prix_essence      DOUBLE PRECISION NOT NULL,
    heure_debut_hc    TIME             NOT NULL DEFAULT TIME '22:00',
    heure_fin_hc      TIME             NOT NULL DEFAULT TIME '06:00'
);

-- Sessions de recharge (onglet "Zoe")
CREATE TABLE recharge (
    id            BIGSERIAL PRIMARY KEY,
    lieu          VARCHAR(20)      NOT NULL,
    debut         TIMESTAMP        NOT NULL,
    fin           TIMESTAMP        NOT NULL,
    pct_debut     DOUBLE PRECISION,
    pct_fin       DOUBLE PRECISION,
    kwh           DOUBLE PRECISION NOT NULL,
    commentaire   VARCHAR(500),
    CONSTRAINT recharge_fin_apres_debut CHECK (fin >= debut)
);
CREATE INDEX idx_recharge_debut ON recharge (debut);

-- Relevés du compteur kilométrique (un par début de mois + relevé courant)
CREATE TABLE releve_km (
    id            BIGSERIAL PRIMARY KEY,
    date_releve   DATE    NOT NULL UNIQUE,
    kilometrage   INTEGER NOT NULL
);

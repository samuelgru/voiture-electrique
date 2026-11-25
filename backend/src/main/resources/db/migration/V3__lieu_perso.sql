-- Lieux de recharge supplémentaires (hors maison et parc relais), chacun avec son prix du kWh
CREATE TABLE lieu_perso (
    id        BIGSERIAL PRIMARY KEY,
    nom       VARCHAR(100)     NOT NULL UNIQUE,
    prix_kwh  DOUBLE PRECISION NOT NULL
);

ALTER TABLE recharge ADD COLUMN lieu_perso_id BIGINT REFERENCES lieu_perso (id);

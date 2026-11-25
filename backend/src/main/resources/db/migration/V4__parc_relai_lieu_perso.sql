-- Le parc relais devient un lieu personnalisé comme les autres (prix du kWh unique).
-- Prix repris : le dernier tarif non nul de l'ancienne colonne tarif.prix_parc_relai.
INSERT INTO lieu_perso (nom, prix_kwh)
SELECT 'Parc relais',
       COALESCE((SELECT prix_parc_relai FROM tarif WHERE prix_parc_relai > 0 ORDER BY date_effet DESC LIMIT 1), 0)
WHERE EXISTS (SELECT 1 FROM recharge WHERE lieu = 'PARC_RELAI')
   OR EXISTS (SELECT 1 FROM tarif WHERE prix_parc_relai > 0);

UPDATE recharge
SET lieu = 'AUTRE',
    lieu_perso_id = (SELECT id FROM lieu_perso WHERE nom = 'Parc relais')
WHERE lieu = 'PARC_RELAI';

ALTER TABLE tarif DROP COLUMN prix_parc_relai;

-- lieu = 'AUTRE' <=> un lieu_perso_id est renseigné (posée après la conversion des données existantes)
ALTER TABLE recharge ADD CONSTRAINT recharge_lieu_perso
    CHECK ((lieu = 'AUTRE') = (lieu_perso_id IS NOT NULL));

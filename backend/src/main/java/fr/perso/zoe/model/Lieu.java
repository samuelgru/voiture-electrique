package fr.perso.zoe.model;

/**
 * Lieu de recharge : à la maison (tarif HC/HP) ou {@link #AUTRE} :
 * un {@link LieuPerso} créé par l'utilisateur (parc relais, travail…), avec son prix du kWh.
 */
public enum Lieu {
    MAISON,
    AUTRE
}

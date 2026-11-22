package fr.perso.zoe.dto;

import java.time.LocalDate;

/** Une ligne du tableau mensuel de l'onglet "Synthèse". */
public record MoisDto(
        LocalDate mois,
        boolean enCours,
        Integer kmDebut,
        Integer kmMensuel,
        Double resteAFaire,
        Double kmParJour,
        double kwh,
        double cout,
        int nbRecharges,
        Double prixEssence,
        Double coutEssence,
        Double gain
) {}

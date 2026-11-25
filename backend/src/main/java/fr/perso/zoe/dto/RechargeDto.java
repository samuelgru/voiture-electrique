package fr.perso.zoe.dto;

import fr.perso.zoe.model.Lieu;

import java.time.LocalDateTime;

/** Session de recharge enrichie des colonnes calculées de l'onglet "Zoe". */
public record RechargeDto(
        Long id,
        Lieu lieu,
        Long lieuPersoId,
        String lieuNom,
        LocalDateTime debut,
        LocalDateTime fin,
        Double pctDebut,
        Double pctFin,
        double kwh,
        String commentaire,
        Double pctCharge,
        double nbHeures,
        double heuresHc,
        double heuresHp,
        double puissanceKw,
        double coutHc,
        double coutHp,
        double coutTotal
) {}

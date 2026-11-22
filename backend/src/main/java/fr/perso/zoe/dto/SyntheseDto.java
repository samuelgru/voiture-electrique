package fr.perso.zoe.dto;

import java.time.LocalDate;
import java.util.List;

public record SyntheseDto(
        // Contrat
        LocalDate contratDebut,
        LocalDate contratFin,
        int contratDureeAns,
        int contratKm,
        double kmParAn,
        double kmParMois,
        double kmParJour,
        // Kilométrage
        int kmDebut,
        Integer kmActuel,
        LocalDate dateKmActuel,
        Integer kmParcourus,
        int nbMois,
        double ecartContratMoisPrecedent,
        double ecartContratMois,
        double penaliteParKm,
        double penalite,
        Double kmProjetesFinContrat,
        Double ecartProjeteFinContrat,
        // Consommation électrique
        double kwhTotal,
        double coutElectricite,
        Double coutAu100km,
        Double kwhAu100km,
        // Comparaison essence
        double consoEssenceL100,
        Double prixLitreEssence,
        Double prixEssenceAu100km,
        double coutEssence,
        double gainTotal,
        Double gainParMois,
        List<MoisDto> mois
) {}

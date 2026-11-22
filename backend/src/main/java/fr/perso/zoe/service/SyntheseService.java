package fr.perso.zoe.service;

import fr.perso.zoe.dto.MoisDto;
import fr.perso.zoe.dto.RechargeDto;
import fr.perso.zoe.dto.SyntheseDto;
import fr.perso.zoe.model.Parametres;
import fr.perso.zoe.model.ReleveKm;
import fr.perso.zoe.model.Tarif;
import fr.perso.zoe.repository.ReleveKmRepository;
import fr.perso.zoe.repository.TarifRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Reproduit les calculs de l'onglet "Synthèse". */
@Service
public class SyntheseService {

    private final ParametresService parametresService;
    private final RechargeService rechargeService;
    private final TarifRepository tarifRepository;
    private final ReleveKmRepository releveRepository;
    private final CalculRechargeService calcul;
    private final ZoneId zone;

    public SyntheseService(ParametresService parametresService, RechargeService rechargeService,
                           TarifRepository tarifRepository, ReleveKmRepository releveRepository,
                           CalculRechargeService calcul, @Value("${zoe.timezone:Europe/Paris}") String zone) {
        this.parametresService = parametresService;
        this.rechargeService = rechargeService;
        this.tarifRepository = tarifRepository;
        this.releveRepository = releveRepository;
        this.calcul = calcul;
        this.zone = ZoneId.of(zone);
    }

    @Transactional(readOnly = true)
    public SyntheseDto calculer() {
        return calculer(LocalDate.now(zone));
    }

    @Transactional(readOnly = true)
    public SyntheseDto calculer(LocalDate aujourdhui) {
        Parametres p = parametresService.get();
        List<Tarif> tarifs = tarifRepository.findAllByOrderByDateEffetAsc();
        List<ReleveKm> releves = releveRepository.findAllByOrderByDateReleveAsc();
        List<RechargeDto> recharges = rechargeService.lister();

        // --- Contrat
        LocalDate contratDebut = p.getContratDebut().withDayOfMonth(1);
        LocalDate contratFin = contratDebut.plusYears(p.getContratDureeAns());
        double kmParAn = (double) p.getContratKm() / p.getContratDureeAns();
        double kmParMois = kmParAn / 12;
        double kmParJour = kmParMois / 30;

        // --- Relevés kilométriques
        ReleveKm dernier = releves.isEmpty() ? null : releves.get(releves.size() - 1);
        Integer kmActuel = dernier != null ? dernier.getKilometrage() : null;
        Integer kmParcourus = kmActuel != null ? kmActuel - p.getKmDebut() : null;

        // Premier relevé de chaque mois = kilométrage en début de mois
        Map<YearMonth, Integer> kmDebutMois = new HashMap<>();
        for (ReleveKm r : releves) {
            kmDebutMois.putIfAbsent(YearMonth.from(r.getDateReleve()), r.getKilometrage());
        }
        kmDebutMois.putIfAbsent(YearMonth.from(contratDebut), p.getKmDebut());

        // Recharges regroupées par mois de début
        Map<YearMonth, List<RechargeDto>> rechargesParMois = new HashMap<>();
        for (RechargeDto r : recharges) {
            rechargesParMois.computeIfAbsent(YearMonth.from(r.debut()), k -> new ArrayList<>()).add(r);
        }

        // --- Tableau mensuel : du début du contrat jusqu'au mois courant (borné à la fin du contrat)
        YearMonth premier = YearMonth.from(contratDebut);
        YearMonth courant = YearMonth.from(aujourdhui);
        YearMonth dernierMoisContrat = YearMonth.from(contratFin).minusMonths(1);
        YearMonth dernierMois = courant.isAfter(dernierMoisContrat) ? dernierMoisContrat : courant;

        List<MoisDto> mois = new ArrayList<>();
        double sommeResteAFaire = 0;
        double sommeResteAFaireMoisClos = 0;
        double coutEssenceTotal = 0;
        for (YearMonth m = premier; !m.isAfter(dernierMois); m = m.plusMonths(1)) {
            boolean enCours = m.equals(courant);
            Integer kmDebut = kmDebutMois.get(m);
            Integer kmFin = kmDebutMois.get(m.plusMonths(1));
            if (kmFin == null && dernier != null && !dernier.getDateReleve().isBefore(m.atDay(1))) {
                kmFin = dernier.getKilometrage();
            }
            Integer kmMensuel = (kmDebut != null && kmFin != null) ? kmFin - kmDebut : null;

            Double resteAFaire = null;
            Double kmJour = null;
            if (kmMensuel != null) {
                resteAFaire = kmParMois - kmMensuel;
                sommeResteAFaire += resteAFaire;
                if (!enCours) {
                    sommeResteAFaireMoisClos += resteAFaire;
                }
                int jours = enCours ? aujourdhui.getDayOfMonth() : m.lengthOfMonth();
                kmJour = jours > 0 ? (double) kmMensuel / jours : null;
            }

            List<RechargeDto> rm = rechargesParMois.getOrDefault(m, List.of());
            double kwh = rm.stream().mapToDouble(RechargeDto::kwh).sum();
            double cout = rm.stream().mapToDouble(RechargeDto::coutTotal).sum();

            Double prixEssence = calcul.tarifApplicable(tarifs, m.atDay(1)).map(Tarif::getPrixEssence).orElse(null);
            Double coutEssence = null;
            Double gain = null;
            if (prixEssence != null && kmMensuel != null) {
                coutEssence = kmMensuel / 100.0 * p.getConsoEssenceL100() * prixEssence;
                coutEssenceTotal += coutEssence;
                gain = coutEssence - cout;
            }

            mois.add(new MoisDto(m.atDay(1), enCours, kmDebut, kmMensuel, resteAFaire, kmJour, kwh, cout, rm.size(),
                    prixEssence, coutEssence, gain));
        }

        // --- Écart au contrat : positif = kilomètres en trop
        double ecartMoisPrecedent = -sommeResteAFaireMoisClos;
        double ecartMois = -sommeResteAFaire;
        double penalite = Math.max(0, ecartMoisPrecedent) * p.getPenaliteParKm();

        // --- Projection à la fin du contrat au rythme actuel
        Double kmProjetes = null;
        Double ecartProjete = null;
        if (kmParcourus != null && dernier != null) {
            long joursEcoules = ChronoUnit.DAYS.between(contratDebut, dernier.getDateReleve());
            long joursContrat = ChronoUnit.DAYS.between(contratDebut, contratFin);
            if (joursEcoules > 0) {
                kmProjetes = (double) kmParcourus / joursEcoules * joursContrat;
                ecartProjete = kmProjetes - p.getContratKm();
            }
        }

        // --- Consommation électrique (toutes les recharges)
        double kwhTotal = recharges.stream().mapToDouble(RechargeDto::kwh).sum();
        double coutElec = recharges.stream().mapToDouble(RechargeDto::coutTotal).sum();
        Double coutAu100 = (kmParcourus != null && kmParcourus > 0) ? coutElec / kmParcourus * 100 : null;
        Double kwhAu100 = (kmParcourus != null && kmParcourus > 0) ? kwhTotal / kmParcourus * 100 : null;

        // --- Comparaison essence
        Double prixLitre = calcul.tarifApplicable(tarifs, aujourdhui).map(Tarif::getPrixEssence).orElse(null);
        Double prixEssence100 = prixLitre != null ? p.getConsoEssenceL100() * prixLitre : null;
        double gainTotal = coutEssenceTotal - coutElec;
        long nbMoisAvecKm = mois.stream().filter(x -> x.kmMensuel() != null).count();
        Double gainParMois = nbMoisAvecKm > 0 ? gainTotal / nbMoisAvecKm : null;

        return new SyntheseDto(
                contratDebut, contratFin, p.getContratDureeAns(), p.getContratKm(), kmParAn, kmParMois, kmParJour,
                p.getKmDebut(), kmActuel, dernier != null ? dernier.getDateReleve() : null, kmParcourus,
                (int) nbMoisAvecKm, ecartMoisPrecedent, ecartMois, p.getPenaliteParKm(), penalite,
                kmProjetes, ecartProjete,
                kwhTotal, coutElec, coutAu100, kwhAu100,
                p.getConsoEssenceL100(), prixLitre, prixEssence100, coutEssenceTotal, gainTotal, gainParMois,
                mois);
    }
}

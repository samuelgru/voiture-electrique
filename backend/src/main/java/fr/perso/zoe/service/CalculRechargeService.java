package fr.perso.zoe.service;

import fr.perso.zoe.dto.RechargeDto;
import fr.perso.zoe.model.Lieu;
import fr.perso.zoe.model.Recharge;
import fr.perso.zoe.model.Tarif;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Reproduit les colonnes calculées de l'onglet "Zoe" :
 * durée, heures creuses / pleines, coût HC, coût HP et total.
 * <p>
 * La puissance est supposée constante pendant la recharge : coût HC = (kWh / heures) × heures HC × prix HC.
 */
@Service
public class CalculRechargeService {

    /** Tarif en vigueur à une date : le dernier dont la date d'effet est ≤ date (équivalent XLOOKUP(..., -1)). */
    public Optional<Tarif> tarifApplicable(List<Tarif> tarifsTries, LocalDate date) {
        Tarif trouve = null;
        for (Tarif t : tarifsTries) {
            if (!t.getDateEffet().isAfter(date)) {
                trouve = t;
            } else {
                break;
            }
        }
        if (trouve == null && !tarifsTries.isEmpty()) {
            trouve = tarifsTries.get(0);
        }
        return Optional.ofNullable(trouve);
    }

    public RechargeDto calculer(Recharge r, List<Tarif> tarifsTries) {
        List<Tarif> tries = tarifsTries.stream().sorted(Comparator.comparing(Tarif::getDateEffet)).toList();
        Optional<Tarif> tarif = tarifApplicable(tries, r.getDebut().toLocalDate());

        double nbHeures = heures(r.getDebut(), r.getFin());
        LocalTime debutHc = tarif.map(Tarif::getHeureDebutHc).orElse(LocalTime.of(22, 0));
        LocalTime finHc = tarif.map(Tarif::getHeureFinHc).orElse(LocalTime.of(6, 0));
        double heuresHc = heuresCreuses(r.getDebut(), r.getFin(), debutHc, finHc);
        double heuresHp = Math.max(0, nbHeures - heuresHc);

        double puissance = nbHeures > 0 ? r.getKwh() / nbHeures : 0;
        double prixHc = 0;
        double prixHp = 0;
        if (tarif.isPresent()) {
            Tarif t = tarif.get();
            if (r.getLieu() == Lieu.MAISON) {
                prixHc = t.getPrixHc();
                prixHp = t.getPrixHp();
            } else {
                prixHc = t.getPrixParcRelai();
                prixHp = t.getPrixParcRelai();
            }
        }
        double coutHc;
        double coutHp;
        if (nbHeures > 0) {
            coutHc = puissance * heuresHc * prixHc;
            coutHp = puissance * heuresHp * prixHp;
        } else {
            // Durée nulle : tout est compté au tarif HP
            coutHc = 0;
            coutHp = r.getKwh() * prixHp;
        }

        Double pctCharge = (r.getPctDebut() != null && r.getPctFin() != null) ? r.getPctFin() - r.getPctDebut() : null;

        return new RechargeDto(
                r.getId(), r.getLieu(), r.getDebut(), r.getFin(), r.getPctDebut(), r.getPctFin(), r.getKwh(),
                r.getCommentaire(), pctCharge, nbHeures, heuresHc, heuresHp, puissance, coutHc, coutHp, coutHc + coutHp);
    }

    static double heures(LocalDateTime debut, LocalDateTime fin) {
        return Duration.between(debut, fin).toSeconds() / 3600.0;
    }

    /**
     * Nombre d'heures de l'intervalle [debut, fin] tombant dans la plage d'heures creuses quotidienne
     * [debutHc, finHc] (la plage peut chevaucher minuit, ex. 22:00 → 06:20).
     */
    static double heuresCreuses(LocalDateTime debut, LocalDateTime fin, LocalTime debutHc, LocalTime finHc) {
        if (!fin.isAfter(debut)) {
            return 0;
        }
        boolean chevaucheMinuit = !finHc.isAfter(debutHc);
        long secondes = 0;
        for (LocalDate jour = debut.toLocalDate().minusDays(1); !jour.isAfter(fin.toLocalDate()); jour = jour.plusDays(1)) {
            LocalDateTime hcDebut = jour.atTime(debutHc);
            LocalDateTime hcFin = chevaucheMinuit ? jour.plusDays(1).atTime(finHc) : jour.atTime(finHc);
            LocalDateTime a = debut.isAfter(hcDebut) ? debut : hcDebut;
            LocalDateTime b = fin.isBefore(hcFin) ? fin : hcFin;
            if (b.isAfter(a)) {
                secondes += Duration.between(a, b).toSeconds();
            }
        }
        return secondes / 3600.0;
    }
}

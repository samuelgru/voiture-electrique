package fr.perso.zoe.service;

import fr.perso.zoe.dto.RechargeDto;
import fr.perso.zoe.model.Lieu;
import fr.perso.zoe.model.Recharge;
import fr.perso.zoe.model.Tarif;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CalculRechargeServiceTest {

    private final CalculRechargeService service = new CalculRechargeService();

    private static Tarif tarif(LocalDate date, LocalTime finHc) {
        Tarif t = new Tarif();
        t.setDateEffet(date);
        t.setPrixHc(0.1635);
        t.setPrixHp(0.2081);
        t.setPrixEssence(1.7);
        t.setHeureDebutHc(LocalTime.of(22, 0));
        t.setHeureFinHc(finHc);
        return t;
    }

    private static Recharge recharge(Lieu lieu, LocalDateTime debut, LocalDateTime fin, double kwh) {
        Recharge r = new Recharge();
        r.setLieu(lieu);
        r.setDebut(debut);
        r.setFin(fin);
        r.setKwh(kwh);
        return r;
    }

    /** Ligne 2 de l'onglet Zoe : 05/09/2025 21:37 → 06/09/2025 08:49, 16,85 kWh, total 2,9697 €. */
    @Test
    void reproduitLaLigne2DuClasseur() {
        Recharge r = recharge(Lieu.MAISON, LocalDateTime.of(2025, 9, 5, 21, 37), LocalDateTime.of(2025, 9, 6, 8, 49), 16.85);
        RechargeDto dto = service.calculer(r, List.of(tarif(LocalDate.of(2025, 9, 1), LocalTime.of(6, 0))));

        assertThat(dto.nbHeures()).isCloseTo(11.2, within(1e-9));
        assertThat(dto.heuresHc()).isCloseTo(8.0, within(1e-9));
        assertThat(dto.heuresHp()).isCloseTo(3.2, within(1e-9));
        assertThat(dto.coutHc()).isCloseTo(1.96784, within(1e-4));
        assertThat(dto.coutHp()).isCloseTo(1.00185, within(1e-4));
        assertThat(dto.coutTotal()).isCloseTo(2.96969, within(1e-4));
    }

    @Test
    void lieuPersonnaliseAuPrixDuKwhSansHeuresCreuses() {
        fr.perso.zoe.model.LieuPerso lieu = new fr.perso.zoe.model.LieuPerso();
        lieu.setNom("Borne du travail");
        lieu.setPrixKwh(0.40);
        // 22:00 → 08:00 : tombe en partie dans les HC de la maison, mais le prix reste 0,40 €/kWh
        Recharge r = recharge(Lieu.AUTRE, LocalDateTime.of(2026, 1, 10, 22, 0), LocalDateTime.of(2026, 1, 11, 8, 0), 10);
        r.setLieuPerso(lieu);
        RechargeDto dto = service.calculer(r, List.of(tarif(LocalDate.of(2025, 9, 1), LocalTime.of(6, 0))));
        assertThat(dto.coutTotal()).isCloseTo(4.0, within(1e-9));
        assertThat(dto.lieuNom()).isEqualTo("Borne du travail");
    }

    @Test
    void plageHcAvecFinA0620() {
        Recharge r = recharge(Lieu.MAISON, LocalDateTime.of(2026, 1, 10, 22, 30), LocalDateTime.of(2026, 1, 11, 6, 16), 20);
        RechargeDto dto = service.calculer(r, List.of(tarif(LocalDate.of(2025, 9, 1), LocalTime.of(6, 20))));
        assertThat(dto.heuresHp()).isCloseTo(0, within(1e-9));
        assertThat(dto.heuresHc()).isCloseTo(dto.nbHeures(), within(1e-9));
    }

    @Test
    void rechargeSurPlusieursJours() {
        double hc = CalculRechargeService.heuresCreuses(LocalDateTime.of(2026, 1, 1, 12, 0), LocalDateTime.of(2026, 1, 3, 12, 0),
                LocalTime.of(22, 0), LocalTime.of(6, 0));
        assertThat(hc).isCloseTo(16, within(1e-9));
    }
}

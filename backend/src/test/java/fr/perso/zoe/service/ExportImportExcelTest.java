package fr.perso.zoe.service;

import fr.perso.zoe.dto.ImportResultDto;
import fr.perso.zoe.dto.RechargeDto;
import fr.perso.zoe.model.*;
import fr.perso.zoe.repository.LieuPersoRepository;
import fr.perso.zoe.repository.RechargeRepository;
import fr.perso.zoe.repository.ReleveKmRepository;
import fr.perso.zoe.repository.TarifRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Un fichier exporté doit pouvoir être réimporté sans perte. */
class ExportImportExcelTest {

    private static Tarif tarif(LocalDate date, double hc, double hp, LocalTime debutHc, LocalTime finHc) {
        Tarif t = new Tarif();
        t.setDateEffet(date);
        t.setPrixHc(hc);
        t.setPrixHp(hp);
        t.setPrixEssence(1.9);
        t.setHeureDebutHc(debutHc);
        t.setHeureFinHc(finHc);
        return t;
    }

    private static RechargeDto dto(Lieu lieu, LocalDateTime debut, LocalDateTime fin, Double pctDebut, Double pctFin,
                                   double kwh, String commentaire) {
        return new RechargeDto(null, lieu, null, lieu == Lieu.AUTRE ? "Borne du travail" : null, debut, fin, pctDebut, pctFin, kwh, commentaire,
                pctDebut != null && pctFin != null ? pctFin - pctDebut : null, 11.2, 8, 3.2, 1.5, 1.97, 1.0, 2.97);
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportPuisImportRestitueLesDonnees() throws Exception {
        Parametres p = new Parametres();
        p.setContratDebut(LocalDate.of(2025, 9, 1));
        p.setContratDureeAns(3);
        p.setContratKm(30000);
        p.setKmDebut(16203);
        p.setPenaliteParKm(0.0943);
        p.setConsoEssenceL100(4.5);

        List<Tarif> tarifs = List.of(
                tarif(LocalDate.of(2025, 9, 1), 0.1635, 0.2081, LocalTime.of(22, 0), LocalTime.of(6, 0)),
                tarif(LocalDate.of(2026, 2, 1), 0.1579, 0.2065, LocalTime.of(23, 0), LocalTime.of(6, 20)));
        List<RechargeDto> recharges = List.of(
                dto(Lieu.MAISON, LocalDateTime.of(2025, 9, 5, 21, 37), LocalDateTime.of(2025, 9, 6, 8, 49), 0.67, 1.0, 16.85, "Première recharge"),
                dto(Lieu.AUTRE, LocalDateTime.of(2026, 3, 5, 12, 0), LocalDateTime.of(2026, 3, 5, 14, 0), null, null, 10, null));
        LieuPerso travail = new LieuPerso();
        travail.setNom("Borne du travail");
        travail.setPrixKwh(0.42);
        List<ReleveKm> releves = List.of(
                new ReleveKm(LocalDate.of(2025, 9, 1), 16203),
                new ReleveKm(LocalDate.of(2025, 10, 1), 17517),
                new ReleveKm(LocalDate.of(2026, 3, 15), 28396));

        byte[] fichier = ExportExcelService.ecrire(p, recharges, tarifs, releves, List.of(travail));

        RechargeRepository rechargeRepo = mock(RechargeRepository.class);
        TarifRepository tarifRepo = mock(TarifRepository.class);
        ReleveKmRepository releveRepo = mock(ReleveKmRepository.class);
        LieuPersoRepository lieuRepo = mock(LieuPersoRepository.class);
        ParametresService parametresService = mock(ParametresService.class);
        Parametres cible = new Parametres();
        org.mockito.Mockito.when(parametresService.get()).thenReturn(cible);

        ImportResultDto resultat = new ImportExcelService(rechargeRepo, tarifRepo, releveRepo, lieuRepo, parametresService, "Europe/Paris")
                .importer(new ByteArrayInputStream(fichier));

        assertThat(resultat.recharges()).isEqualTo(2);
        assertThat(resultat.tarifs()).isEqualTo(2);
        assertThat(resultat.releves()).isEqualTo(3);
        assertThat(resultat.avertissements()).isEmpty();

        assertThat(cible.getContratDebut()).isEqualTo(p.getContratDebut());
        assertThat(cible.getContratDureeAns()).isEqualTo(3);
        assertThat(cible.getContratKm()).isEqualTo(30000);
        assertThat(cible.getKmDebut()).isEqualTo(16203);
        assertThat(cible.getPenaliteParKm()).isCloseTo(0.0943, within(1e-9));
        assertThat(cible.getConsoEssenceL100()).isCloseTo(4.5, within(1e-9));

        ArgumentCaptor<List<Recharge>> rc = ArgumentCaptor.forClass(List.class);
        verify(rechargeRepo).saveAll(rc.capture());
        List<Recharge> lues = rc.getValue();
        assertThat(lues).hasSize(2);
        assertThat(lues.get(0).getLieu()).isEqualTo(Lieu.MAISON);
        assertThat(lues.get(0).getDebut()).isEqualTo(LocalDateTime.of(2025, 9, 5, 21, 37));
        assertThat(lues.get(0).getFin()).isEqualTo(LocalDateTime.of(2025, 9, 6, 8, 49));
        assertThat(lues.get(0).getPctDebut()).isCloseTo(0.67, within(1e-9));
        assertThat(lues.get(0).getPctFin()).isCloseTo(1.0, within(1e-9));
        assertThat(lues.get(0).getKwh()).isCloseTo(16.85, within(1e-9));
        assertThat(lues.get(0).getCommentaire()).isEqualTo("Première recharge");
        assertThat(lues.get(1).getLieu()).isEqualTo(Lieu.AUTRE);
        assertThat(lues.get(1).getPctDebut()).isNull();
        assertThat(lues.get(1).getCommentaire()).isNull();
        assertThat(lues.get(1).getLieuPerso().getNom()).isEqualTo("Borne du travail");
        assertThat(lues.get(1).getLieuPerso().getPrixKwh()).isCloseTo(0.42, within(1e-9));

        ArgumentCaptor<Iterable<Tarif>> tc = ArgumentCaptor.forClass(Iterable.class);
        verify(tarifRepo).saveAll(tc.capture());
        List<Tarif> tarifsLus = List.copyOf((java.util.Collection<Tarif>) tc.getValue());
        assertThat(tarifsLus).hasSize(2);
        assertThat(tarifsLus.get(1).getDateEffet()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(tarifsLus.get(1).getPrixHc()).isCloseTo(0.1579, within(1e-9));
        assertThat(tarifsLus.get(1).getPrixHp()).isCloseTo(0.2065, within(1e-9));
        assertThat(tarifsLus.get(1).getPrixEssence()).isCloseTo(1.9, within(1e-9));
        assertThat(tarifsLus.get(0).getHeureDebutHc()).isEqualTo(LocalTime.of(22, 0));
        assertThat(tarifsLus.get(1).getHeureDebutHc()).isEqualTo(LocalTime.of(23, 0));
        assertThat(tarifsLus.get(1).getHeureFinHc()).isEqualTo(LocalTime.of(6, 20));

        ArgumentCaptor<Iterable<ReleveKm>> kc = ArgumentCaptor.forClass(Iterable.class);
        verify(releveRepo).saveAll(kc.capture());
        List<ReleveKm> relevesLus = List.copyOf((java.util.Collection<ReleveKm>) kc.getValue());
        // le dernier relevé n'est pas dupliqué à la date du jour
        assertThat(relevesLus).extracting(ReleveKm::getDateReleve)
                .containsExactly(LocalDate.of(2025, 9, 1), LocalDate.of(2025, 10, 1), LocalDate.of(2026, 3, 15));
        assertThat(relevesLus).extracting(ReleveKm::getKilometrage).containsExactly(16203, 17517, 28396);
    }
}

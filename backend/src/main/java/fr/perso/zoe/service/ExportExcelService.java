package fr.perso.zoe.service;

import fr.perso.zoe.dto.RechargeDto;
import fr.perso.zoe.model.LieuPerso;
import fr.perso.zoe.model.Parametres;
import fr.perso.zoe.model.ReleveKm;
import fr.perso.zoe.model.Tarif;
import fr.perso.zoe.repository.LieuPersoRepository;
import fr.perso.zoe.repository.ReleveKmRepository;
import fr.perso.zoe.repository.TarifRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/**
 * Exporte les données au format attendu par {@link ImportExcelService} (mêmes onglets, mêmes cellules) :
 * un fichier exporté peut être réimporté tel quel, et sert de modèle pour préparer un import.
 * Les colonnes calculées sont écrites en valeurs, à titre indicatif : l'import les ignore.
 */
@Service
public class ExportExcelService {

    private final ParametresService parametresService;
    private final RechargeService rechargeService;
    private final TarifRepository tarifRepository;
    private final ReleveKmRepository releveRepository;
    private final LieuPersoRepository lieuRepository;

    public ExportExcelService(ParametresService parametresService, RechargeService rechargeService,
                              TarifRepository tarifRepository, ReleveKmRepository releveRepository,
                              LieuPersoRepository lieuRepository) {
        this.lieuRepository = lieuRepository;
        this.parametresService = parametresService;
        this.rechargeService = rechargeService;
        this.tarifRepository = tarifRepository;
        this.releveRepository = releveRepository;
    }

    @Transactional(readOnly = true)
    public byte[] exporter() throws IOException {
        return ecrire(parametresService.get(),
                rechargeService.lister(),
                tarifRepository.findAllByOrderByDateEffetAsc(),
                releveRepository.findAllByOrderByDateReleveAsc(),
                lieuRepository.findAllByOrderByNomAsc());
    }

    static byte[] ecrire(Parametres p, List<RechargeDto> recharges, List<Tarif> tarifs, List<ReleveKm> releves,
                         List<LieuPerso> lieux)
            throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles st = new Styles(wb);
            synthese(wb, st, p, releves);
            zoe(wb, st, recharges);
            prix(wb, st, tarifs);
            lieux(wb, st, lieux);
            lisezMoi(wb, st);
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ---------------------------------------------------------------- onglets

    private static void synthese(Workbook wb, Styles st, Parametres p, List<ReleveKm> releves) {
        Sheet s = wb.createSheet("Synthèse");
        texte(s, "A1", "Contrat", st.entete);
        nombre(s, "B1", p.getContratDureeAns(), null);
        texte(s, "C1", "ans", null);
        nombre(s, "A2", p.getContratKm(), null);
        texte(s, "B2", "Kms", null);
        texte(s, "E1", "Kilométrage", st.entete);
        texte(s, "E2", "Début", null);
        nombre(s, "F2", p.getKmDebut(), null);
        texte(s, "E3", "Actuel", null);
        if (!releves.isEmpty()) {
            nombre(s, "F3", releves.get(releves.size() - 1).getKilometrage(), null);
        }
        texte(s, "E4", "Début contrat", null);
        date(s, "F4", p.getContratDebut(), st.date);
        texte(s, "G5", "Comparaison Essence", st.entete);
        texte(s, "K5", "pénalité", null);
        nombre(s, "L5", p.getPenaliteParKm(), null);
        texte(s, "M5", "par km de trop (€)", null);
        texte(s, "G6", "Consommation / 100 kms", null);
        nombre(s, "I6", p.getConsoEssenceL100(), null);

        texte(s, "A10", "Mois", st.entete);
        texte(s, "C10", "Kilométrage", st.entete);
        int ligne = 11;
        for (ReleveKm r : releves) {
            Row row = s.createRow(ligne++);
            Cell a = row.createCell(0);
            a.setCellValue(r.getDateReleve());
            a.setCellStyle(st.date);
            row.createCell(2).setCellValue(r.getKilometrage());
        }
        s.setColumnWidth(0, 18 * 256);
        s.setColumnWidth(4, 16 * 256);
        s.setColumnWidth(5, 14 * 256);
        s.setColumnWidth(6, 24 * 256);
    }

    private static void zoe(Workbook wb, Styles st, List<RechargeDto> recharges) {
        Sheet s = wb.createSheet("Zoe");
        String[] titres = {"Lieu", "début", "fin", "% debut", "%fin", "nb %", "nb Watt/h", "nb Heures", "", "",
                "Nb HC", "Prix HC", "Nb HP", "Prix HP", "Total", "Commentaire"};
        Row entete = s.createRow(0);
        for (int i = 0; i < titres.length; i++) {
            if (titres[i].isEmpty()) continue;
            Cell c = entete.createCell(i);
            c.setCellValue(titres[i]);
            // colonnes saisies : en gras ; colonnes calculées (ignorées à l'import) : grisées
            boolean saisie = i <= 4 || i == 6 || i == 15;
            c.setCellStyle(saisie ? st.entete : st.calcule);
        }
        int ligne = 1;
        for (RechargeDto r : recharges.stream().sorted(Comparator.comparing(RechargeDto::debut)).toList()) {
            Row row = s.createRow(ligne++);
            row.createCell(0).setCellValue(libelleLieu(r));
            Cell debut = row.createCell(1);
            debut.setCellValue(r.debut());
            debut.setCellStyle(st.dateHeure);
            Cell fin = row.createCell(2);
            fin.setCellValue(r.fin());
            fin.setCellStyle(st.dateHeure);
            pourcentage(row, 3, r.pctDebut(), st);
            pourcentage(row, 4, r.pctFin(), st);
            pourcentage(row, 5, r.pctCharge(), st);
            row.createCell(6).setCellValue(r.kwh());
            row.createCell(7).setCellValue(arrondi(r.nbHeures()));
            row.createCell(10).setCellValue(arrondi(r.heuresHc()));
            row.createCell(11).setCellValue(arrondi(r.coutHc()));
            row.createCell(12).setCellValue(arrondi(r.heuresHp()));
            row.createCell(13).setCellValue(arrondi(r.coutHp()));
            row.createCell(14).setCellValue(arrondi(r.coutTotal()));
            if (r.commentaire() != null && !r.commentaire().isBlank()) {
                row.createCell(ImportExcelService.COL_COMMENTAIRE).setCellValue(r.commentaire());
            }
        }
        s.createFreezePane(0, 1);
        s.setColumnWidth(0, 12 * 256);
        s.setColumnWidth(1, 18 * 256);
        s.setColumnWidth(2, 18 * 256);
        s.setColumnWidth(ImportExcelService.COL_COMMENTAIRE, 40 * 256);
    }

    private static String libelleLieu(RechargeDto r) {
        return switch (r.lieu()) {
            case MAISON -> "maison";
            case AUTRE -> r.lieuNom();
        };
    }

    private static void lieux(Workbook wb, Styles st, List<LieuPerso> lieux) {
        Sheet s = wb.createSheet("Lieux");
        String[] titres = {"Lieu", "Prix kWh"};
        Row entete = s.createRow(0);
        for (int i = 0; i < titres.length; i++) {
            Cell c = entete.createCell(i);
            c.setCellValue(titres[i]);
            c.setCellStyle(st.entete);
        }
        int ligne = 1;
        for (LieuPerso l : lieux) {
            Row row = s.createRow(ligne++);
            row.createCell(0).setCellValue(l.getNom());
            row.createCell(1).setCellValue(l.getPrixKwh());
        }
        s.setColumnWidth(0, 28 * 256);
        s.setColumnWidth(1, 12 * 256);
    }

    private static void prix(Workbook wb, Styles st, List<Tarif> tarifs) {
        Sheet s = wb.createSheet("Prix Electricité");
        texte(s, "A1", "Prix Electricité", st.entete);
        texte(s, "E1", "Essence", st.entete);
        texte(s, "F1", "Heures creuses", st.entete);
        String[] titres = {"Date", "HC", "HP", "", "le litre", "début HC", "fin HC"};
        Row entete = s.createRow(1);
        for (int i = 0; i < titres.length; i++) {
            if (titres[i].isEmpty()) continue;
            Cell c = entete.createCell(i);
            c.setCellValue(titres[i]);
            c.setCellStyle(st.entete);
        }
        int ligne = 2;
        for (Tarif t : tarifs) {
            Row row = s.createRow(ligne++);
            Cell d = row.createCell(0);
            d.setCellValue(t.getDateEffet());
            d.setCellStyle(st.date);
            row.createCell(1).setCellValue(t.getPrixHc());
            row.createCell(2).setCellValue(t.getPrixHp());
            row.createCell(4).setCellValue(t.getPrixEssence());
            heure(row, 5, t.getHeureDebutHc(), st);
            heure(row, 6, t.getHeureFinHc(), st);
        }
        s.setColumnWidth(0, 14 * 256);
        s.setColumnWidth(3, 12 * 256);
        s.setColumnWidth(5, 14 * 256);
    }

    private static void lisezMoi(Workbook wb, Styles st) {
        Sheet s = wb.createSheet("Lisez-moi");
        String[] lignes = {
                "Format du fichier d'import / export de « Ma Zoé »",
                "",
                "L'import remplace TOUTES les recharges, tarifs et relevés kilométriques. Les onglets sont reconnus par leur nom.",
                "",
                "Onglet « Zoe » : une ligne par recharge, titres en ligne 1, données dès la ligne 2.",
                "  A Lieu : « maison » ou le nom d'un lieu de l'onglet « Lieux »   B début / C fin : date et heure   D % début / E % fin : entre 0 et 1 (ex. 0,67)",
                "  G nb Watt/h : énergie en kWh   P Commentaire : facultatif.   Colonnes grisées (F, H, K à O) : calculées, ignorées à l'import.",
                "",
                "Onglet « Prix Electricité » : titres en ligne 2, données dès la ligne 3, un tarif par date d'effet.",
                "  A Date   B HC (€/kWh)   C HP (€/kWh)   D (inutilisée ; l'ancien prix « Parc relai » n'est lu que pour créer ce lieu)   E Essence (€/litre)   F début HC / G fin HC : facultatifs (défaut 22:00 → 06:00).",
                "",
                "Onglet « Lieux » (facultatif) : titres en ligne 1, un lieu de recharge par ligne : A nom   B prix du kWh (€). Un lieu cité dans « Zoe » mais absent ici est créé à 0 €.",
                "",
                "Onglet « Synthèse » : paramètres aux cellules fixes, relevés kilométriques dès la ligne 12.",
                "  B1 durée du contrat (ans)   A2 km du contrat   F2 compteur au début   F3 compteur actuel   F4 début du contrat (facultatif)",
                "  L5 pénalité (€/km en trop)   I6 consommation essence (L/100 km)",
                "  Relevés : A = date, C = kilométrage (un par mois suffit)."
        };
        for (int i = 0; i < lignes.length; i++) {
            Row row = s.createRow(i);
            Cell c = row.createCell(0);
            c.setCellValue(lignes[i]);
            if (i == 0) c.setCellStyle(st.entete);
        }
        s.setColumnWidth(0, 140 * 256);
    }

    // ---------------------------------------------------------------- utilitaires

    private static final class Styles {
        final CellStyle entete;
        final CellStyle calcule;
        final CellStyle date;
        final CellStyle dateHeure;
        final CellStyle pourcent;
        final CellStyle heure;

        Styles(Workbook wb) {
            DataFormat df = wb.createDataFormat();
            Font gras = wb.createFont();
            gras.setBold(true);
            entete = wb.createCellStyle();
            entete.setFont(gras);
            Font gris = wb.createFont();
            gris.setItalic(true);
            gris.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            calcule = wb.createCellStyle();
            calcule.setFont(gris);
            date = wb.createCellStyle();
            date.setDataFormat(df.getFormat("dd/mm/yyyy"));
            dateHeure = wb.createCellStyle();
            dateHeure.setDataFormat(df.getFormat("dd/mm/yyyy hh:mm"));
            pourcent = wb.createCellStyle();
            pourcent.setDataFormat(df.getFormat("0%"));
            heure = wb.createCellStyle();
            heure.setDataFormat(df.getFormat("hh:mm"));
        }
    }

    private static Cell cellule(Sheet s, String ref) {
        org.apache.poi.ss.util.CellReference cr = new org.apache.poi.ss.util.CellReference(ref);
        Row row = s.getRow(cr.getRow());
        if (row == null) row = s.createRow(cr.getRow());
        return row.createCell(cr.getCol());
    }

    private static void texte(Sheet s, String ref, String valeur, CellStyle style) {
        Cell c = cellule(s, ref);
        c.setCellValue(valeur);
        if (style != null) c.setCellStyle(style);
    }

    private static void nombre(Sheet s, String ref, double valeur, CellStyle style) {
        Cell c = cellule(s, ref);
        c.setCellValue(valeur);
        if (style != null) c.setCellStyle(style);
    }

    private static void date(Sheet s, String ref, LocalDate valeur, CellStyle style) {
        Cell c = cellule(s, ref);
        c.setCellValue(valeur);
        c.setCellStyle(style);
    }

    private static void pourcentage(Row row, int col, Double valeur, Styles st) {
        if (valeur == null) return;
        Cell c = row.createCell(col);
        c.setCellValue(valeur);
        c.setCellStyle(st.pourcent);
    }

    private static void heure(Row row, int col, LocalTime valeur, Styles st) {
        Cell c = row.createCell(col);
        c.setCellValue(valeur.toSecondOfDay() / 86400.0);
        c.setCellStyle(st.heure);
    }

    private static double arrondi(double v) {
        return Math.round(v * 10000) / 10000.0;
    }
}

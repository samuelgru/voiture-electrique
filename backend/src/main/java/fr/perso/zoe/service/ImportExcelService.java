package fr.perso.zoe.service;

import fr.perso.zoe.dto.ImportResultDto;
import fr.perso.zoe.model.*;
import fr.perso.zoe.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Importe le classeur Excel historique (onglets "Zoe", "Prix Electricité" et "Synthèse").
 * Seules les données saisies sont lues : tous les calculs sont refaits par l'application.
 * L'import remplace intégralement les données existantes.
 */
@Service
public class ImportExcelService {

    /** Heure de fin des HC dans la formule de la colonne "HC apres", ex. INT(C2)+"06:20". */
    private static final Pattern FIN_HC = Pattern.compile("INT\\(C\\d+\\)\\+\"(\\d{1,2}:\\d{2})\"");
    /** Heure de début des HC dans la formule de la colonne "HC avt", ex. INT(B2)+"22:00". */
    private static final Pattern DEBUT_HC = Pattern.compile("INT\\(B\\d+\\)\\+\"(\\d{1,2}:\\d{2})\"");

    private final RechargeRepository rechargeRepository;
    private final TarifRepository tarifRepository;
    private final ReleveKmRepository releveRepository;
    private final LieuPersoRepository lieuRepository;
    private final ParametresService parametresService;
    private final ZoneId zone;

    public ImportExcelService(RechargeRepository rechargeRepository, TarifRepository tarifRepository,
                              ReleveKmRepository releveRepository, LieuPersoRepository lieuRepository,
                              ParametresService parametresService,
                              @Value("${zoe.timezone:Europe/Paris}") String zone) {
        this.lieuRepository = lieuRepository;
        this.rechargeRepository = rechargeRepository;
        this.tarifRepository = tarifRepository;
        this.releveRepository = releveRepository;
        this.parametresService = parametresService;
        this.zone = ZoneId.of(zone);
    }

    /** Colonne du commentaire dans l'onglet "Zoe" (absente du classeur historique). */
    static final int COL_COMMENTAIRE = 15;

    private record PlageHc(LocalDate date, LocalTime debut, LocalTime fin) {}

    @Transactional
    public ImportResultDto importer(InputStream in) throws IOException {
        List<String> avertissements = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(in)) {
            Sheet zoe = feuille(wb, "zoe");
            Sheet prix = feuille(wb, "prix electricite");
            Sheet synthese = feuille(wb, "synthese");
            if (zoe == null || prix == null) {
                throw new IllegalArgumentException("Onglets \"Zoe\" et \"Prix Electricité\" introuvables");
            }

            // ---------- Lieux supplémentaires (onglet facultatif)
            Map<String, LieuPerso> lieux = new LinkedHashMap<>();
            Sheet feuilleLieux = feuille(wb, "lieux");
            if (feuilleLieux != null) {
                for (int i = 1; i <= feuilleLieux.getLastRowNum(); i++) {
                    Row row = feuilleLieux.getRow(i);
                    if (row == null) continue;
                    String nom = texte(row.getCell(0));
                    Double prixKwh = nombre(row.getCell(1));
                    if (nom == null || nom.isBlank()) continue;
                    lieux.putIfAbsent(normaliser(nom), nouveauLieu(nom, prixKwh == null ? 0 : prixKwh));
                }
            }

            // ---------- Recharges
            List<Recharge> recharges = new ArrayList<>();
            List<PlageHc> plages = new ArrayList<>();
            for (int i = 1; i <= zoe.getLastRowNum(); i++) {
                Row row = zoe.getRow(i);
                if (row == null) continue;
                String lieu = texte(row.getCell(0));
                LocalDateTime debut = dateHeure(row.getCell(1));
                LocalDateTime fin = dateHeure(row.getCell(2));
                Double kwh = nombre(row.getCell(6));
                if (lieu == null || lieu.isBlank() || debut == null || fin == null || kwh == null) {
                    if (lieu != null && !lieu.isBlank()) {
                        avertissements.add("Zoe ligne " + (i + 1) + " ignorée (données incomplètes)");
                    }
                    continue;
                }
                Recharge r = new Recharge();
                String cle = normaliser(lieu);
                if (cle.equals("maison")) {
                    r.setLieu(Lieu.MAISON);
                } else {
                    boolean parcRelai = cle.equals("parc relai");
                    String cleLieu = parcRelai ? "parc relais" : cle;
                    r.setLieu(Lieu.AUTRE);
                    r.setLieuPerso(lieux.computeIfAbsent(cleLieu, k -> {
                        // Classeur historique : le parc relais avait son prix dans l'onglet « Prix Electricité » (colonne D)
                        double prixKwh = parcRelai ? ancienPrixParcRelai(prix) : 0;
                        // Un parc relais à 0 € est légitime (recharge gratuite) : pas d'avertissement
                        if (!parcRelai) {
                            avertissements.add("Lieu « " + lieu.trim() + " » créé à 0 €/kWh (absent de l'onglet « Lieux »)");
                        }
                        return nouveauLieu(parcRelai ? "Parc relais" : lieu, prixKwh);
                    }));
                }
                r.setDebut(debut);
                r.setFin(fin);
                r.setPctDebut(nombre(row.getCell(3)));
                r.setPctFin(nombre(row.getCell(4)));
                r.setKwh(kwh);
                String commentaire = texte(row.getCell(COL_COMMENTAIRE));
                if (commentaire != null && !commentaire.isBlank()) r.setCommentaire(commentaire.trim());
                recharges.add(r);

                LocalTime hcDebut = heureFormule(row.getCell(8), DEBUT_HC);
                LocalTime hcFin = heureFormule(row.getCell(9), FIN_HC);
                if (r.getLieu() == Lieu.MAISON && hcDebut != null && hcFin != null) {
                    plages.add(new PlageHc(debut.toLocalDate(), hcDebut, hcFin));
                }
            }

            // ---------- Tarifs
            TreeMap<LocalDate, Tarif> tarifs = new TreeMap<>();
            Map<LocalDate, LocalTime[]> plagesExplicites = new HashMap<>();
            for (int i = 2; i <= prix.getLastRowNum(); i++) {
                Row row = prix.getRow(i);
                if (row == null) continue;
                LocalDateTime date = dateHeure(row.getCell(0));
                Double hc = nombre(row.getCell(1));
                Double hp = nombre(row.getCell(2));
                if (date == null || hc == null || hp == null) continue;
                Tarif t = new Tarif();
                t.setDateEffet(date.toLocalDate());
                t.setPrixHc(hc);
                t.setPrixHp(hp);
                t.setPrixEssence(Optional.ofNullable(nombre(row.getCell(4))).orElse(0.0));
                tarifs.put(t.getDateEffet(), t);
                LocalTime hcDebut = heure(row.getCell(5));
                LocalTime hcFin = heure(row.getCell(6));
                if (hcDebut != null && hcFin != null) {
                    plagesExplicites.put(t.getDateEffet(), new LocalTime[]{hcDebut, hcFin});
                }
            }
            appliquerPlagesHc(tarifs, plages);
            // Les heures creuses saisies dans les colonnes F et G (fichier exporté par l'appli) priment sur les formules
            plagesExplicites.forEach((date, h) -> {
                Tarif t = tarifs.get(date);
                t.setHeureDebutHc(h[0]);
                t.setHeureFinHc(h[1]);
            });

            // ---------- Synthèse : paramètres du contrat et relevés kilométriques
            List<ReleveKm> releves = new ArrayList<>();
            boolean parametresImportes = false;
            if (synthese != null) {
                Parametres p = parametresService.get();
                Double duree = nombre(cellule(synthese, "B1"));
                Double kmContrat = nombre(cellule(synthese, "A2"));
                Double kmDebut = nombre(cellule(synthese, "F2"));
                Double consoEssence = nombre(cellule(synthese, "I6"));
                Double penalite = nombre(cellule(synthese, "L5"));
                if (duree != null) p.setContratDureeAns(duree.intValue());
                if (kmContrat != null) p.setContratKm(kmContrat.intValue());
                if (kmDebut != null) p.setKmDebut(kmDebut.intValue());
                if (consoEssence != null) p.setConsoEssenceL100(consoEssence);
                if (penalite != null) p.setPenaliteParKm(penalite);

                TreeMap<LocalDate, Integer> parDate = new TreeMap<>();
                LocalDate premierMois = null;
                for (int i = 11; i <= synthese.getLastRowNum(); i++) {
                    Row row = synthese.getRow(i);
                    if (row == null) continue;
                    LocalDateTime mois = dateHeure(row.getCell(0));
                    Double km = nombre(row.getCell(2));
                    if (mois == null || km == null) continue;
                    if (premierMois == null) premierMois = mois.toLocalDate();
                    parDate.put(mois.toLocalDate(), km.intValue());
                }
                LocalDateTime debutContrat = dateHeure(cellule(synthese, "F4"));
                if (debutContrat != null) {
                    p.setContratDebut(debutContrat.toLocalDate());
                } else if (premierMois != null) {
                    p.setContratDebut(premierMois);
                }
                Double kmActuel = nombre(cellule(synthese, "F3"));
                // Un fichier exporté contient déjà le dernier relevé dans le tableau : pas de doublon à la date du jour
                boolean dejaDansTableau = kmActuel != null && !parDate.isEmpty()
                        && parDate.lastEntry().getValue() == kmActuel.intValue();
                if (kmActuel != null && !dejaDansTableau) {
                    parDate.put(LocalDate.now(zone), kmActuel.intValue());
                }
                parDate.forEach((d, km) -> releves.add(new ReleveKm(d, km)));
                parametresService.enregistrer(p);
                parametresImportes = true;
            } else {
                avertissements.add("Onglet \"Synthèse\" absent : paramètres et relevés kilométriques non importés");
            }

            // ---------- Remplacement des données
            rechargeRepository.deleteAllInBatch();
            lieuRepository.deleteAllInBatch();
            tarifRepository.deleteAllInBatch();
            releveRepository.deleteAllInBatch();
            lieuRepository.saveAll(lieux.values());
            rechargeRepository.saveAll(recharges);
            tarifRepository.saveAll(tarifs.values());
            releveRepository.saveAll(releves);

            return new ImportResultDto(recharges.size(), tarifs.size(), releves.size(), parametresImportes, avertissements);
        }
    }

    /**
     * Dans le classeur, la plage d'heures creuses est codée en dur dans les formules (22:00 → 06:00 puis 06:20).
     * Chaque changement de plage crée si besoin un tarif à la date de la première recharge concernée,
     * puis chaque tarif reçoit la plage en vigueur à sa date d'effet.
     */
    private static void appliquerPlagesHc(TreeMap<LocalDate, Tarif> tarifs, List<PlageHc> plages) {
        if (tarifs.isEmpty() || plages.isEmpty()) return;
        plages.sort(Comparator.comparing(PlageHc::date));
        List<PlageHc> changements = new ArrayList<>();
        for (PlageHc p : plages) {
            PlageHc prec = changements.isEmpty() ? null : changements.get(changements.size() - 1);
            if (prec == null || !prec.debut().equals(p.debut()) || !prec.fin().equals(p.fin())) {
                changements.add(p);
            }
        }
        for (int i = 1; i < changements.size(); i++) {
            LocalDate d = changements.get(i).date();
            if (!tarifs.containsKey(d)) {
                Map.Entry<LocalDate, Tarif> base = tarifs.floorEntry(d);
                if (base == null) base = tarifs.firstEntry();
                Tarif copie = new Tarif();
                copie.setDateEffet(d);
                copie.setPrixHc(base.getValue().getPrixHc());
                copie.setPrixHp(base.getValue().getPrixHp());
                copie.setPrixEssence(base.getValue().getPrixEssence());
                tarifs.put(d, copie);
            }
        }
        for (Tarif t : tarifs.values()) {
            PlageHc applicable = changements.get(0);
            for (PlageHc c : changements) {
                if (!c.date().isAfter(t.getDateEffet())) applicable = c;
            }
            t.setHeureDebutHc(applicable.debut());
            t.setHeureFinHc(applicable.fin());
        }
    }

    // ---------------------------------------------------------------- utilitaires

    /** Dernier prix non nul de la colonne D (« Parc relai ») de l'onglet « Prix Electricité », 0 si absent. */
    private static double ancienPrixParcRelai(Sheet prix) {
        double dernier = 0;
        for (int i = 2; i <= prix.getLastRowNum(); i++) {
            Row row = prix.getRow(i);
            if (row == null) continue;
            Double v = nombre(row.getCell(3));
            if (v != null && v > 0) dernier = v;
        }
        return dernier;
    }

    private static LieuPerso nouveauLieu(String nom, double prixKwh) {
        LieuPerso l = new LieuPerso();
        l.setNom(nom.trim());
        l.setPrixKwh(prixKwh);
        return l;
    }

    private static Sheet feuille(Workbook wb, String nomNormalise) {
        for (Sheet s : wb) {
            if (normaliser(s.getSheetName()).equals(nomNormalise)) return s;
        }
        return null;
    }

    private static String normaliser(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").trim().toLowerCase(Locale.ROOT);
    }

    private static Cell cellule(Sheet sheet, String ref) {
        org.apache.poi.ss.util.CellReference cr = new org.apache.poi.ss.util.CellReference(ref);
        Row row = sheet.getRow(cr.getRow());
        return row == null ? null : row.getCell(cr.getCol());
    }

    private static CellType type(Cell c) {
        return c.getCellType() == CellType.FORMULA ? c.getCachedFormulaResultType() : c.getCellType();
    }

    private static String texte(Cell c) {
        if (c == null) return null;
        return switch (type(c)) {
            case STRING -> c.getStringCellValue();
            case NUMERIC -> String.valueOf(c.getNumericCellValue());
            default -> null;
        };
    }

    private static Double nombre(Cell c) {
        if (c == null) return null;
        return switch (type(c)) {
            case NUMERIC -> c.getNumericCellValue();
            case STRING -> {
                try {
                    yield Double.parseDouble(c.getStringCellValue().trim().replace(',', '.'));
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            default -> null;
        };
    }

    /** Date Excel arrondie à la seconde (les valeurs flottantes ne tombent pas toujours pile). */
    private static LocalDateTime dateHeure(Cell c) {
        if (c == null || type(c) != CellType.NUMERIC) return null;
        double v = c.getNumericCellValue();
        long secondes = Math.round(v * 86400);
        return LocalDateTime.of(1899, 12, 30, 0, 0).plus(secondes, ChronoUnit.SECONDS);
    }

    /** Heure saisie dans une cellule (fraction de jour). */
    private static LocalTime heure(Cell c) {
        if (c == null || type(c) != CellType.NUMERIC) return null;
        long minutes = Math.round((c.getNumericCellValue() % 1) * 1440) % 1440;
        return LocalTime.ofSecondOfDay(minutes * 60);
    }

    private static LocalTime heureFormule(Cell c, Pattern pattern) {
        if (c == null || c.getCellType() != CellType.FORMULA) return null;
        try {
            Matcher m = pattern.matcher(c.getCellFormula());
            while (m.find()) {
                String h = m.group(1);
                if (!h.startsWith("24")) {
                    return LocalTime.parse(h.length() == 4 ? "0" + h : h);
                }
            }
        } catch (RuntimeException ignored) {
            // formule illisible : on garde la plage par défaut
        }
        return null;
    }
}

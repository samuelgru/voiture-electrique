package fr.perso.zoe.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

/** Prix de référence applicables à partir de {@link #dateEffet}. */
@Entity
@Table(name = "tarif")
public class Tarif {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_effet", nullable = false, unique = true)
    private LocalDate dateEffet;

    @Column(name = "prix_hc", nullable = false)
    private double prixHc;

    @Column(name = "prix_hp", nullable = false)
    private double prixHp;

    @Column(name = "prix_essence", nullable = false)
    private double prixEssence;

    @Column(name = "heure_debut_hc", nullable = false)
    private LocalTime heureDebutHc = LocalTime.of(22, 0);

    @Column(name = "heure_fin_hc", nullable = false)
    private LocalTime heureFinHc = LocalTime.of(6, 0);

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getDateEffet() { return dateEffet; }
    public void setDateEffet(LocalDate dateEffet) { this.dateEffet = dateEffet; }
    public double getPrixHc() { return prixHc; }
    public void setPrixHc(double prixHc) { this.prixHc = prixHc; }
    public double getPrixHp() { return prixHp; }
    public void setPrixHp(double prixHp) { this.prixHp = prixHp; }
    public double getPrixEssence() { return prixEssence; }
    public void setPrixEssence(double prixEssence) { this.prixEssence = prixEssence; }
    public LocalTime getHeureDebutHc() { return heureDebutHc; }
    public void setHeureDebutHc(LocalTime heureDebutHc) { this.heureDebutHc = heureDebutHc; }
    public LocalTime getHeureFinHc() { return heureFinHc; }
    public void setHeureFinHc(LocalTime heureFinHc) { this.heureFinHc = heureFinHc; }
}

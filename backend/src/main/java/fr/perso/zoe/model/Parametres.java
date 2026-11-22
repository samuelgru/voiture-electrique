package fr.perso.zoe.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "parametres")
public class Parametres {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "contrat_debut", nullable = false)
    private LocalDate contratDebut;

    @Column(name = "contrat_duree_ans", nullable = false)
    private int contratDureeAns;

    @Column(name = "contrat_km", nullable = false)
    private int contratKm;

    @Column(name = "km_debut", nullable = false)
    private int kmDebut;

    @Column(name = "penalite_par_km", nullable = false)
    private double penaliteParKm;

    @Column(name = "conso_essence_l100", nullable = false)
    private double consoEssenceL100;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getContratDebut() { return contratDebut; }
    public void setContratDebut(LocalDate contratDebut) { this.contratDebut = contratDebut; }
    public int getContratDureeAns() { return contratDureeAns; }
    public void setContratDureeAns(int contratDureeAns) { this.contratDureeAns = contratDureeAns; }
    public int getContratKm() { return contratKm; }
    public void setContratKm(int contratKm) { this.contratKm = contratKm; }
    public int getKmDebut() { return kmDebut; }
    public void setKmDebut(int kmDebut) { this.kmDebut = kmDebut; }
    public double getPenaliteParKm() { return penaliteParKm; }
    public void setPenaliteParKm(double penaliteParKm) { this.penaliteParKm = penaliteParKm; }
    public double getConsoEssenceL100() { return consoEssenceL100; }
    public void setConsoEssenceL100(double consoEssenceL100) { this.consoEssenceL100 = consoEssenceL100; }
}

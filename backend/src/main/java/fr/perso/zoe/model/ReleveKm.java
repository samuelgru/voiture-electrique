package fr.perso.zoe.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Relevé du compteur kilométrique à une date donnée. */
@Entity
@Table(name = "releve_km")
public class ReleveKm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_releve", nullable = false, unique = true)
    private LocalDate dateReleve;

    @Column(nullable = false)
    private int kilometrage;

    public ReleveKm() {}

    public ReleveKm(LocalDate dateReleve, int kilometrage) {
        this.dateReleve = dateReleve;
        this.kilometrage = kilometrage;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getDateReleve() { return dateReleve; }
    public void setDateReleve(LocalDate dateReleve) { this.dateReleve = dateReleve; }
    public int getKilometrage() { return kilometrage; }
    public void setKilometrage(int kilometrage) { this.kilometrage = kilometrage; }
}

package fr.perso.zoe.model;

import jakarta.persistence.*;

/** Lieu de recharge défini par l'utilisateur, au prix du kWh unique (pas d'heures creuses). */
@Entity
@Table(name = "lieu_perso")
public class LieuPerso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nom;

    @Column(name = "prix_kwh", nullable = false)
    private double prixKwh;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public double getPrixKwh() { return prixKwh; }
    public void setPrixKwh(double prixKwh) { this.prixKwh = prixKwh; }
}

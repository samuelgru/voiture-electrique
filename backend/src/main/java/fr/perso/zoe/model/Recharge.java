package fr.perso.zoe.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Une session de recharge (une ligne de l'onglet "Zoe"). */
@Entity
@Table(name = "recharge")
public class Recharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Lieu lieu = Lieu.MAISON;

    /** Renseigné uniquement quand {@link #lieu} vaut {@link Lieu#AUTRE}. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lieu_perso_id")
    private LieuPerso lieuPerso;

    @Column(nullable = false)
    private LocalDateTime debut;

    @Column(nullable = false)
    private LocalDateTime fin;

    /** Niveau de batterie au début, entre 0 et 1. */
    @Column(name = "pct_debut")
    private Double pctDebut;

    /** Niveau de batterie à la fin, entre 0 et 1. */
    @Column(name = "pct_fin")
    private Double pctFin;

    @Column(nullable = false)
    private double kwh;

    @Column(length = 500)
    private String commentaire;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Lieu getLieu() { return lieu; }
    public void setLieu(Lieu lieu) { this.lieu = lieu; }
    public LieuPerso getLieuPerso() { return lieuPerso; }
    public void setLieuPerso(LieuPerso lieuPerso) { this.lieuPerso = lieuPerso; }
    public LocalDateTime getDebut() { return debut; }
    public void setDebut(LocalDateTime debut) { this.debut = debut; }
    public LocalDateTime getFin() { return fin; }
    public void setFin(LocalDateTime fin) { this.fin = fin; }
    public Double getPctDebut() { return pctDebut; }
    public void setPctDebut(Double pctDebut) { this.pctDebut = pctDebut; }
    public Double getPctFin() { return pctFin; }
    public void setPctFin(Double pctFin) { this.pctFin = pctFin; }
    public double getKwh() { return kwh; }
    public void setKwh(double kwh) { this.kwh = kwh; }
    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
}

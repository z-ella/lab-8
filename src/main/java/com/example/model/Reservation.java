package com.example.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "reservations")
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String salle;

    @Column(nullable = false)
    private String reserveur;

    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDateTime dateFin;

    @Version
    private Long version;

    protected Reservation() {
    }

    public Reservation(String salle, String reserveur, LocalDateTime dateDebut, LocalDateTime dateFin) {
        this.salle = Objects.requireNonNull(salle, "salle");
        this.reserveur = Objects.requireNonNull(reserveur, "reserveur");
        this.dateDebut = Objects.requireNonNull(dateDebut, "dateDebut");
        this.dateFin = Objects.requireNonNull(dateFin, "dateFin");
        if (!dateFin.isAfter(dateDebut)) {
            throw new IllegalArgumentException("La date de fin doit suivre la date de debut.");
        }
    }

    public Long getId() {
        return id;
    }

    public String getSalle() {
        return salle;
    }

    public void setSalle(String salle) {
        this.salle = Objects.requireNonNull(salle, "salle");
    }

    public String getReserveur() {
        return reserveur;
    }

    public void setReserveur(String reserveur) {
        this.reserveur = Objects.requireNonNull(reserveur, "reserveur");
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = Objects.requireNonNull(dateDebut, "dateDebut");
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = Objects.requireNonNull(dateFin, "dateFin");
        if (dateDebut != null && !dateFin.isAfter(dateDebut)) {
            throw new IllegalArgumentException("La date de fin doit suivre la date de debut.");
        }
    }

    public Long getVersion() {
        return version;
    }
}

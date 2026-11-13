package edu.eci.dosw.oficioya.reputation.domain;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * Reputación de un usuario: promedio de sus reseñas visibles. Se especializa en trabajador y contratante
 * (herencia mapeada como tabla por subclase).
 */
@Entity
@Table(name = "reputacion")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "tipo", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class Reputacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "promedio_calificaciones", nullable = false)
    private double promedioCalificaciones;

    @Column(name = "total_resenas", nullable = false)
    private int totalResenas;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    protected Reputacion() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    /**
     * Recalcula el promedio con las puntuaciones de las reseñas visibles (RF-02).
     */
    public void recalcular(List<Integer> puntuacionesVisibles, LocalDateTime fecha) {
        this.totalResenas = puntuacionesVisibles.size();
        this.promedioCalificaciones = ReglasReputacion.promedio(puntuacionesVisibles);
        this.fechaActualizacion = fecha;
    }

    /** Valor de reputación que ven los demás dominios. */
    public abstract double valorReputacion();

    public abstract TipoParte getTipo();

    protected void tocar(LocalDateTime fecha) {
        this.fechaActualizacion = fecha;
    }

    public Integer getId() {
        return id;
    }

    public double getPromedioCalificaciones() {
        return promedioCalificaciones;
    }

    public int getTotalResenas() {
        return totalResenas;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}

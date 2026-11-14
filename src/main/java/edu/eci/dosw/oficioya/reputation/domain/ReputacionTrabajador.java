package edu.eci.dosw.oficioya.reputation.domain;

import java.util.UUID;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * Reputación de un trabajador: además del promedio, acumula penalizaciones y puede quedar pausado.
 */
@Entity
@Table(name = "reputacion_trabajador")
@DiscriminatorValue("TRABAJADOR")
@PrimaryKeyJoinColumn(name = "id")
public class ReputacionTrabajador extends Reputacion {

    @Column(name = "trabajador_id", nullable = false, unique = true)
    private UUID trabajadorId;

    @Column(name = "penalizacion_acumulada", nullable = false)
    private double penalizacionAcumulada;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoTrabajador estado;

    @Column(name = "fecha_pausado")
    private LocalDateTime fechaPausado;

    protected ReputacionTrabajador() {
        // requerido por JPA
    }

    public ReputacionTrabajador(UUID trabajadorId) {
        this.trabajadorId = trabajadorId;
        this.estado = EstadoTrabajador.ACTIVO;
    }

    /**
     * La reputación final descuenta las penalizaciones del promedio, sin bajar de cero.
     */
    @Override
    public double valorReputacion() {
        return Math.max(0, getPromedioCalificaciones() - penalizacionAcumulada);
    }

    @Override
    public TipoParte getTipo() {
        return TipoParte.TRABAJADOR;
    }

    /** Suma una penalización (RF-06). Es permanente: no se descuenta al recalcular el promedio. */
    public void penalizar(double puntos, LocalDateTime fecha) {
        this.penalizacionAcumulada += puntos;
        tocar(fecha);
    }

    /** Pausa al trabajador por baja reputación (RF-05). Devuelve false si ya estaba pausado. */
    public boolean pausar(LocalDateTime fecha) {
        if (estaPausado()) {
            return false;
        }
        this.estado = EstadoTrabajador.PAUSADO;
        this.fechaPausado = fecha;
        tocar(fecha);
        return true;
    }

    public boolean estaPausado() {
        return estado == EstadoTrabajador.PAUSADO;
    }

    public UUID getTrabajadorId() {
        return trabajadorId;
    }

    public double getPenalizacionAcumulada() {
        return penalizacionAcumulada;
    }

    public EstadoTrabajador getEstado() {
        return estado;
    }

    public LocalDateTime getFechaPausado() {
        return fechaPausado;
    }
}

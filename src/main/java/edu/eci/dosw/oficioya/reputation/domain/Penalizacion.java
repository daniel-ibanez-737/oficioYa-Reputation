package edu.eci.dosw.oficioya.reputation.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Penalización aplicada a un trabajador por cancelar tarde una solicitud (RF-06).
 * Se guarda aparte del promedio para que sea trazable y permanente.
 */
@Entity
@Table(name = "penalizacion")
public class Penalizacion {

    public static final String MOTIVO_CANCELACION_TARDIA = "Cancelación después del límite de horas permitido";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reputacion_trabajador_id", nullable = false)
    private ReputacionTrabajador reputacionTrabajador;

    @Column(name = "solicitud_id", nullable = false, unique = true)
    private Integer solicitudId;

    @Column(name = "puntos", nullable = false)
    private double puntos;

    @Column(name = "motivo", nullable = false)
    private String motivo;

    @Column(name = "fecha_hora_servicio", nullable = false)
    private LocalDateTime fechaHoraServicio;

    @Column(name = "fecha_cancelacion", nullable = false)
    private LocalDateTime fechaCancelacion;

    @Column(name = "fecha_aplicacion", nullable = false)
    private LocalDateTime fechaAplicacion;

    protected Penalizacion() {
        // requerido por JPA
    }

    @SuppressWarnings("java:S107")
    public Penalizacion(ReputacionTrabajador reputacionTrabajador, Integer solicitudId, double puntos,
                        LocalDateTime fechaHoraServicio, LocalDateTime fechaCancelacion,
                        LocalDateTime fechaAplicacion) {
        this.reputacionTrabajador = reputacionTrabajador;
        this.solicitudId = solicitudId;
        this.puntos = puntos;
        this.motivo = MOTIVO_CANCELACION_TARDIA;
        this.fechaHoraServicio = fechaHoraServicio;
        this.fechaCancelacion = fechaCancelacion;
        this.fechaAplicacion = fechaAplicacion;
    }

    public Integer getId() {
        return id;
    }

    public Integer getSolicitudId() {
        return solicitudId;
    }

    public double getPuntos() {
        return puntos;
    }

    public String getMotivo() {
        return motivo;
    }

    public LocalDateTime getFechaAplicacion() {
        return fechaAplicacion;
    }
}

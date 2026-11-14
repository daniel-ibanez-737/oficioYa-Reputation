package edu.eci.dosw.oficioya.reputation.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import edu.eci.dosw.oficioya.reputation.exception.SolicitudInvalidaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Calificación (1 a 5) con comentario opcional que una parte deja a la otra al terminar un servicio.
 * Es inmutable: no tiene setters; solo un administrador puede retirarla de la vista tras un reporte.
 */
@Entity
@Table(name = "resena")
public class Resena {

    public static final int PUNTUACION_MINIMA = 1;
    public static final int PUNTUACION_MAXIMA = 5;
    public static final int LONGITUD_MAXIMA_COMENTARIO = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Identificador único y trazable que se expone en la API (RNF-04). */
    @Column(name = "codigo", nullable = false, unique = true)
    private UUID codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private ServicioCalificable servicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reputacion_id", nullable = false)
    private Reputacion reputacion;

    @Column(name = "autor_id", nullable = false)
    private UUID autorId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo_autor", nullable = false, length = 20)
    private TipoParte tipoAutor;

    @Column(name = "calificado_id", nullable = false)
    private UUID calificadoId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo_calificado", nullable = false, length = 20)
    private TipoParte tipoCalificado;

    @Column(name = "puntuacion", nullable = false)
    private int puntuacion;

    @Column(name = "comentario", length = LONGITUD_MAXIMA_COMENTARIO)
    private String comentario;

    @Column(name = "visible", nullable = false)
    private boolean visible;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    protected Resena() {
        // requerido por JPA
    }

    public Resena(ServicioCalificable servicio, Reputacion reputacion, UUID autorId, TipoParte tipoAutor,
                  int puntuacion, String comentario, LocalDateTime fechaCreacion) {
        if (puntuacion < PUNTUACION_MINIMA || puntuacion > PUNTUACION_MAXIMA) {
            throw new SolicitudInvalidaException("La puntuación debe estar entre 1 y 5");
        }
        if (reputacion.getTipo() != tipoAutor.contraparte()) {
            throw new IllegalArgumentException("La reseña debe asociarse a la reputación de la contraparte");
        }
        this.codigo = UUID.randomUUID();
        this.servicio = servicio;
        this.reputacion = reputacion;
        this.autorId = autorId;
        this.tipoAutor = tipoAutor;
        this.calificadoId = servicio.idDe(tipoAutor.contraparte());
        this.tipoCalificado = tipoAutor.contraparte();
        this.puntuacion = puntuacion;
        this.comentario = comentario == null || comentario.isBlank() ? null : comentario.strip();
        this.visible = true;
        this.fechaCreacion = fechaCreacion;
    }

    /** La retira de la vista pública por decisión de moderación; el registro se conserva. */
    public void retirar() {
        this.visible = false;
    }

    public Integer getId() {
        return id;
    }

    public UUID getCodigo() {
        return codigo;
    }

    public ServicioCalificable getServicio() {
        return servicio;
    }

    public Reputacion getReputacion() {
        return reputacion;
    }

    public UUID getAutorId() {
        return autorId;
    }

    public TipoParte getTipoAutor() {
        return tipoAutor;
    }

    public UUID getCalificadoId() {
        return calificadoId;
    }

    public TipoParte getTipoCalificado() {
        return tipoCalificado;
    }

    public int getPuntuacion() {
        return puntuacion;
    }

    public String getComentario() {
        return comentario;
    }

    public boolean isVisible() {
        return visible;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}

package edu.eci.dosw.oficioya.reputation.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import edu.eci.dosw.oficioya.reputation.exception.ConflictoException;
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
 * Reporte de una reseña que un usuario considera falsa o inapropiada (RF-04).
 * Queda pendiente hasta que un administrador lo resuelve; la resolución queda registrada (RNF-07).
 */
@Entity
@Table(name = "reporte_resena")
public class ReporteResena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resena_id", nullable = false)
    private Resena resena;

    @Column(name = "reportante_id", nullable = false)
    private Integer reportanteId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "motivo", nullable = false, length = 20)
    private MotivoReporte motivo;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoReporte estado;

    @Column(name = "fecha_reporte", nullable = false)
    private LocalDateTime fechaReporte;

    @Column(name = "moderador_id")
    private Integer moderadorId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "resultado", length = 20)
    private ResultadoModeracion resultado;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    protected ReporteResena() {
        // requerido por JPA
    }

    public ReporteResena(Resena resena, Integer reportanteId, MotivoReporte motivo, String descripcion,
                         LocalDateTime fechaReporte) {
        this.resena = resena;
        this.reportanteId = reportanteId;
        this.motivo = motivo;
        this.descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.strip();
        this.estado = EstadoReporte.PENDIENTE;
        this.fechaReporte = fechaReporte;
    }

    /** Registra la decisión del administrador. Un reporte resuelto no se puede volver a resolver. */
    public void resolver(Integer moderadorId, ResultadoModeracion resultado, LocalDateTime fecha) {
        if (estaResuelto()) {
            throw new ConflictoException("El reporte ya fue resuelto");
        }
        this.moderadorId = moderadorId;
        this.resultado = resultado;
        this.fechaResolucion = fecha;
        this.estado = EstadoReporte.RESUELTO;
    }

    public boolean estaResuelto() {
        return estado == EstadoReporte.RESUELTO;
    }

    public Integer getId() {
        return id;
    }

    public Resena getResena() {
        return resena;
    }

    public Integer getReportanteId() {
        return reportanteId;
    }

    public MotivoReporte getMotivo() {
        return motivo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public EstadoReporte getEstado() {
        return estado;
    }

    public LocalDateTime getFechaReporte() {
        return fechaReporte;
    }

    public Integer getModeradorId() {
        return moderadorId;
    }

    public ResultadoModeracion getResultado() {
        return resultado;
    }

    public LocalDateTime getFechaResolucion() {
        return fechaResolucion;
    }
}

package edu.eci.dosw.oficioya.reputation.domain;

import java.util.UUID;

import java.time.LocalDateTime;
import java.util.Set;

import edu.eci.dosw.oficioya.reputation.exception.ConflictoException;
import edu.eci.dosw.oficioya.reputation.exception.OperacionNoPermitidaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Servicio (solicitud) en estado Cumplida que Service notificó a Reputation.
 * Solo sobre un servicio calificable se pueden registrar calificaciones, una por cada parte.
 */
@Entity
@Table(name = "servicio_calificable")
public class ServicioCalificable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "solicitud_id", nullable = false, unique = true)
    private UUID solicitudId;

    @Column(name = "trabajador_id", nullable = false)
    private UUID trabajadorId;

    @Column(name = "contratante_id", nullable = false)
    private UUID contratanteId;

    @Column(name = "fecha_cumplida", nullable = false)
    private LocalDateTime fechaCumplida;

    @Column(name = "calificado_por_trabajador", nullable = false)
    private boolean calificadoPorTrabajador;

    @Column(name = "calificado_por_contratante", nullable = false)
    private boolean calificadoPorContratante;

    protected ServicioCalificable() {
        // requerido por JPA
    }

    public ServicioCalificable(UUID solicitudId, UUID trabajadorId, UUID contratanteId,
                               LocalDateTime fechaCumplida) {
        this.solicitudId = solicitudId;
        this.trabajadorId = trabajadorId;
        this.contratanteId = contratanteId;
        this.fechaCumplida = fechaCumplida;
    }

    /**
     * Determina en calidad de qué parte participa el usuario en este servicio, según su id y sus roles.
     */
    public TipoParte parteDe(UUID usuarioId, Set<TipoParte> roles) {
        if (roles.contains(TipoParte.TRABAJADOR) && trabajadorId.equals(usuarioId)) {
            return TipoParte.TRABAJADOR;
        }
        if (roles.contains(TipoParte.CONTRATANTE) && contratanteId.equals(usuarioId)) {
            return TipoParte.CONTRATANTE;
        }
        throw new OperacionNoPermitidaException("Solo el trabajador o el contratante del servicio pueden calificarlo");
    }

    public boolean yaCalifico(TipoParte parte) {
        return parte == TipoParte.TRABAJADOR ? calificadoPorTrabajador : calificadoPorContratante;
    }

    /**
     * Marca que la parte ya calificó; una segunda calificación de la misma parte se rechaza.
     */
    public void registrarCalificacion(TipoParte parte) {
        if (yaCalifico(parte)) {
            throw new ConflictoException("Esta parte ya calificó el servicio");
        }
        if (parte == TipoParte.TRABAJADOR) {
            calificadoPorTrabajador = true;
        } else {
            calificadoPorContratante = true;
        }
    }

    /** Id del usuario que recibe la calificación de la parte indicada. */
    public UUID idDe(TipoParte parte) {
        return parte == TipoParte.TRABAJADOR ? trabajadorId : contratanteId;
    }

    public Integer getId() {
        return id;
    }

    public UUID getSolicitudId() {
        return solicitudId;
    }

    public UUID getTrabajadorId() {
        return trabajadorId;
    }

    public UUID getContratanteId() {
        return contratanteId;
    }

    public LocalDateTime getFechaCumplida() {
        return fechaCumplida;
    }
}

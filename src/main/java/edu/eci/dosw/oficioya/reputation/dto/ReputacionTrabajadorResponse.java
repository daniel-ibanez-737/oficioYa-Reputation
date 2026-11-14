package edu.eci.dosw.oficioya.reputation.dto;

import java.util.UUID;

import edu.eci.dosw.oficioya.reputation.domain.EstadoTrabajador;
import edu.eci.dosw.oficioya.reputation.domain.ReputacionTrabajador;

public record ReputacionTrabajadorResponse(
        UUID trabajadorId,
        double promedioCalificaciones,
        double penalizacionAcumulada,
        double reputacion,
        int totalResenas,
        EstadoTrabajador estado) {

    public static ReputacionTrabajadorResponse desde(ReputacionTrabajador r) {
        return new ReputacionTrabajadorResponse(r.getTrabajadorId(), r.getPromedioCalificaciones(),
                r.getPenalizacionAcumulada(), r.valorReputacion(), r.getTotalResenas(), r.getEstado());
    }

    /** Trabajador que aún no tiene reseñas ni penalizaciones. */
    public static ReputacionTrabajadorResponse sinResenas(UUID trabajadorId) {
        return new ReputacionTrabajadorResponse(trabajadorId, 0, 0, 0, 0, EstadoTrabajador.ACTIVO);
    }
}

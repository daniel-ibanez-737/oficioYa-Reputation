package edu.eci.dosw.oficioya.reputation.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import edu.eci.dosw.oficioya.reputation.domain.EstadoReporte;
import edu.eci.dosw.oficioya.reputation.domain.MotivoReporte;
import edu.eci.dosw.oficioya.reputation.domain.ReporteResena;
import edu.eci.dosw.oficioya.reputation.domain.ResultadoModeracion;

public record ReporteResponse(
        Integer id,
        UUID codigoResena,
        UUID reportanteId,
        MotivoReporte motivo,
        String descripcion,
        EstadoReporte estado,
        LocalDateTime fechaReporte,
        UUID moderadorId,
        ResultadoModeracion resultado,
        LocalDateTime fechaResolucion) {

    public static ReporteResponse desde(ReporteResena r) {
        return new ReporteResponse(r.getId(), r.getResena().getCodigo(), r.getReportanteId(), r.getMotivo(),
                r.getDescripcion(), r.getEstado(), r.getFechaReporte(), r.getModeradorId(), r.getResultado(),
                r.getFechaResolucion());
    }
}

package edu.eci.dosw.oficioya.reputation.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import edu.eci.dosw.oficioya.reputation.domain.MotivoReporte;
import edu.eci.dosw.oficioya.reputation.domain.ReporteResena;
import edu.eci.dosw.oficioya.reputation.domain.Resena;

/**
 * Correo que Reputation pide enviar al administrador (vía Orchestrator → Notificaciones) cuando se reporta una
 * reseña. Lleva ids y el contenido de la reseña, nunca nombres ni datos de contacto de los usuarios.
 */
public record AvisoReporteResenaDTO(
        String destinatario,
        String asunto,
        Integer reporteId,
        UUID codigoResena,
        Integer calificadoId,
        int puntuacion,
        String comentario,
        Integer reportanteId,
        MotivoReporte motivo,
        String descripcion,
        LocalDateTime fechaReporte) {

    public static AvisoReporteResenaDTO de(ReporteResena reporte, String destinatario) {
        Resena resena = reporte.getResena();
        return new AvisoReporteResenaDTO(destinatario,
                "OficioYa · Reseña reportada (" + reporte.getMotivo() + ")",
                reporte.getId(), resena.getCodigo(), resena.getCalificadoId(), resena.getPuntuacion(),
                resena.getComentario(), reporte.getReportanteId(), reporte.getMotivo(), reporte.getDescripcion(),
                reporte.getFechaReporte());
    }
}

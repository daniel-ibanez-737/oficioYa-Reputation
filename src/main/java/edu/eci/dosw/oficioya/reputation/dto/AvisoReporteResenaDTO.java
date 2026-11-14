package edu.eci.dosw.oficioya.reputation.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import edu.eci.dosw.oficioya.reputation.domain.ReporteResena;
import edu.eci.dosw.oficioya.reputation.domain.Resena;

/**
 * Aviso al administrador cuando se reporta una reseña, en el formato real de Notification
 * ({@code POST /api/v1/notification}, vía Orchestrator). Lleva ids y el contenido de la reseña, nunca nombres ni
 * datos de contacto de los usuarios.
 *
 * @param receiverId  id (UUID) del administrador que recibe el aviso
 * @param title       título del aviso
 * @param description detalle del reporte
 * @param type        tipo de notificación; Notification aún no tiene uno para reseñas reportadas, así que es
 *                    configurable y por defecto va vacío ({@code null})
 * @param requestId   solicitud de servicio a la que pertenece la reseña
 * @param dateCreated fecha del reporte
 */
public record AvisoReporteResenaDTO(
        UUID receiverId,
        String title,
        String description,
        String type,
        UUID requestId,
        LocalDateTime dateCreated) {

    public static AvisoReporteResenaDTO de(ReporteResena reporte, UUID administradorId, String tipo) {
        Resena resena = reporte.getResena();
        String detalle = "Reporte " + reporte.getId() + " de la reseña " + resena.getCodigo()
                + " (puntuación " + resena.getPuntuacion() + ", calificado " + resena.getCalificadoId() + "). "
                + "Motivo: " + reporte.getMotivo() + ". Reportado por " + reporte.getReportanteId() + ": "
                + reporte.getDescripcion()
                + (resena.getComentario() == null ? "" : " | Comentario de la reseña: " + resena.getComentario());
        return new AvisoReporteResenaDTO(administradorId,
                "OficioYa · Reseña reportada (" + reporte.getMotivo() + ")", detalle,
                tipo == null || tipo.isBlank() ? null : tipo.trim(),
                resena.getServicio().getSolicitudId(), reporte.getFechaReporte());
    }
}

package edu.eci.dosw.oficioya.reputation.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import edu.eci.dosw.oficioya.reputation.domain.Resena;
import edu.eci.dosw.oficioya.reputation.domain.TipoParte;

/**
 * Reseña de solo lectura. No incluye el id del autor, solo si fue trabajador o contratante.
 */
public record ResenaResponse(
        UUID codigo,
        Integer solicitudId,
        TipoParte tipoAutor,
        TipoParte tipoCalificado,
        Integer calificadoId,
        int puntuacion,
        String comentario,
        LocalDateTime fechaCreacion) {

    public static ResenaResponse desde(Resena resena) {
        return new ResenaResponse(resena.getCodigo(), resena.getServicio().getSolicitudId(), resena.getTipoAutor(),
                resena.getTipoCalificado(), resena.getCalificadoId(), resena.getPuntuacion(), resena.getComentario(),
                resena.getFechaCreacion());
    }
}

package edu.eci.dosw.oficioya.reputation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Calificación que una parte deja a la otra sobre un servicio cumplido.
 */
public record CalificacionRequest(
        @NotNull Integer solicitudId,
        @NotNull @Min(1) @Max(5) Integer puntuacion,
        @Size(max = 500) String comentario) {
}

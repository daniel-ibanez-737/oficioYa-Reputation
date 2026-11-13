package edu.eci.dosw.oficioya.reputation.dto;

import edu.eci.dosw.oficioya.reputation.domain.MotivoReporte;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Reporte de una reseña. La descripción es obligatoria (mínimo 50 caracteres sin contar los espacios del inicio y
 * del final) porque es lo que lee el administrador en el correo.
 */
public record ReporteRequest(
        @NotNull MotivoReporte motivo,
        @NotBlank(message = "La descripción del reporte es obligatoria")
        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        @Pattern(regexp = "(?s)\\s*\\S.{48,}\\S\\s*", message = "La descripción debe tener al menos 50 caracteres")
        String descripcion) {
}

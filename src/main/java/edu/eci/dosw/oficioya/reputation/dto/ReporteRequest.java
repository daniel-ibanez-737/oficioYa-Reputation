package edu.eci.dosw.oficioya.reputation.dto;

import edu.eci.dosw.oficioya.reputation.domain.MotivoReporte;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReporteRequest(@NotNull MotivoReporte motivo, @Size(max = 500) String descripcion) {
}

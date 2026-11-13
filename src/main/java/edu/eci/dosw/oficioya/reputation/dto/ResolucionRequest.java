package edu.eci.dosw.oficioya.reputation.dto;

import edu.eci.dosw.oficioya.reputation.domain.ResultadoModeracion;
import jakarta.validation.constraints.NotNull;

public record ResolucionRequest(@NotNull ResultadoModeracion resultado) {
}

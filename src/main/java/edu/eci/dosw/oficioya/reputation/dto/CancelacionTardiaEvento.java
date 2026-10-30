package edu.eci.dosw.oficioya.reputation.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

/**
 * Aviso de Service (vía Orchestrator) de que un trabajador canceló una solicitud.
 * SUPUESTO: contrato pendiente de acordar con el Squad 3.
 */
public record CancelacionTardiaEvento(
        @NotNull Integer solicitudId,
        @NotNull Integer trabajadorId,
        @NotNull LocalDateTime fechaHoraServicio,
        @NotNull LocalDateTime fechaCancelacion) {
}

package edu.eci.dosw.oficioya.reputation.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

/**
 * Aviso de Service (vía Orchestrator) de que una solicitud quedó en estado Cumplida.
 * SUPUESTO: contrato pendiente de acordar con el Squad 3.
 */
public record ServicioCumplidoEvento(
        @NotNull Integer solicitudId,
        @NotNull Integer trabajadorId,
        @NotNull Integer contratanteId,
        @NotNull LocalDateTime fechaCumplida) {
}

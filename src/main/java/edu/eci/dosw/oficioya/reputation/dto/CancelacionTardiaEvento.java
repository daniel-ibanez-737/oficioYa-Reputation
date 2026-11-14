package edu.eci.dosw.oficioya.reputation.dto;

import java.util.UUID;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

/**
 * Aviso de Service (vía Orchestrator) de que un trabajador canceló una solicitud.
 * SUPUESTO: contrato pendiente de acordar con el Squad 3.
 */
public record CancelacionTardiaEvento(
        @NotNull UUID solicitudId,
        @NotNull UUID trabajadorId,
        @NotNull LocalDateTime fechaHoraServicio,
        @NotNull LocalDateTime fechaCancelacion) {
}

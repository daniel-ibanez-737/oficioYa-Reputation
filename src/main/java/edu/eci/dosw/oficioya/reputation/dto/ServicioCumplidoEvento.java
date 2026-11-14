package edu.eci.dosw.oficioya.reputation.dto;

import java.util.UUID;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

/**
 * Aviso de Service (vía Orchestrator) de que una solicitud quedó en estado Cumplida.
 * SUPUESTO: contrato pendiente de acordar con el Squad 3.
 */
public record ServicioCumplidoEvento(
        @NotNull UUID solicitudId,
        @NotNull UUID trabajadorId,
        @NotNull UUID contratanteId,
        @NotNull LocalDateTime fechaCumplida) {
}

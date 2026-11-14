package edu.eci.dosw.oficioya.reputation.dto;

import java.util.UUID;

import edu.eci.dosw.oficioya.reputation.domain.EstadoTrabajador;

/**
 * Aviso que Reputation envía a Worker (vía Orchestrator) cuando un trabajador se pausa.
 */
public record CambioEstadoTrabajadorDTO(UUID trabajadorId, EstadoTrabajador estado, String motivo) {
}

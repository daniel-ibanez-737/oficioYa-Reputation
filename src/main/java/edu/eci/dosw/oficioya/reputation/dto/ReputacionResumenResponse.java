package edu.eci.dosw.oficioya.reputation.dto;

import java.util.UUID;

/**
 * Resumen que consume Search (vía Orchestrator) para ordenar los resultados por reputación.
 */
public record ReputacionResumenResponse(UUID trabajadorId, double reputacion, int totalResenas) {
}

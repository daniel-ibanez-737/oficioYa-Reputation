package edu.eci.dosw.oficioya.reputation.dto;

/**
 * Resumen que consume Search (vía Orchestrator) para ordenar los resultados por reputación.
 */
public record ReputacionResumenResponse(Integer trabajadorId, double reputacion, int totalResenas) {
}

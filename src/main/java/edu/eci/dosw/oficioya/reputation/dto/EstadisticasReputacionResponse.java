package edu.eci.dosw.oficioya.reputation.dto;

/**
 * Datos agregados de un periodo para Reporting (vía Orchestrator). Sin datos personales.
 */
public record EstadisticasReputacionResponse(
        long totalResenas,
        double calificacionPromedio,
        long resenasReportadas,
        long reportesPendientes,
        long reportesResueltos) {
}

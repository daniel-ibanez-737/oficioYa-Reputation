package edu.eci.dosw.oficioya.reputation.dto;

/**
 * Resultado de procesar un aviso de cancelación: si se aplicó la penalización y cómo quedó la reputación.
 */
public record ResultadoPenalizacionResponse(boolean aplicada, String detalle, ReputacionTrabajadorResponse reputacion) {
}

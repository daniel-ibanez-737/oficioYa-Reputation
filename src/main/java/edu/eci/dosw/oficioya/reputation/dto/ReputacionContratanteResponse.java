package edu.eci.dosw.oficioya.reputation.dto;

import java.util.UUID;

public record ReputacionContratanteResponse(UUID contratanteId, double promedioCalificaciones, int totalResenas) {
}

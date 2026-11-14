package edu.eci.dosw.oficioya.reputation.dto;

import java.util.UUID;

import java.time.LocalDateTime;

import edu.eci.dosw.oficioya.reputation.domain.ServicioCalificable;

public record ServicioCalificableResponse(UUID solicitudId, UUID trabajadorId, UUID contratanteId,
                                          LocalDateTime fechaCumplida) {

    public static ServicioCalificableResponse desde(ServicioCalificable s) {
        return new ServicioCalificableResponse(s.getSolicitudId(), s.getTrabajadorId(), s.getContratanteId(),
                s.getFechaCumplida());
    }
}

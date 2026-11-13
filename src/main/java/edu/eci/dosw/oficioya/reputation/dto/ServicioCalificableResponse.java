package edu.eci.dosw.oficioya.reputation.dto;

import java.time.LocalDateTime;

import edu.eci.dosw.oficioya.reputation.domain.ServicioCalificable;

public record ServicioCalificableResponse(Integer solicitudId, Integer trabajadorId, Integer contratanteId,
                                          LocalDateTime fechaCumplida) {

    public static ServicioCalificableResponse desde(ServicioCalificable s) {
        return new ServicioCalificableResponse(s.getSolicitudId(), s.getTrabajadorId(), s.getContratanteId(),
                s.getFechaCumplida());
    }
}

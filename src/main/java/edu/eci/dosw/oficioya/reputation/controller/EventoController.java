package edu.eci.dosw.oficioya.reputation.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import edu.eci.dosw.oficioya.reputation.dto.CancelacionTardiaEvento;
import edu.eci.dosw.oficioya.reputation.dto.ResultadoPenalizacionResponse;
import edu.eci.dosw.oficioya.reputation.dto.ServicioCalificableResponse;
import edu.eci.dosw.oficioya.reputation.dto.ServicioCumplidoEvento;
import edu.eci.dosw.oficioya.reputation.service.ReputacionService;
import edu.eci.dosw.oficioya.reputation.service.ResenaService;
import jakarta.validation.Valid;

/**
 * Avisos que llegan de Service a través del Orchestrator (OFICIO-170).
 * SUPUESTO: contrato y rol del llamador pendientes de acordar con los Squads 1 y 3.
 * Ambos endpoints son idempotentes, porque un aviso puede llegar repetido.
 */
@Tag(name = "Eventos", description = "Avisos de otros dominios a través del Orchestrator (rol SISTEMA)")
@RestController
@RequestMapping("/api/reputation/eventos")
public class EventoController {

    private final ResenaService resenaService;
    private final ReputacionService reputacionService;

    public EventoController(ResenaService resenaService, ReputacionService reputacionService) {
        this.resenaService = resenaService;
        this.reputacionService = reputacionService;
    }

    @Operation(summary = "Aviso de Service: la solicitud quedó Cumplida")
    @PostMapping("/servicios-cumplidos")
    public ServicioCalificableResponse servicioCumplido(@Valid @RequestBody ServicioCumplidoEvento evento) {
        return resenaService.registrarServicioCumplido(evento);
    }

    @Operation(summary = "Aviso de Service: el trabajador canceló una solicitud")
    @PostMapping("/cancelaciones-tardias")
    public ResultadoPenalizacionResponse cancelacionTardia(@Valid @RequestBody CancelacionTardiaEvento evento) {
        return reputacionService.aplicarCancelacion(evento);
    }
}

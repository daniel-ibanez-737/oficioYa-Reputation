package edu.eci.dosw.oficioya.reputation.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import edu.eci.dosw.oficioya.reputation.dto.ReputacionContratanteResponse;
import edu.eci.dosw.oficioya.reputation.dto.ReputacionResumenResponse;
import edu.eci.dosw.oficioya.reputation.dto.ReputacionTrabajadorResponse;
import edu.eci.dosw.oficioya.reputation.service.ReputacionService;

/**
 * Consulta de reputación (OFICIO-167).
 */
@Tag(name = "Reputación", description = "Consulta de reputación")
@RestController
@RequestMapping("/api/v1/reputation")
public class ReputacionController {

    private final ReputacionService reputacionService;

    public ReputacionController(ReputacionService reputacionService) {
        this.reputacionService = reputacionService;
    }

    @Operation(summary = "Reputación de un trabajador")
    @GetMapping("/trabajadores/{trabajadorId}")
    public ReputacionTrabajadorResponse trabajador(@PathVariable Integer trabajadorId) {
        return reputacionService.consultarTrabajador(trabajadorId);
    }

    @Operation(summary = "Reputación de un contratante")
    @GetMapping("/contratantes/{contratanteId}")
    public ReputacionContratanteResponse contratante(@PathVariable Integer contratanteId) {
        return reputacionService.consultarContratante(contratanteId);
    }

    /** Usado por Search (vía Orchestrator) para ordenar sus resultados. */
    @Operation(summary = "Reputación de varios trabajadores (para Search)")
    @GetMapping("/reputaciones")
    public List<ReputacionResumenResponse> resumen(@RequestParam(required = false) List<Integer> trabajadorIds) {
        return reputacionService.consultarResumen(trabajadorIds);
    }
}

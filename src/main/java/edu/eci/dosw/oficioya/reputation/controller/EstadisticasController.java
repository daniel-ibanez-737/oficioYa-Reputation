package edu.eci.dosw.oficioya.reputation.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import edu.eci.dosw.oficioya.reputation.dto.EstadisticasReputacionResponse;
import edu.eci.dosw.oficioya.reputation.service.EstadisticasService;

/**
 * Estadísticas agregadas que consume Reporting (vía Orchestrator). Solo administradores.
 */
@Tag(name = "Estadísticas", description = "Datos agregados para Reporting (administrador)")
@RestController
@RequestMapping("/api/v1/reputation/estadisticas")
public class EstadisticasController {

    private final EstadisticasService estadisticasService;

    public EstadisticasController(EstadisticasService estadisticasService) {
        this.estadisticasService = estadisticasService;
    }

    @Operation(summary = "Estadísticas de reseñas y reportes de un periodo")
    @GetMapping
    public EstadisticasReputacionResponse estadisticas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return estadisticasService.calcular(desde, hasta);
    }
}

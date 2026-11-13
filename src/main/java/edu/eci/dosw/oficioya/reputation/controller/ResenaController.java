package edu.eci.dosw.oficioya.reputation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import edu.eci.dosw.oficioya.reputation.dto.CalificacionRequest;
import edu.eci.dosw.oficioya.reputation.dto.ReporteRequest;
import edu.eci.dosw.oficioya.reputation.dto.ReporteResponse;
import edu.eci.dosw.oficioya.reputation.dto.ResenaResponse;
import edu.eci.dosw.oficioya.reputation.dto.UsuarioAutenticado;
import edu.eci.dosw.oficioya.reputation.service.ModeracionService;
import edu.eci.dosw.oficioya.reputation.service.ResenaService;
import jakarta.validation.Valid;

/**
 * Calificaciones y reseñas. Las reseñas solo se crean y se consultan: no hay endpoints para editarlas
 * ni borrarlas (RF-03).
 */
@Tag(name = "Reseñas", description = "Calificar, consultar y reportar reseñas")
@RestController
@RequestMapping("/api/v1/reputation")
public class ResenaController {

    private final ResenaService resenaService;
    private final ModeracionService moderacionService;

    public ResenaController(ResenaService resenaService, ModeracionService moderacionService) {
        this.resenaService = resenaService;
        this.moderacionService = moderacionService;
    }

    @Operation(summary = "Calificar a la otra parte de un servicio cumplido (1 a 5)")
    @PostMapping("/resenas")
    @ResponseStatus(HttpStatus.CREATED)
    public ResenaResponse calificar(@Parameter(hidden = true) JwtAuthenticationToken token, @Valid @RequestBody CalificacionRequest solicitud) {
        return resenaService.calificar(UsuarioAutenticado.desde(token), solicitud);
    }

    @Operation(summary = "Consultar una reseña por su código")
    @GetMapping("/resenas/{codigo}")
    public ResenaResponse obtener(@PathVariable UUID codigo) {
        return resenaService.obtenerPorCodigo(codigo);
    }

    @Operation(summary = "Reseñas visibles del perfil de un trabajador")
    @GetMapping("/trabajadores/{trabajadorId}/resenas")
    public List<ResenaResponse> resenasDeTrabajador(@PathVariable Integer trabajadorId) {
        return resenaService.resenasDeTrabajador(trabajadorId);
    }

    @Operation(summary = "Reportar una reseña falsa o inapropiada")
    @PostMapping("/resenas/{codigo}/reportes")
    @ResponseStatus(HttpStatus.CREATED)
    public ReporteResponse reportar(@Parameter(hidden = true) JwtAuthenticationToken token, @PathVariable UUID codigo,
                                    @Valid @RequestBody ReporteRequest solicitud) {
        return moderacionService.reportar(UsuarioAutenticado.desde(token), codigo, solicitud);
    }
}

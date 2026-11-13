package edu.eci.dosw.oficioya.reputation.controller;

import java.util.List;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import edu.eci.dosw.oficioya.reputation.domain.EstadoReporte;
import edu.eci.dosw.oficioya.reputation.dto.ReporteResponse;
import edu.eci.dosw.oficioya.reputation.dto.ResolucionRequest;
import edu.eci.dosw.oficioya.reputation.dto.UsuarioAutenticado;
import edu.eci.dosw.oficioya.reputation.service.ModeracionService;
import jakarta.validation.Valid;

/**
 * Moderación de reseñas reportadas. Solo para administradores (ver SecurityConfig).
 */
@Tag(name = "Moderación", description = "Moderación de reseñas reportadas (administrador)")
@RestController
@RequestMapping("/api/v1/reputation/reportes")
public class ModeracionController {

    private final ModeracionService moderacionService;

    public ModeracionController(ModeracionService moderacionService) {
        this.moderacionService = moderacionService;
    }

    @Operation(summary = "Listar reportes, opcionalmente por estado")
    @GetMapping
    public List<ReporteResponse> listar(@RequestParam(required = false) EstadoReporte estado) {
        return moderacionService.listar(estado);
    }

    @Operation(summary = "Resolver un reporte: retirar o mantener la reseña")
    @PostMapping("/{reporteId}/resolucion")
    public ReporteResponse resolver(@Parameter(hidden = true) JwtAuthenticationToken token, @PathVariable Integer reporteId,
                                    @Valid @RequestBody ResolucionRequest solicitud) {
        return moderacionService.resolver(UsuarioAutenticado.desde(token), reporteId, solicitud.resultado());
    }
}

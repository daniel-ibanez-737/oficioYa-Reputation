package edu.eci.dosw.oficioya.reputation.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.eci.dosw.oficioya.reputation.client.OrquestadorClient;
import edu.eci.dosw.oficioya.reputation.domain.EstadoReporte;
import edu.eci.dosw.oficioya.reputation.domain.ReporteResena;
import edu.eci.dosw.oficioya.reputation.domain.Resena;
import edu.eci.dosw.oficioya.reputation.domain.ResultadoModeracion;
import edu.eci.dosw.oficioya.reputation.dto.AvisoReporteResenaDTO;
import edu.eci.dosw.oficioya.reputation.dto.ReporteRequest;
import edu.eci.dosw.oficioya.reputation.dto.ReporteResponse;
import edu.eci.dosw.oficioya.reputation.dto.UsuarioAutenticado;
import edu.eci.dosw.oficioya.reputation.exception.ConflictoException;
import edu.eci.dosw.oficioya.reputation.exception.IntegracionException;
import edu.eci.dosw.oficioya.reputation.exception.RecursoNoEncontradoException;
import edu.eci.dosw.oficioya.reputation.repository.ReporteResenaRepository;
import edu.eci.dosw.oficioya.reputation.repository.ResenaRepository;

/**
 * Reporte y moderación de reseñas (OFICIO-169, RF-04 y RNF-07).
 */
@Service
public class ModeracionService {

    private static final Logger log = LoggerFactory.getLogger(ModeracionService.class);

    private final ResenaRepository resenaRepository;
    private final ReporteResenaRepository reporteRepository;
    private final ReputacionService reputacionService;
    private final OrquestadorClient orquestador;
    private final UUID administradorId;
    private final String tipoAvisoReporte;

    public ModeracionService(ResenaRepository resenaRepository, ReporteResenaRepository reporteRepository,
                             ReputacionService reputacionService, OrquestadorClient orquestador,
                             @Value("${oficioya.reputation.administrador-id}") UUID administradorId,
                             @Value("${oficioya.reputation.tipo-aviso-reporte:}") String tipoAvisoReporte) {
        this.resenaRepository = resenaRepository;
        this.reporteRepository = reporteRepository;
        this.reputacionService = reputacionService;
        this.orquestador = orquestador;
        this.administradorId = administradorId;
        this.tipoAvisoReporte = tipoAvisoReporte;
    }

    /**
     * Un usuario reporta una reseña visible; queda Pendiente de revisión y se envía un correo al administrador
     * (vía Orchestrator) con la reseña, el motivo y la descripción.
     * SUPUESTO: la reseña sigue visible hasta que un administrador resuelva el reporte.
     */
    @Transactional
    public ReporteResponse reportar(UsuarioAutenticado usuario, UUID codigoResena, ReporteRequest solicitud) {
        Resena resena = resenaRepository.findByCodigo(codigoResena)
                .orElseThrow(() -> new RecursoNoEncontradoException("La reseña no existe"));
        if (!resena.isVisible()) {
            throw new ConflictoException("La reseña ya fue retirada");
        }
        if (reporteRepository.existsByResenaIdAndReportanteIdAndEstadoNot(resena.getId(), usuario.id(),
                EstadoReporte.RESUELTO)) {
            throw new ConflictoException("Ya tienes un reporte pendiente sobre esta reseña");
        }
        ReporteResena reporte = reporteRepository.save(new ReporteResena(resena, usuario.id(), solicitud.motivo(),
                solicitud.descripcion().trim(), LocalDateTime.now()));
        log.info("Reseña reportada: reporteId={} resena={} reportanteId={} motivo={}", reporte.getId(),
                codigoResena, usuario.id(), solicitud.motivo());
        avisarAlAdministrador(reporte);
        return ReporteResponse.desde(reporte);
    }

    /**
     * El reporte ya quedó guardado; si el correo no se puede enviar se registra el error y no se revierte
     * (el administrador igual lo ve en GET /reportes).
     */
    private void avisarAlAdministrador(ReporteResena reporte) {
        try {
            orquestador.enviarCorreoReporteResena(AvisoReporteResenaDTO.de(reporte, administradorId, tipoAvisoReporte));
        } catch (IntegracionException e) {
            log.error("No se pudo enviar el correo del reporte {} al administrador: {}", reporte.getId(),
                    e.getMessage());
        }
    }

    /** Reportes para el administrador, del más antiguo al más reciente; sin estado devuelve todos. */
    @Transactional(readOnly = true)
    public List<ReporteResponse> listar(EstadoReporte estado) {
        List<ReporteResena> reportes = estado == null
                ? reporteRepository.findAllByOrderByFechaReporteAscIdAsc()
                : reporteRepository.findByEstadoOrderByFechaReporteAscIdAsc(estado);
        return reportes.stream().map(ReporteResponse::desde).toList();
    }

    /**
     * El administrador resuelve el reporte. Si retira la reseña, se oculta, se resuelven los demás reportes
     * abiertos sobre ella y se recalcula la reputación del calificado. La decisión queda registrada.
     */
    @Transactional
    public ReporteResponse resolver(UsuarioAutenticado administrador, Integer reporteId,
                                    ResultadoModeracion resultado) {
        ReporteResena reporte = reporteRepository.findById(reporteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El reporte no existe"));
        LocalDateTime ahora = LocalDateTime.now();
        reporte.resolver(administrador.id(), resultado, ahora);

        Resena resena = reporte.getResena();
        if (resultado == ResultadoModeracion.RESENA_RETIRADA) {
            resena.retirar();
            reporteRepository.findByResenaIdAndEstadoNot(resena.getId(), EstadoReporte.RESUELTO).stream()
                    .filter(otro -> !otro.getId().equals(reporteId))
                    .forEach(otro -> otro.resolver(administrador.id(), resultado, ahora));
            reputacionService.actualizarTrasCambioDeResenas(resena.getReputacion());
        }
        log.info("Moderación registrada: reporteId={} resena={} administradorId={} resultado={}", reporteId,
                resena.getCodigo(), administrador.id(), resultado);
        return ReporteResponse.desde(reporte);
    }
}

package edu.eci.dosw.oficioya.reputation.service;

import java.util.UUID;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.eci.dosw.oficioya.reputation.client.OrquestadorClient;
import edu.eci.dosw.oficioya.reputation.domain.Penalizacion;
import edu.eci.dosw.oficioya.reputation.domain.ReglasReputacion;
import edu.eci.dosw.oficioya.reputation.domain.Resena;
import edu.eci.dosw.oficioya.reputation.domain.Reputacion;
import edu.eci.dosw.oficioya.reputation.domain.ReputacionContratante;
import edu.eci.dosw.oficioya.reputation.domain.ReputacionTrabajador;
import edu.eci.dosw.oficioya.reputation.domain.TipoParte;
import edu.eci.dosw.oficioya.reputation.dto.CambioEstadoTrabajadorDTO;
import edu.eci.dosw.oficioya.reputation.dto.CancelacionTardiaEvento;
import edu.eci.dosw.oficioya.reputation.dto.ReputacionContratanteResponse;
import edu.eci.dosw.oficioya.reputation.dto.ReputacionResumenResponse;
import edu.eci.dosw.oficioya.reputation.dto.ReputacionTrabajadorResponse;
import edu.eci.dosw.oficioya.reputation.dto.ResultadoPenalizacionResponse;
import edu.eci.dosw.oficioya.reputation.exception.IntegracionException;
import edu.eci.dosw.oficioya.reputation.repository.PenalizacionRepository;
import edu.eci.dosw.oficioya.reputation.repository.ReputacionContratanteRepository;
import edu.eci.dosw.oficioya.reputation.repository.ReputacionTrabajadorRepository;
import edu.eci.dosw.oficioya.reputation.repository.ResenaRepository;

/**
 * Casos de uso de la reputación: recálculo del promedio, pausado automático, penalizaciones,
 * y consultas (OFICIO-167 y OFICIO-168).
 */
@Service
public class ReputacionService {

    private static final Logger log = LoggerFactory.getLogger(ReputacionService.class);

    static final String MOTIVO_PAUSA = "Promedio inferior al umbral en las últimas reseñas";

    private final ReputacionTrabajadorRepository reputacionTrabajadorRepository;
    private final ReputacionContratanteRepository reputacionContratanteRepository;
    private final ResenaRepository resenaRepository;
    private final PenalizacionRepository penalizacionRepository;
    private final ReglasReputacion reglas;
    private final OrquestadorClient orquestador;

    public ReputacionService(ReputacionTrabajadorRepository reputacionTrabajadorRepository,
                             ReputacionContratanteRepository reputacionContratanteRepository,
                             ResenaRepository resenaRepository,
                             PenalizacionRepository penalizacionRepository,
                             ReglasReputacion reglas,
                             OrquestadorClient orquestador) {
        this.reputacionTrabajadorRepository = reputacionTrabajadorRepository;
        this.reputacionContratanteRepository = reputacionContratanteRepository;
        this.resenaRepository = resenaRepository;
        this.penalizacionRepository = penalizacionRepository;
        this.reglas = reglas;
        this.orquestador = orquestador;
    }

    /** Reputación del usuario calificado; se crea la primera vez que recibe una reseña o penalización. */
    @Transactional
    public Reputacion obtenerOCrear(TipoParte tipo, UUID usuarioId) {
        if (tipo == TipoParte.TRABAJADOR) {
            return obtenerOCrearTrabajador(usuarioId);
        }
        return reputacionContratanteRepository.findByContratanteId(usuarioId)
                .orElseGet(() -> reputacionContratanteRepository.save(new ReputacionContratante(usuarioId)));
    }

    private ReputacionTrabajador obtenerOCrearTrabajador(UUID trabajadorId) {
        return reputacionTrabajadorRepository.findByTrabajadorId(trabajadorId)
                .orElseGet(() -> reputacionTrabajadorRepository.save(new ReputacionTrabajador(trabajadorId)));
    }

    /**
     * Recalcula el promedio con las reseñas visibles (RF-02) y, si es un trabajador,
     * aplica la regla de pausado automático (RF-05).
     */
    @Transactional
    public void actualizarTrasCambioDeResenas(Reputacion reputacion) {
        Reputacion real = (Reputacion) Hibernate.unproxy(reputacion);
        List<Integer> puntuaciones = resenaRepository
                .findByReputacionIdAndVisibleTrueOrderByFechaCreacionDescIdDesc(real.getId())
                .stream().map(Resena::getPuntuacion).toList();
        LocalDateTime ahora = LocalDateTime.now();
        real.recalcular(puntuaciones, ahora);

        if (real instanceof ReputacionTrabajador trabajador && reglas.debePausar(puntuaciones)
                && trabajador.pausar(ahora)) {
            log.warn("Pausado automático: trabajadorId={} promedioUltimas{}={}", trabajador.getTrabajadorId(),
                    reglas.resenasEvaluadas(),
                    ReglasReputacion.promedio(puntuaciones.subList(0, reglas.resenasEvaluadas())));
            notificarEstado(trabajador, MOTIVO_PAUSA);
        }
    }

    /**
     * RF-06: descuenta puntos al trabajador si la cancelación fue tardía. Es idempotente por solicitud:
     * si el mismo aviso llega dos veces, la penalización no se aplica de nuevo.
     */
    @Transactional
    public ResultadoPenalizacionResponse aplicarCancelacion(CancelacionTardiaEvento evento) {
        if (penalizacionRepository.existsBySolicitudId(evento.solicitudId())) {
            return new ResultadoPenalizacionResponse(false, "La penalización de esta solicitud ya se había aplicado",
                    consultarTrabajador(evento.trabajadorId()));
        }
        if (!reglas.esCancelacionTardia(evento.fechaHoraServicio(), evento.fechaCancelacion())) {
            return new ResultadoPenalizacionResponse(false, "La cancelación se hizo dentro del plazo permitido",
                    consultarTrabajador(evento.trabajadorId()));
        }
        LocalDateTime ahora = LocalDateTime.now();
        ReputacionTrabajador trabajador = obtenerOCrearTrabajador(evento.trabajadorId());
        trabajador.penalizar(reglas.puntosPenalizacion(), ahora);
        penalizacionRepository.save(new Penalizacion(trabajador, evento.solicitudId(), reglas.puntosPenalizacion(),
                evento.fechaHoraServicio(), evento.fechaCancelacion(), ahora));
        log.info("Penalización aplicada: trabajadorId={} solicitudId={} puntos={} reputacion={}",
                evento.trabajadorId(), evento.solicitudId(), reglas.puntosPenalizacion(), trabajador.valorReputacion());
        return new ResultadoPenalizacionResponse(true, Penalizacion.MOTIVO_CANCELACION_TARDIA,
                ReputacionTrabajadorResponse.desde(trabajador));
    }

    @Transactional(readOnly = true)
    public ReputacionTrabajadorResponse consultarTrabajador(UUID trabajadorId) {
        return reputacionTrabajadorRepository.findByTrabajadorId(trabajadorId)
                .map(ReputacionTrabajadorResponse::desde)
                .orElseGet(() -> ReputacionTrabajadorResponse.sinResenas(trabajadorId));
    }

    @Transactional(readOnly = true)
    public ReputacionContratanteResponse consultarContratante(UUID contratanteId) {
        return reputacionContratanteRepository.findByContratanteId(contratanteId)
                .map(r -> new ReputacionContratanteResponse(contratanteId, r.getPromedioCalificaciones(),
                        r.getTotalResenas()))
                .orElseGet(() -> new ReputacionContratanteResponse(contratanteId, 0, 0));
    }

    /** Reputación de varios trabajadores a la vez (la consume Search). Los que no tienen reputación se omiten. */
    @Transactional(readOnly = true)
    public List<ReputacionResumenResponse> consultarResumen(Collection<UUID> trabajadorIds) {
        if (trabajadorIds == null || trabajadorIds.isEmpty()) {
            return List.of();
        }
        return reputacionTrabajadorRepository.findByTrabajadorIdIn(trabajadorIds).stream()
                .map(r -> new ReputacionResumenResponse(r.getTrabajadorId(), r.valorReputacion(), r.getTotalResenas()))
                .toList();
    }

    /**
     * El cambio de estado ya quedó guardado en Reputation; si Worker no responde, se registra el error
     * y no se revierte (Worker puede consultar el estado con GET /trabajadores/{id}).
     */
    private void notificarEstado(ReputacionTrabajador trabajador, String motivo) {
        try {
            orquestador.notificarCambioEstadoTrabajador(
                    new CambioEstadoTrabajadorDTO(trabajador.getTrabajadorId(), trabajador.getEstado(), motivo));
        } catch (IntegracionException e) {
            log.error("No se pudo avisar a Worker el estado {} del trabajador {}: {}", trabajador.getEstado(),
                    trabajador.getTrabajadorId(), e.getMessage());
        }
    }
}

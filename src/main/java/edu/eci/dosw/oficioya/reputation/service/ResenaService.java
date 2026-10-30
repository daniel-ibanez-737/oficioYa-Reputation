package edu.eci.dosw.oficioya.reputation.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.eci.dosw.oficioya.reputation.domain.Resena;
import edu.eci.dosw.oficioya.reputation.domain.Reputacion;
import edu.eci.dosw.oficioya.reputation.domain.ServicioCalificable;
import edu.eci.dosw.oficioya.reputation.domain.TipoParte;
import edu.eci.dosw.oficioya.reputation.dto.CalificacionRequest;
import edu.eci.dosw.oficioya.reputation.dto.ResenaResponse;
import edu.eci.dosw.oficioya.reputation.dto.ServicioCalificableResponse;
import edu.eci.dosw.oficioya.reputation.dto.ServicioCumplidoEvento;
import edu.eci.dosw.oficioya.reputation.dto.UsuarioAutenticado;
import edu.eci.dosw.oficioya.reputation.exception.ConflictoException;
import edu.eci.dosw.oficioya.reputation.exception.RecursoNoEncontradoException;
import edu.eci.dosw.oficioya.reputation.repository.ReputacionTrabajadorRepository;
import edu.eci.dosw.oficioya.reputation.repository.ResenaRepository;
import edu.eci.dosw.oficioya.reputation.repository.ServicioCalificableRepository;

/**
 * Registro y consulta de calificaciones y reseñas (OFICIO-166, OFICIO-167 y RF-07).
 */
@Service
public class ResenaService {

    private static final Logger log = LoggerFactory.getLogger(ResenaService.class);

    private final ServicioCalificableRepository servicioRepository;
    private final ResenaRepository resenaRepository;
    private final ReputacionTrabajadorRepository reputacionTrabajadorRepository;
    private final ReputacionService reputacionService;

    public ResenaService(ServicioCalificableRepository servicioRepository, ResenaRepository resenaRepository,
                         ReputacionTrabajadorRepository reputacionTrabajadorRepository,
                         ReputacionService reputacionService) {
        this.servicioRepository = servicioRepository;
        this.resenaRepository = resenaRepository;
        this.reputacionTrabajadorRepository = reputacionTrabajadorRepository;
        this.reputacionService = reputacionService;
    }

    /**
     * Habilita la calificación de un servicio que Service reportó como Cumplido.
     * Es idempotente: si el aviso llega repetido, se devuelve el registro existente.
     */
    @Transactional
    public ServicioCalificableResponse registrarServicioCumplido(ServicioCumplidoEvento evento) {
        ServicioCalificable servicio = servicioRepository.findBySolicitudId(evento.solicitudId())
                .orElseGet(() -> {
                    log.info("Servicio habilitado para calificar: solicitudId={} trabajadorId={} contratanteId={}",
                            evento.solicitudId(), evento.trabajadorId(), evento.contratanteId());
                    return servicioRepository.save(new ServicioCalificable(evento.solicitudId(),
                            evento.trabajadorId(), evento.contratanteId(), evento.fechaCumplida()));
                });
        return ServicioCalificableResponse.desde(servicio);
    }

    /**
     * RF-01 / RF-07: una parte del servicio califica a la otra. La reseña queda asociada al servicio
     * y a la reputación del calificado, y se recalcula el promedio.
     */
    @Transactional
    public ResenaResponse calificar(UsuarioAutenticado usuario, CalificacionRequest solicitud) {
        ServicioCalificable servicio = servicioRepository.findBySolicitudId(solicitud.solicitudId())
                .orElseThrow(() -> new ConflictoException(
                        "El servicio no está en estado Cumplida o aún no fue notificado a Reputation"));
        TipoParte autor = servicio.parteDe(usuario.id(), usuario.roles());
        servicio.registrarCalificacion(autor);

        TipoParte calificado = autor.contraparte();
        Reputacion reputacion = reputacionService.obtenerOCrear(calificado, servicio.idDe(calificado));
        Resena resena = resenaRepository.save(new Resena(servicio, reputacion, usuario.id(), autor,
                solicitud.puntuacion(), solicitud.comentario(), LocalDateTime.now()));
        reputacionService.actualizarTrasCambioDeResenas(reputacion);

        log.info("Calificación registrada: codigo={} solicitudId={} autor={} calificado={}:{} puntuacion={}",
                resena.getCodigo(), servicio.getSolicitudId(), autor, calificado, resena.getCalificadoId(),
                resena.getPuntuacion());
        return ResenaResponse.desde(resena);
    }

    /** RF-03: reseñas visibles del perfil de un trabajador, de la más reciente a la más antigua. */
    @Transactional(readOnly = true)
    public List<ResenaResponse> resenasDeTrabajador(Integer trabajadorId) {
        return reputacionTrabajadorRepository.findByTrabajadorId(trabajadorId)
                .map(r -> resenaRepository.findByReputacionIdAndVisibleTrueOrderByFechaCreacionDescIdDesc(r.getId())
                        .stream().map(ResenaResponse::desde).toList())
                .orElse(List.of());
    }

    /** RNF-04: una reseña se puede rastrear por su código único hasta el servicio que la originó. */
    @Transactional(readOnly = true)
    public ResenaResponse obtenerPorCodigo(UUID codigo) {
        return resenaRepository.findByCodigo(codigo)
                .filter(Resena::isVisible)
                .map(ResenaResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("La reseña no existe"));
    }
}

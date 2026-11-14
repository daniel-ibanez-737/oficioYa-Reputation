package edu.eci.dosw.oficioya.reputation.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import edu.eci.dosw.oficioya.reputation.dto.AvisoReporteResenaDTO;
import edu.eci.dosw.oficioya.reputation.dto.CambioEstadoTrabajadorDTO;

/**
 * Orchestrator simulado para el perfil {@code demo}: solo registra en el log los avisos que se enviarían.
 */
@Component
@Profile("demo")
public class OrquestadorClientSimulado implements OrquestadorClient {

    private static final Logger log = LoggerFactory.getLogger(OrquestadorClientSimulado.class);

    @Override
    public void notificarCambioEstadoTrabajador(CambioEstadoTrabajadorDTO cambio) {
        log.info("[demo] Aviso a Worker vía Orchestrator: trabajadorId={} estado={} motivo={}",
                cambio.trabajadorId(), cambio.estado(), cambio.motivo());
    }

    @Override
    public void enviarCorreoReporteResena(AvisoReporteResenaDTO aviso) {
        log.info("[demo] Aviso al administrador vía Orchestrator → Notification: para={} titulo=\"{}\" "
                + "solicitud={} detalle=\"{}\"", aviso.receiverId(), aviso.title(), aviso.requestId(),
                aviso.description());
    }
}

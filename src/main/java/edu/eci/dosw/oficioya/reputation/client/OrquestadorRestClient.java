package edu.eci.dosw.oficioya.reputation.client;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import edu.eci.dosw.oficioya.reputation.domain.EstadoTrabajador;
import edu.eci.dosw.oficioya.reputation.dto.AvisoReporteResenaDTO;
import edu.eci.dosw.oficioya.reputation.dto.CambioEstadoTrabajadorDTO;
import edu.eci.dosw.oficioya.reputation.exception.IntegracionException;

/**
 * Implementación REST de {@link OrquestadorClient}. Todo sale por el Orchestrator; las rutas son configurables y
 * los formatos son los de las APIs reales de Worker ({@code PUT /api/v1/workers/{id}/status}) y Notification
 * ({@code POST /api/v1/notification}).
 */
@Component
@Profile("!demo")
public class OrquestadorRestClient implements OrquestadorClient {

    private final RestClient restClient;
    private final String rutaEstadoTrabajador;
    private final String rutaCorreo;

    public OrquestadorRestClient(RestClient.Builder builder,
                                 @Value("${oficioya.orquestador.base-url}") String baseUrl,
                                 @Value("${oficioya.orquestador.rutas.estado-trabajador}") String rutaEstadoTrabajador,
                                 @Value("${oficioya.orquestador.rutas.correo}") String rutaCorreo) {
        this.restClient = builder.baseUrl(baseUrl).requestInterceptor(new PropagacionTokenInterceptor()).build();
        this.rutaEstadoTrabajador = rutaEstadoTrabajador;
        this.rutaCorreo = rutaCorreo;
    }

    @Override
    public void notificarCambioEstadoTrabajador(CambioEstadoTrabajadorDTO cambio) {
        try {
            restClient.put()
                    .uri(rutaEstadoTrabajador, cambio.trabajadorId())
                    // Formato real de Worker: {"status": "ACTIVE" | "PAUSED"}
                    .body(Map.of("status", cambio.estado() == EstadoTrabajador.PAUSADO ? "PAUSED" : "ACTIVE"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new IntegracionException("No fue posible avisar a Worker el cambio de estado del trabajador", e);
        }
    }

    @Override
    public void enviarCorreoReporteResena(AvisoReporteResenaDTO aviso) {
        try {
            restClient.post()
                    .uri(rutaCorreo)
                    .body(aviso)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new IntegracionException("No fue posible enviar el aviso del reporte al administrador", e);
        }
    }
}

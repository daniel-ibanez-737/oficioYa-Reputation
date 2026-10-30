package edu.eci.dosw.oficioya.reputation.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import edu.eci.dosw.oficioya.reputation.dto.CambioEstadoTrabajadorDTO;
import edu.eci.dosw.oficioya.reputation.exception.IntegracionException;

/**
 * Implementación REST de {@link OrquestadorClient}. La ruta es configurable
 * (SUPUESTO: pendiente de confirmar con Squad 1 y con el squad de Worker).
 */
@Component
@Profile("!demo")
public class OrquestadorRestClient implements OrquestadorClient {

    private final RestClient restClient;
    private final String rutaEstadoTrabajador;

    public OrquestadorRestClient(RestClient.Builder builder,
                                 @Value("${oficioya.orquestador.base-url}") String baseUrl,
                                 @Value("${oficioya.orquestador.rutas.estado-trabajador}") String rutaEstadoTrabajador) {
        this.restClient = builder.baseUrl(baseUrl).requestInterceptor(new PropagacionTokenInterceptor()).build();
        this.rutaEstadoTrabajador = rutaEstadoTrabajador;
    }

    @Override
    public void notificarCambioEstadoTrabajador(CambioEstadoTrabajadorDTO cambio) {
        try {
            restClient.put()
                    .uri(rutaEstadoTrabajador, cambio.trabajadorId())
                    .body(cambio)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new IntegracionException("No fue posible avisar a Worker el cambio de estado del trabajador", e);
        }
    }
}

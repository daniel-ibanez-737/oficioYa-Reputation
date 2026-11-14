package edu.eci.dosw.oficioya.reputation.client;

import static edu.eci.dosw.oficioya.reputation.IdsDePrueba.id;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

import edu.eci.dosw.oficioya.reputation.domain.EstadoTrabajador;
import edu.eci.dosw.oficioya.reputation.dto.AvisoReporteResenaDTO;
import edu.eci.dosw.oficioya.reputation.dto.CambioEstadoTrabajadorDTO;
import edu.eci.dosw.oficioya.reputation.exception.IntegracionException;

class OrquestadorClientTest {

    private static final String BASE = "http://orquestador";
    private static final CambioEstadoTrabajadorDTO PAUSA =
            new CambioEstadoTrabajadorDTO(id(101), EstadoTrabajador.PAUSADO, "baja reputación");
    private static final AvisoReporteResenaDTO CORREO = new AvisoReporteResenaDTO(id(900),
            "OficioYa · Reseña reportada (FALSA)", "Reporte 7", null, id(301), LocalDateTime.of(2026, 11, 11, 10, 0));

    private MockRestServiceServer servidor;
    private OrquestadorRestClient cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new OrquestadorRestClient(builder, BASE, "/api/v1/workers/{trabajadorId}/status",
                "/api/v1/notification");
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void avisaAWorkerPorElOrquestadorReenviandoElJwt() {
        Jwt jwt = Jwt.withTokenValue("token-sistema").header("alg", "HS256").subject("1").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        servidor.expect(requestTo(BASE + "/api/v1/workers/00000000-0000-0000-0000-000000000101/status"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("Authorization", "Bearer token-sistema"))
                .andExpect(content().json("{\"status\": \"PAUSED\"}"))
                .andRespond(withSuccess());

        cliente.notificarCambioEstadoTrabajador(PAUSA);

        servidor.verify();
    }

    @Test
    void losErroresSeConviertenEnIntegracionException() {
        servidor.expect(requestTo(BASE + "/api/v1/workers/00000000-0000-0000-0000-000000000101/status")).andRespond(withServerError());
        assertThatThrownBy(() -> cliente.notificarCambioEstadoTrabajador(PAUSA))
                .isInstanceOf(IntegracionException.class);
    }

    @Test
    void pideElCorreoDelReporteANotificacionesPorElOrquestador() {
        servidor.expect(requestTo(BASE + "/api/v1/notification"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"receiverId\": \"00000000-0000-0000-0000-000000000900\", "
                        + "\"requestId\": \"00000000-0000-0000-0000-000000000301\", \"description\": \"Reporte 7\"}"))
                .andRespond(withSuccess());

        cliente.enviarCorreoReporteResena(CORREO);

        servidor.verify();
    }

    @Test
    void siNotificacionesFallaElCorreoLanzaIntegracionException() {
        servidor.expect(requestTo(BASE + "/api/v1/notification")).andRespond(withServerError());
        assertThatThrownBy(() -> cliente.enviarCorreoReporteResena(CORREO))
                .isInstanceOf(IntegracionException.class);
    }

    @Test
    void elClienteSimuladoSoloRegistraElAviso() {
        OrquestadorClientSimulado simulado = new OrquestadorClientSimulado();
        assertThatCode(() -> simulado.notificarCambioEstadoTrabajador(PAUSA)).doesNotThrowAnyException();
        assertThatCode(() -> simulado.enviarCorreoReporteResena(CORREO)).doesNotThrowAnyException();
    }
}

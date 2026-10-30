package edu.eci.dosw.oficioya.reputation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.jayway.jsonpath.JsonPath;

import edu.eci.dosw.oficioya.reputation.client.OrquestadorClient;
import edu.eci.dosw.oficioya.reputation.domain.EstadoTrabajador;
import edu.eci.dosw.oficioya.reputation.dto.CambioEstadoTrabajadorDTO;
import edu.eci.dosw.oficioya.reputation.dto.EstadisticasReputacionResponse;
import edu.eci.dosw.oficioya.reputation.exception.IntegracionException;

/**
 * Pruebas de aceptación de Reputation de punta a punta: API REST + seguridad + servicios + JPA + Flyway (H2).
 * El Orchestrator se reemplaza por un mock. Cada prueba usa ids nuevos para no depender de las demás.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AceptacionReputationTest {

    private static final String BASE = "/api/reputation";
    private static final AtomicInteger SECUENCIA = new AtomicInteger(1000);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OrquestadorClient orquestador;

    private int trabajador;
    private int contratante;

    @BeforeEach
    void preparar() {
        trabajador = nuevoId();
        contratante = nuevoId();
        clearInvocations(orquestador);
    }

    // ---------- utilidades ----------

    private static int nuevoId() {
        return SECUENCIA.incrementAndGet();
    }

    private static RequestPostProcessor usuario(int id, String rol) {
        return jwt().jwt(j -> j.subject(String.valueOf(id))).authorities(new SimpleGrantedAuthority(rol));
    }

    private static RequestPostProcessor sistema() {
        return usuario(1, "ROLE_SISTEMA");
    }

    private static RequestPostProcessor administrador() {
        return usuario(2, "ROLE_ADMINISTRADOR");
    }

    private ResultActions servicioCumplido(int solicitud, int trabajadorId, int contratanteId) throws Exception {
        return mvc.perform(post(BASE + "/eventos/servicios-cumplidos").with(sistema())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"solicitudId": %d, "trabajadorId": %d, "contratanteId": %d,
                         "fechaCumplida": "2026-10-20T15:00:00"}""".formatted(solicitud, trabajadorId, contratanteId)));
    }

    private ResultActions calificar(int autorId, String rol, int solicitud, int puntuacion) throws Exception {
        return mvc.perform(post(BASE + "/resenas").with(usuario(autorId, rol))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"solicitudId\": %d, \"puntuacion\": %d, \"comentario\": \"Comentario %d\"}"
                        .formatted(solicitud, puntuacion, puntuacion)));
    }

    /** Crea un servicio cumplido y el contratante califica al trabajador; devuelve el código de la reseña. */
    private String contratanteCalifica(int puntuacion) throws Exception {
        int solicitud = nuevoId();
        servicioCumplido(solicitud, trabajador, contratante).andExpect(status().isOk());
        String json = calificar(contratante, "ROLE_CONTRATANTE", solicitud, puntuacion)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.codigo");
    }

    private ResultActions reportar(String codigo, int reportanteId) throws Exception {
        return mvc.perform(post(BASE + "/resenas/" + codigo + "/reportes").with(usuario(reportanteId, "ROLE_TRABAJADOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\": \"FALSA\", \"descripcion\": \"Nunca pasó\"}"));
    }

    private int reporteId(ResultActions resultado) throws Exception {
        return JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private ResultActions resolver(int reporteId, String resultado) throws Exception {
        return mvc.perform(post(BASE + "/reportes/" + reporteId + "/resolucion").with(administrador())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resultado\": \"" + resultado + "\"}"));
    }

    // ---------- RF-01 / RF-07: calificación mutua asociada al servicio ----------

    @Test
    void ambasPartesSeCalificanYLaResenaQuedaAsociadaAlServicio() throws Exception {
        int solicitud = nuevoId();
        servicioCumplido(solicitud, trabajador, contratante)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solicitudId").value(solicitud));

        String json = calificar(contratante, "ROLE_CONTRATANTE", solicitud, 5)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.solicitudId").value(solicitud))
                .andExpect(jsonPath("$.tipoAutor").value("CONTRATANTE"))
                .andExpect(jsonPath("$.calificadoId").value(trabajador))
                .andExpect(jsonPath("$.codigo").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        calificar(trabajador, "ROLE_TRABAJADOR", solicitud, 4)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoCalificado").value("CONTRATANTE"));

        String codigo = JsonPath.read(json, "$.codigo");
        mvc.perform(get(BASE + "/resenas/" + codigo).with(usuario(contratante, "ROLE_CONTRATANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solicitudId").value(solicitud));
        mvc.perform(get(BASE + "/contratantes/" + contratante).with(usuario(trabajador, "ROLE_TRABAJADOR")))
                .andExpect(jsonPath("$.promedioCalificaciones").value(4.0))
                .andExpect(jsonPath("$.totalResenas").value(1));
    }

    @Test
    void elAvisoDeServicioCumplidoEsIdempotente() throws Exception {
        int solicitud = nuevoId();
        servicioCumplido(solicitud, trabajador, contratante).andExpect(status().isOk());
        servicioCumplido(solicitud, trabajador, contratante)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trabajadorId").value(trabajador));
    }

    @Test
    void reglasDeLaCalificacion() throws Exception {
        int solicitud = nuevoId();
        // servicio que aún no está Cumplido
        calificar(contratante, "ROLE_CONTRATANTE", solicitud, 5).andExpect(status().isConflict());

        servicioCumplido(solicitud, trabajador, contratante);
        // puntuación fuera de rango
        calificar(contratante, "ROLE_CONTRATANTE", solicitud, 6).andExpect(status().isBadRequest());
        // alguien que no participó en el servicio
        calificar(nuevoId(), "ROLE_CONTRATANTE", solicitud, 5).andExpect(status().isForbidden());
        // solo una calificación por parte
        calificar(contratante, "ROLE_CONTRATANTE", solicitud, 5).andExpect(status().isCreated());
        calificar(contratante, "ROLE_CONTRATANTE", solicitud, 1).andExpect(status().isConflict());
        // cuerpo inválido
        mvc.perform(post(BASE + "/resenas").with(usuario(contratante, "ROLE_CONTRATANTE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{ no es json"))
                .andExpect(status().isBadRequest());
    }

    // ---------- RF-02 / RF-03: promedio y reseñas de solo lectura ----------

    @Test
    void elPromedioSeRecalculaYLasResenasSoloSePuedenConsultar() throws Exception {
        String primera = contratanteCalifica(5);
        contratanteCalifica(4);
        contratanteCalifica(3);

        mvc.perform(get(BASE + "/trabajadores/" + trabajador).with(usuario(contratante, "ROLE_CONTRATANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promedioCalificaciones").value(4.0))
                .andExpect(jsonPath("$.reputacion").value(4.0))
                .andExpect(jsonPath("$.totalResenas").value(3))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
        mvc.perform(get(BASE + "/trabajadores/" + trabajador + "/resenas").with(usuario(contratante, "ROLE_CONTRATANTE")))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].puntuacion").value(3))
                .andExpect(jsonPath("$[0].autorId").doesNotExist());

        // No existe forma de editar ni borrar una reseña.
        mvc.perform(delete(BASE + "/resenas/" + primera).with(usuario(contratante, "ROLE_CONTRATANTE")))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void trabajadorSinResenasTieneReputacionCero() throws Exception {
        mvc.perform(get(BASE + "/trabajadores/" + trabajador).with(usuario(contratante, "ROLE_CONTRATANTE")))
                .andExpect(jsonPath("$.reputacion").value(0.0))
                .andExpect(jsonPath("$.totalResenas").value(0));
        mvc.perform(get(BASE + "/trabajadores/" + trabajador + "/resenas").with(usuario(contratante, "ROLE_CONTRATANTE")))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(BASE + "/contratantes/" + contratante).with(usuario(trabajador, "ROLE_TRABAJADOR")))
                .andExpect(jsonPath("$.totalResenas").value(0));
    }

    @Test
    void searchConsultaLaReputacionDeVariosTrabajadores() throws Exception {
        contratanteCalifica(5);
        int otro = nuevoId();
        mvc.perform(get(BASE + "/reputaciones").param("trabajadorIds", String.valueOf(trabajador), String.valueOf(otro))
                        .with(usuario(contratante, "ROLE_CONTRATANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].trabajadorId").value(trabajador))
                .andExpect(jsonPath("$[0].reputacion").value(5.0));
        mvc.perform(get(BASE + "/reputaciones").with(usuario(contratante, "ROLE_CONTRATANTE")))
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---------- RF-05: pausado automático ----------

    @Test
    void sePausaConPromedioMenorATresEnLasUltimasCinco() throws Exception {
        for (int puntuacion : new int[] {1, 2, 3, 2}) {
            contratanteCalifica(puntuacion);
        }
        verify(orquestador, never()).notificarCambioEstadoTrabajador(any());
        contratanteCalifica(1);

        mvc.perform(get(BASE + "/trabajadores/" + trabajador).with(usuario(contratante, "ROLE_CONTRATANTE")))
                .andExpect(jsonPath("$.estado").value("PAUSADO"));
        ArgumentCaptor<CambioEstadoTrabajadorDTO> aviso = ArgumentCaptor.forClass(CambioEstadoTrabajadorDTO.class);
        verify(orquestador).notificarCambioEstadoTrabajador(aviso.capture());
        assertThat(aviso.getValue().trabajadorId()).isEqualTo(trabajador);
        assertThat(aviso.getValue().estado()).isEqualTo(EstadoTrabajador.PAUSADO);
    }

    @Test
    void siWorkerNoRespondeElPausadoIgualQuedaRegistrado() throws Exception {
        doThrow(new IntegracionException("caído", new RuntimeException()))
                .when(orquestador).notificarCambioEstadoTrabajador(any());
        for (int i = 0; i < 5; i++) {
            contratanteCalifica(1);
        }
        mvc.perform(get(BASE + "/trabajadores/" + trabajador).with(usuario(contratante, "ROLE_CONTRATANTE")))
                .andExpect(jsonPath("$.estado").value("PAUSADO"));
    }

    // ---------- RF-06: penalización por cancelación tardía ----------

    private ResultActions cancelacion(int solicitud, String fechaCancelacion) throws Exception {
        return mvc.perform(post(BASE + "/eventos/cancelaciones-tardias").with(sistema())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"solicitudId": %d, "trabajadorId": %d, "fechaHoraServicio": "2026-11-05T10:00:00",
                         "fechaCancelacion": "%s"}""".formatted(solicitud, trabajador, fechaCancelacion)));
    }

    @Test
    void cancelarConMenosDeVeinticuatroHorasDescuentaMedioPuntoUnaSolaVez() throws Exception {
        contratanteCalifica(5);
        int solicitud = nuevoId();

        cancelacion(solicitud, "2026-11-05T02:00:00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aplicada").value(true))
                .andExpect(jsonPath("$.reputacion.penalizacionAcumulada").value(0.5))
                .andExpect(jsonPath("$.reputacion.reputacion").value(4.5));
        cancelacion(solicitud, "2026-11-05T02:00:00")
                .andExpect(jsonPath("$.aplicada").value(false))
                .andExpect(jsonPath("$.reputacion.reputacion").value(4.5));
        cancelacion(nuevoId(), "2026-11-01T10:00:00")
                .andExpect(jsonPath("$.aplicada").value(false));

        // La penalización se mantiene aunque lleguen nuevas reseñas.
        contratanteCalifica(5);
        mvc.perform(get(BASE + "/trabajadores/" + trabajador).with(usuario(contratante, "ROLE_CONTRATANTE")))
                .andExpect(jsonPath("$.reputacion").value(4.5));
    }

    @Test
    void penalizacionDeUnTrabajadorSinResenasNoBajaDeCero() throws Exception {
        cancelacion(nuevoId(), "2026-11-05T09:00:00")
                .andExpect(jsonPath("$.aplicada").value(true))
                .andExpect(jsonPath("$.reputacion.reputacion").value(0.0));
    }

    // ---------- RF-04 / OFICIO-169: reporte y moderación ----------

    @Test
    void reportarYMantenerUnaResena() throws Exception {
        String codigo = contratanteCalifica(1);
        int reporte = reporteId(reportar(codigo, trabajador)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE")));
        reportar(codigo, trabajador).andExpect(status().isConflict());

        mvc.perform(get(BASE + "/reportes").param("estado", "PENDIENTE").with(administrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(reporte)));

        resolver(reporte, "RESENA_MANTENIDA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESUELTO"))
                .andExpect(jsonPath("$.moderadorId").value(2))
                .andExpect(jsonPath("$.fechaResolucion").isNotEmpty());
        resolver(reporte, "RESENA_RETIRADA").andExpect(status().isConflict());

        mvc.perform(get(BASE + "/resenas/" + codigo).with(usuario(trabajador, "ROLE_TRABAJADOR")))
                .andExpect(status().isOk());
        mvc.perform(get(BASE + "/reportes").with(administrador()))
                .andExpect(jsonPath("$[*].id", hasItem(reporte)));
    }

    @Test
    void retirarUnaResenaLaOcultaCierraLosDemasReportesYRecalculaElPromedio() throws Exception {
        contratanteCalifica(5);
        String injusta = contratanteCalifica(1);
        int primero = reporteId(reportar(injusta, trabajador));
        int segundo = reporteId(reportar(injusta, nuevoId()));

        resolver(primero, "RESENA_RETIRADA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("RESENA_RETIRADA"));

        mvc.perform(get(BASE + "/resenas/" + injusta).with(usuario(trabajador, "ROLE_TRABAJADOR")))
                .andExpect(status().isNotFound());
        mvc.perform(get(BASE + "/trabajadores/" + trabajador).with(usuario(contratante, "ROLE_CONTRATANTE")))
                .andExpect(jsonPath("$.promedioCalificaciones").value(5.0))
                .andExpect(jsonPath("$.totalResenas").value(1));
        mvc.perform(get(BASE + "/reportes").param("estado", "RESUELTO").with(administrador()))
                .andExpect(jsonPath("$[?(@.id == %d || @.id == %d)].id".formatted(primero, segundo),
                        containsInAnyOrder(primero, segundo)));
        reportar(injusta, nuevoId()).andExpect(status().isConflict());
    }

    @Test
    void reportesSobreRecursosInexistentes() throws Exception {
        reportar("00000000-0000-0000-0000-000000000000", trabajador).andExpect(status().isNotFound());
        resolver(999_999, "RESENA_MANTENIDA").andExpect(status().isNotFound());
        mvc.perform(post(BASE + "/reportes/1/resolucion").with(administrador())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Estadísticas para Reporting ----------

    private EstadisticasReputacionResponse estadisticasDeHoy() throws Exception {
        String hoy = java.time.LocalDate.now().toString();
        String json = mvc.perform(get(BASE + "/estadisticas").param("desde", hoy).param("hasta", hoy)
                        .with(administrador()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new EstadisticasReputacionResponse(((Number) JsonPath.read(json, "$.totalResenas")).longValue(),
                ((Number) JsonPath.read(json, "$.calificacionPromedio")).doubleValue(),
                ((Number) JsonPath.read(json, "$.resenasReportadas")).longValue(),
                ((Number) JsonPath.read(json, "$.reportesPendientes")).longValue(),
                ((Number) JsonPath.read(json, "$.reportesResueltos")).longValue());
    }

    @Test
    void reportingConsultaEstadisticasAgregadasDelPeriodo() throws Exception {
        EstadisticasReputacionResponse antes = estadisticasDeHoy();
        String codigo = contratanteCalifica(4);
        int reporte = reporteId(reportar(codigo, trabajador));
        reportar(contratanteCalifica(5), trabajador);
        resolver(reporte, "RESENA_MANTENIDA");

        EstadisticasReputacionResponse despues = estadisticasDeHoy();
        assertThat(despues.totalResenas()).isEqualTo(antes.totalResenas() + 2);
        assertThat(despues.resenasReportadas()).isEqualTo(antes.resenasReportadas() + 2);
        assertThat(despues.reportesResueltos()).isEqualTo(antes.reportesResueltos() + 1);
        assertThat(despues.reportesPendientes()).isEqualTo(antes.reportesPendientes() + 1);
        assertThat(despues.calificacionPromedio()).isBetween(1.0, 5.0);

        mvc.perform(get(BASE + "/estadisticas").param("desde", "2026-10-10").param("hasta", "2026-10-01")
                        .with(administrador()))
                .andExpect(status().isBadRequest());
        mvc.perform(get(BASE + "/estadisticas").param("desde", "2020-01-01").param("hasta", "2020-01-31")
                        .with(administrador()))
                .andExpect(jsonPath("$.totalResenas").value(0))
                .andExpect(jsonPath("$.calificacionPromedio").value(0.0));
        mvc.perform(get(BASE + "/estadisticas").param("desde", "2026-10-01").param("hasta", "2026-10-10")
                        .with(usuario(contratante, "ROLE_CONTRATANTE")))
                .andExpect(status().isForbidden());
    }

    // ---------- Seguridad ----------

    @Test
    void sinJwtResponde401() throws Exception {
        mvc.perform(get(BASE + "/trabajadores/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void permisosPorRol() throws Exception {
        // Solo el Orchestrator (o un administrador) envía avisos de otros dominios.
        servicioCumplido(nuevoId(), trabajador, contratante).andExpect(status().isOk());
        mvc.perform(post(BASE + "/eventos/servicios-cumplidos").with(usuario(contratante, "ROLE_CONTRATANTE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        // Moderación y estadísticas: solo administrador.
        mvc.perform(get(BASE + "/reportes").with(usuario(contratante, "ROLE_CONTRATANTE")))
                .andExpect(status().isForbidden());
        mvc.perform(get(BASE + "/estadisticas").with(usuario(contratante, "ROLE_CONTRATANTE")))
                .andExpect(status().isForbidden());
        // Un administrador no califica servicios.
        calificar(2, "ROLE_ADMINISTRADOR", nuevoId(), 5).andExpect(status().isForbidden());
        // El token debe identificar a un usuario numérico.
        mvc.perform(post(BASE + "/resenas").with(jwt().jwt(j -> j.subject("no-numerico"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CONTRATANTE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"solicitudId\": 1, \"puntuacion\": 5}"))
                .andExpect(status().isForbidden());
    }
}

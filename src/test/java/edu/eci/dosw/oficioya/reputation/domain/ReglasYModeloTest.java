package edu.eci.dosw.oficioya.reputation.domain;

import java.util.UUID;

import static edu.eci.dosw.oficioya.reputation.IdsDePrueba.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import edu.eci.dosw.oficioya.reputation.exception.ConflictoException;
import edu.eci.dosw.oficioya.reputation.exception.OperacionNoPermitidaException;
import edu.eci.dosw.oficioya.reputation.exception.SolicitudInvalidaException;

class ReglasYModeloTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 30, 10, 0);

    private final ReglasReputacion reglas = new ReglasReputacion(3.0, 5, 0.5, 24);

    // ---------- Reglas (OFICIO-168) ----------

    @Test
    void promedioRedondeaADosDecimalesYEsCeroSinPuntuaciones() {
        assertThat(ReglasReputacion.promedio(List.of(5, 4, 4))).isEqualTo(4.33);
        assertThat(ReglasReputacion.promedio(List.of())).isZero();
    }

    @Test
    void pausaSoloConCincoResenasYPromedioDeLasUltimasCincoMenorATres() {
        assertThat(reglas.debePausar(List.of(1, 1, 1, 1))).isFalse();
        assertThat(reglas.debePausar(List.of(2, 3, 2, 3, 2))).isTrue();
        assertThat(reglas.debePausar(List.of(3, 3, 3, 3, 3))).isFalse();
        // solo cuentan las 5 más recientes (primeras de la lista)
        assertThat(reglas.debePausar(List.of(5, 5, 5, 5, 5, 1, 1, 1))).isFalse();
    }

    @Test
    void cancelacionEsTardiaConMenosDeVeinticuatroHorasDeAnticipacion() {
        LocalDateTime servicio = AHORA.plusDays(2);
        assertThat(reglas.esCancelacionTardia(servicio, servicio.minusHours(23))).isTrue();
        assertThat(reglas.esCancelacionTardia(servicio, servicio.plusHours(1))).isTrue();
        assertThat(reglas.esCancelacionTardia(servicio, servicio.minusHours(24))).isFalse();
        assertThat(reglas.esCancelacionTardia(servicio, servicio.minusDays(2))).isFalse();
        assertThat(reglas.puntosPenalizacion()).isEqualTo(0.5);
    }

    // ---------- Servicio calificable ----------

    private final ServicioCalificable servicio = new ServicioCalificable(id(1), id(10), id(20), AHORA);

    @Test
    void identificaLaParteSegunIdYRol() {
        assertThat(servicio.parteDe(id(10), Set.of(TipoParte.TRABAJADOR))).isEqualTo(TipoParte.TRABAJADOR);
        assertThat(servicio.parteDe(id(20), Set.of(TipoParte.CONTRATANTE))).isEqualTo(TipoParte.CONTRATANTE);
        Set<TipoParte> soloContratante = Set.of(TipoParte.CONTRATANTE);
        assertThatThrownBy(() -> servicio.parteDe(id(10), soloContratante))
                .isInstanceOf(OperacionNoPermitidaException.class);
        Set<TipoParte> ambos = Set.of(TipoParte.TRABAJADOR, TipoParte.CONTRATANTE);
        assertThatThrownBy(() -> servicio.parteDe(id(99), ambos)).isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    void cadaParteCalificaUnaSolaVez() {
        servicio.registrarCalificacion(TipoParte.CONTRATANTE);
        assertThat(servicio.yaCalifico(TipoParte.CONTRATANTE)).isTrue();
        assertThat(servicio.yaCalifico(TipoParte.TRABAJADOR)).isFalse();
        assertThatThrownBy(() -> servicio.registrarCalificacion(TipoParte.CONTRATANTE))
                .isInstanceOf(ConflictoException.class);
        servicio.registrarCalificacion(TipoParte.TRABAJADOR);
        assertThat(servicio.yaCalifico(TipoParte.TRABAJADOR)).isTrue();
    }

    // ---------- Reseña ----------

    @Test
    void laResenaApuntaALaContraparteYValidaLaPuntuacion() {
        ReputacionTrabajador reputacion = new ReputacionTrabajador(id(10));
        Resena resena = new Resena(servicio, reputacion, id(20), TipoParte.CONTRATANTE, 5, "  Excelente  ", AHORA);
        assertThat(resena.getCalificadoId()).isEqualTo(id(10));
        assertThat(resena.getTipoCalificado()).isEqualTo(TipoParte.TRABAJADOR);
        assertThat(resena.getComentario()).isEqualTo("Excelente");
        assertThat(resena.getCodigo()).isNotNull();
        assertThat(resena.isVisible()).isTrue();
        resena.retirar();
        assertThat(resena.isVisible()).isFalse();

        assertThat(new Resena(servicio, reputacion, id(20), TipoParte.CONTRATANTE, 3, " ", AHORA).getComentario()).isNull();
        assertThatThrownBy(() -> new Resena(servicio, reputacion, id(20), TipoParte.CONTRATANTE, 0, null, AHORA))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> new Resena(servicio, reputacion, id(20), TipoParte.CONTRATANTE, 6, null, AHORA))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> new Resena(servicio, reputacion, id(10), TipoParte.TRABAJADOR, 4, null, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- Reputación ----------

    @Test
    void reputacionDelTrabajadorDescuentaPenalizacionesSinBajarDeCero() {
        ReputacionTrabajador trabajador = new ReputacionTrabajador(id(10));
        trabajador.recalcular(List.of(5, 4), AHORA);
        assertThat(trabajador.getPromedioCalificaciones()).isEqualTo(4.5);
        trabajador.penalizar(0.5, AHORA);
        assertThat(trabajador.valorReputacion()).isEqualTo(4.0);
        trabajador.penalizar(10, AHORA);
        assertThat(trabajador.valorReputacion()).isZero();
        assertThat(trabajador.getTipo()).isEqualTo(TipoParte.TRABAJADOR);
    }

    @Test
    void pausarEsIdempotente() {
        ReputacionTrabajador trabajador = new ReputacionTrabajador(id(10));
        assertThat(trabajador.getEstado()).isEqualTo(EstadoTrabajador.ACTIVO);
        assertThat(trabajador.pausar(AHORA)).isTrue();
        assertThat(trabajador.getEstado()).isEqualTo(EstadoTrabajador.PAUSADO);
        assertThat(trabajador.getFechaPausado()).isEqualTo(AHORA);
        assertThat(trabajador.pausar(AHORA)).isFalse();
    }

    @Test
    void reputacionDelContratanteEsSuPromedio() {
        ReputacionContratante contratante = new ReputacionContratante(id(20));
        contratante.recalcular(List.of(4, 3), AHORA);
        assertThat(contratante.valorReputacion()).isEqualTo(3.5);
        assertThat(contratante.getTipo()).isEqualTo(TipoParte.CONTRATANTE);
        assertThat(TipoParte.CONTRATANTE.contraparte()).isEqualTo(TipoParte.TRABAJADOR);
    }

    // ---------- Reporte ----------

    @Test
    void unReporteSeResuelveUnaSolaVez() {
        Resena resena = new Resena(servicio, new ReputacionTrabajador(id(10)), id(20), TipoParte.CONTRATANTE, 1, null, AHORA);
        ReporteResena reporte = new ReporteResena(resena, id(10), MotivoReporte.FALSA, " ", AHORA);
        assertThat(reporte.getEstado()).isEqualTo(EstadoReporte.PENDIENTE);
        assertThat(reporte.getDescripcion()).isNull();
        reporte.resolver(id(1), ResultadoModeracion.RESENA_MANTENIDA, AHORA);
        assertThat(reporte.estaResuelto()).isTrue();
        assertThat(reporte.getModeradorId()).isEqualTo(id(1));
        assertThatThrownBy(() -> reporte.resolver(id(1), ResultadoModeracion.RESENA_RETIRADA, AHORA))
                .isInstanceOf(ConflictoException.class);
    }
}

package edu.eci.dosw.oficioya.reputation.domain;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Reglas de reputación (OFICIO-168). Los valores vienen de configuración
 * ({@code oficioya.reputation.reglas.*}) para poder ajustarlos sin recompilar
 * si la profesora o el negocio los cambian.
 */
@Component
public class ReglasReputacion {

    private final double umbralPausa;
    private final int resenasEvaluadas;
    private final double puntosPenalizacion;
    private final long horasLimiteCancelacion;

    public ReglasReputacion(@Value("${oficioya.reputation.reglas.umbral-pausa:3.0}") double umbralPausa,
                            @Value("${oficioya.reputation.reglas.resenas-evaluadas:5}") int resenasEvaluadas,
                            @Value("${oficioya.reputation.reglas.penalizacion-cancelacion-tardia:0.5}")
                            double puntosPenalizacion,
                            @Value("${oficioya.reputation.reglas.horas-limite-cancelacion:24}")
                            long horasLimiteCancelacion) {
        this.umbralPausa = umbralPausa;
        this.resenasEvaluadas = resenasEvaluadas;
        this.puntosPenalizacion = puntosPenalizacion;
        this.horasLimiteCancelacion = horasLimiteCancelacion;
    }

    /** Promedio redondeado a dos decimales; 0 si no hay puntuaciones. */
    public static double promedio(List<Integer> puntuaciones) {
        if (puntuaciones.isEmpty()) {
            return 0;
        }
        double promedio = puntuaciones.stream().mapToInt(Integer::intValue).average().orElse(0);
        return Math.round(promedio * 100) / 100.0;
    }

    /**
     * RF-05: se pausa si hay al menos N reseñas y el promedio de las N más recientes es inferior al umbral.
     *
     * @param puntuacionesRecientes puntuaciones visibles ordenadas de la más reciente a la más antigua
     */
    public boolean debePausar(List<Integer> puntuacionesRecientes) {
        if (puntuacionesRecientes.size() < resenasEvaluadas) {
            return false;
        }
        return promedio(puntuacionesRecientes.subList(0, resenasEvaluadas)) < umbralPausa;
    }

    /**
     * RF-06: la cancelación es tardía si ocurre con menos de las horas límite de anticipación
     * (o después de la hora del servicio).
     */
    public boolean esCancelacionTardia(LocalDateTime fechaHoraServicio, LocalDateTime fechaCancelacion) {
        return Duration.between(fechaCancelacion, fechaHoraServicio).compareTo(Duration.ofHours(horasLimiteCancelacion)) < 0;
    }

    public double puntosPenalizacion() {
        return puntosPenalizacion;
    }

    public int resenasEvaluadas() {
        return resenasEvaluadas;
    }
}

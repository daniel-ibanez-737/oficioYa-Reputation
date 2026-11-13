package edu.eci.dosw.oficioya.reputation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del microservicio oficioya-reputation.
 * Calificaciones mutuas, reseñas inmutables, reportes de reseñas, pausado automático y penalizaciones.
 */
@SpringBootApplication
public class OficioyaReputationApplication {

    public static void main(String[] args) {
        SpringApplication.run(OficioyaReputationApplication.class, args);
    }
}

package edu.eci.dosw.oficioya.reputation.domain;

/**
 * Parte de un servicio: quien califica o quien es calificado.
 */
public enum TipoParte {
    TRABAJADOR,
    CONTRATANTE;

    public TipoParte contraparte() {
        return this == TRABAJADOR ? CONTRATANTE : TRABAJADOR;
    }
}

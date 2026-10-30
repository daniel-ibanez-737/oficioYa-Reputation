package edu.eci.dosw.oficioya.reputation.exception;

/**
 * Falla al comunicarse con otro dominio a través del Orchestrator.
 */
public class IntegracionException extends RuntimeException {

    public IntegracionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

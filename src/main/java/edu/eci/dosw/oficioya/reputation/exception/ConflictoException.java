package edu.eci.dosw.oficioya.reputation.exception;

/**
 * Se traduce a HTTP 409 — la operación contradice el estado actual (por ejemplo, calificar dos veces).
 */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}

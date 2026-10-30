package edu.eci.dosw.oficioya.reputation.exception;

/**
 * Se traduce a HTTP 400 — la petición no cumple una regla de negocio.
 */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}

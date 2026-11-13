package edu.eci.dosw.oficioya.reputation.exception;

/**
 * Se traduce a HTTP 403 — el usuario no puede realizar esta operación sobre el recurso.
 */
public class OperacionNoPermitidaException extends RuntimeException {

    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}

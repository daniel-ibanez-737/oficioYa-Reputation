package edu.eci.dosw.oficioya.reputation.exception;

/**
 * Se traduce a HTTP 404 — el recurso solicitado no existe.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}

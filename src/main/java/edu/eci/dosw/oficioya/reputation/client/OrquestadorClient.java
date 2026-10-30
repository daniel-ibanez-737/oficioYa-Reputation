package edu.eci.dosw.oficioya.reputation.client;

import edu.eci.dosw.oficioya.reputation.dto.CambioEstadoTrabajadorDTO;

/**
 * Única salida de Reputation hacia otros dominios: todo pasa por el Orchestrator.
 */
public interface OrquestadorClient {

    /** Avisa a Worker que un trabajador quedó pausado. */
    void notificarCambioEstadoTrabajador(CambioEstadoTrabajadorDTO cambio);
}

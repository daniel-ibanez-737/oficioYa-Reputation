package edu.eci.dosw.oficioya.reputation.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * Reputación de un contratante: el promedio de las calificaciones que le dan los trabajadores.
 */
@Entity
@Table(name = "reputacion_contratante")
@DiscriminatorValue("CONTRATANTE")
@PrimaryKeyJoinColumn(name = "id")
public class ReputacionContratante extends Reputacion {

    @Column(name = "contratante_id", nullable = false, unique = true)
    private UUID contratanteId;

    protected ReputacionContratante() {
        // requerido por JPA
    }

    public ReputacionContratante(UUID contratanteId) {
        this.contratanteId = contratanteId;
    }

    @Override
    public double valorReputacion() {
        return getPromedioCalificaciones();
    }

    @Override
    public TipoParte getTipo() {
        return TipoParte.CONTRATANTE;
    }

    public UUID getContratanteId() {
        return contratanteId;
    }
}

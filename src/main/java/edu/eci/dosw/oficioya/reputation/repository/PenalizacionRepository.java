package edu.eci.dosw.oficioya.reputation.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.eci.dosw.oficioya.reputation.domain.Penalizacion;

public interface PenalizacionRepository extends JpaRepository<Penalizacion, Integer> {

    boolean existsBySolicitudId(UUID solicitudId);
}

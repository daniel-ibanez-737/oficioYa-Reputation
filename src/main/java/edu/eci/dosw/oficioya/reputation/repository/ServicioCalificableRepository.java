package edu.eci.dosw.oficioya.reputation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.eci.dosw.oficioya.reputation.domain.ServicioCalificable;

public interface ServicioCalificableRepository extends JpaRepository<ServicioCalificable, Integer> {

    Optional<ServicioCalificable> findBySolicitudId(Integer solicitudId);
}

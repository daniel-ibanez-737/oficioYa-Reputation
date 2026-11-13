package edu.eci.dosw.oficioya.reputation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.eci.dosw.oficioya.reputation.domain.ReputacionContratante;

public interface ReputacionContratanteRepository extends JpaRepository<ReputacionContratante, Integer> {

    Optional<ReputacionContratante> findByContratanteId(Integer contratanteId);
}

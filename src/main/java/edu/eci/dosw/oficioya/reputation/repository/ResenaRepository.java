package edu.eci.dosw.oficioya.reputation.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.eci.dosw.oficioya.reputation.domain.Resena;

public interface ResenaRepository extends JpaRepository<Resena, Integer> {

    Optional<Resena> findByCodigo(UUID codigo);

    /** Reseñas visibles de una reputación, de la más reciente a la más antigua. */
    List<Resena> findByReputacionIdAndVisibleTrueOrderByFechaCreacionDescIdDesc(Integer reputacionId);

    List<Resena> findByVisibleTrueAndFechaCreacionBetween(LocalDateTime inicio, LocalDateTime fin);
}

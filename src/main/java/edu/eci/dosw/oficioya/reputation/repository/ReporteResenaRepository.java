package edu.eci.dosw.oficioya.reputation.repository;

import java.util.UUID;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.eci.dosw.oficioya.reputation.domain.EstadoReporte;
import edu.eci.dosw.oficioya.reputation.domain.ReporteResena;

public interface ReporteResenaRepository extends JpaRepository<ReporteResena, Integer> {

    List<ReporteResena> findByEstadoOrderByFechaReporteAscIdAsc(EstadoReporte estado);

    List<ReporteResena> findAllByOrderByFechaReporteAscIdAsc();

    boolean existsByResenaIdAndReportanteIdAndEstadoNot(Integer resenaId, UUID reportanteId, EstadoReporte estado);

    List<ReporteResena> findByResenaIdAndEstadoNot(Integer resenaId, EstadoReporte estado);

    long countByFechaReporteBetween(LocalDateTime inicio, LocalDateTime fin);

    long countByFechaReporteBetweenAndEstado(LocalDateTime inicio, LocalDateTime fin, EstadoReporte estado);
}

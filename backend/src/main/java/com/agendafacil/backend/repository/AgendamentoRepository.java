package com.agendafacil.backend.repository;

import com.agendafacil.backend.dto.TopServicoResponse;
import com.agendafacil.backend.entity.Agendamento;
import com.agendafacil.backend.entity.StatusAgendamento;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long>, JpaSpecificationExecutor<Agendamento> {

    @Query("""
            SELECT a FROM Agendamento a
            WHERE a.profissional.id = :profissionalId
              AND a.status = :status
              AND a.inicio < :fim
              AND a.fim > :inicio
            """)
    List<Agendamento> findConflitantes(
            @Param("profissionalId") Long profissionalId,
            @Param("status") StatusAgendamento status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    List<Agendamento> findByClienteIdOrderByInicioDesc(Long clienteId);

    long countByInicioBetweenAndStatusIn(LocalDateTime inicio, LocalDateTime fim, List<StatusAgendamento> statuses);

    @Query("""
            SELECT COALESCE(SUM(a.precoCobrado), 0) FROM Agendamento a
            WHERE a.status = :status AND a.inicio >= :inicio AND a.inicio < :fim
            """)
    BigDecimal somarFaturamento(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("status") StatusAgendamento status);

    @Query("""
            SELECT new com.agendafacil.backend.dto.TopServicoResponse(a.servico.id, a.servico.nome, COUNT(a))
            FROM Agendamento a
            WHERE a.status = :status
            GROUP BY a.servico.id, a.servico.nome
            ORDER BY COUNT(a) DESC
            """)
    List<TopServicoResponse> buscarTopServicos(@Param("status") StatusAgendamento status, Pageable pageable);
}

package com.agendafacil.backend.repository;

import com.agendafacil.backend.entity.Agendamento;
import com.agendafacil.backend.entity.StatusAgendamento;
import java.time.LocalDateTime;
import java.util.List;
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
}

package com.agendafacil.backend.service;

import com.agendafacil.backend.dto.DashboardResponse;
import com.agendafacil.backend.dto.TopServicoResponse;
import com.agendafacil.backend.entity.StatusAgendamento;
import com.agendafacil.backend.repository.AgendamentoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private static final int TOP_SERVICOS_LIMIT = 5;

    private final AgendamentoRepository agendamentoRepository;

    public DashboardService(AgendamentoRepository agendamentoRepository) {
        this.agendamentoRepository = agendamentoRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse obterDashboard() {
        LocalDateTime inicioHoje = LocalDate.now().atStartOfDay();
        LocalDateTime inicioAmanha = inicioHoje.plusDays(1);
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime inicioMesSeguinte = inicioMes.plusMonths(1);

        long agendamentosHoje = agendamentoRepository.countByInicioBetweenAndStatusIn(
                inicioHoje, inicioAmanha, List.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONCLUIDO));

        BigDecimal faturamentoMes = agendamentoRepository.somarFaturamento(
                inicioMes, inicioMesSeguinte, StatusAgendamento.CONCLUIDO);

        List<TopServicoResponse> topServicos = agendamentoRepository.buscarTopServicos(
                StatusAgendamento.CONCLUIDO, PageRequest.of(0, TOP_SERVICOS_LIMIT));

        return new DashboardResponse(agendamentosHoje, faturamentoMes, topServicos);
    }
}

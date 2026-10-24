package com.agendafacil.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        long agendamentosHoje,
        BigDecimal faturamentoMes,
        List<TopServicoResponse> topServicos) {}

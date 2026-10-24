package com.agendafacil.backend.dto;

import com.agendafacil.backend.entity.Agendamento;
import com.agendafacil.backend.entity.StatusAgendamento;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AgendamentoResponse(
        Long id,
        Long profissionalId,
        String profissionalNome,
        Long servicoId,
        String servicoNome,
        LocalDateTime inicio,
        LocalDateTime fim,
        StatusAgendamento status,
        BigDecimal precoCobrado) {

    public static AgendamentoResponse from(Agendamento agendamento) {
        return new AgendamentoResponse(
                agendamento.getId(),
                agendamento.getProfissional().getId(),
                agendamento.getProfissional().getNome(),
                agendamento.getServico().getId(),
                agendamento.getServico().getNome(),
                agendamento.getInicio(),
                agendamento.getFim(),
                agendamento.getStatus(),
                agendamento.getPrecoCobrado());
    }
}

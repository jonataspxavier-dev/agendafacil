package com.agendafacil.backend.dto;

import com.agendafacil.backend.entity.Profissional;
import com.agendafacil.backend.entity.Servico;
import java.util.List;

public record ProfissionalResponse(Long id, String nome, boolean ativo, List<Long> servicoIds, List<JornadaResponse> jornada) {

    public static ProfissionalResponse from(Profissional profissional) {
        return new ProfissionalResponse(
                profissional.getId(),
                profissional.getNome(),
                profissional.isAtivo(),
                profissional.getServicos().stream().map(Servico::getId).toList(),
                profissional.getJornada().stream().map(JornadaResponse::from).toList());
    }
}

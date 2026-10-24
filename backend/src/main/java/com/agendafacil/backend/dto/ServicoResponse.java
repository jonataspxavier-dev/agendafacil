package com.agendafacil.backend.dto;

import com.agendafacil.backend.entity.Servico;
import java.math.BigDecimal;

public record ServicoResponse(Long id, String nome, String descricao, int duracaoMin, BigDecimal preco, boolean ativo) {

    public static ServicoResponse from(Servico servico) {
        return new ServicoResponse(
                servico.getId(),
                servico.getNome(),
                servico.getDescricao(),
                servico.getDuracaoMin(),
                servico.getPreco(),
                servico.isAtivo());
    }
}

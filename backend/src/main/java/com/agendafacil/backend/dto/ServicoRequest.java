package com.agendafacil.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ServicoRequest(
        @NotBlank String nome,
        String descricao,
        @Positive int duracaoMin,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal preco) {
}

package com.agendafacil.backend.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record AgendamentoRequest(@NotNull Long profissionalId, @NotNull Long servicoId, @NotNull LocalDateTime inicio) {
}

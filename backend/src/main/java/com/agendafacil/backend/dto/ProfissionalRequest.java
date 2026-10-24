package com.agendafacil.backend.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ProfissionalRequest(@NotBlank String nome, List<Long> servicoIds, List<JornadaRequest> jornada) {
}

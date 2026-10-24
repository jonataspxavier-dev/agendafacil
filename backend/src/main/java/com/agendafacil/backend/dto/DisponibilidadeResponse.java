package com.agendafacil.backend.dto;

import java.time.LocalDate;
import java.util.List;

public record DisponibilidadeResponse(Long profissionalId, Long servicoId, LocalDate data, List<SlotResponse> slots) {
}

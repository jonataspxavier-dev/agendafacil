package com.agendafacil.backend.dto;

import java.time.LocalDateTime;

public record SlotResponse(LocalDateTime inicio, LocalDateTime fim) {
}

package com.agendafacil.backend.dto;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record JornadaRequest(@NotNull DayOfWeek diaSemana, @NotNull LocalTime horaInicio, @NotNull LocalTime horaFim) {
}

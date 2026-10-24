package com.agendafacil.backend.dto;

import com.agendafacil.backend.entity.Jornada;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record JornadaResponse(DayOfWeek diaSemana, LocalTime horaInicio, LocalTime horaFim) {

    public static JornadaResponse from(Jornada jornada) {
        return new JornadaResponse(jornada.getDiaSemana(), jornada.getHoraInicio(), jornada.getHoraFim());
    }
}

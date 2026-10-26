package com.agendafacil.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.agendafacil.backend.entity.Agendamento;
import com.agendafacil.backend.entity.Jornada;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class DisponibilidadeServiceTest {

    private static final LocalDate SEGUNDA = LocalDate.of(2026, 1, 5); // a Monday / uma segunda-feira

    @Test
    void noJornadaMeansNoSlots() {
        List<LocalDateTime> slots = DisponibilidadeService.calcularSlotsDisponiveis(
                List.of(), 30, SEGUNDA, List.of());

        assertThat(slots).isEmpty();
    }

    @Test
    void oneWindowNoBookingsReturnsFullStepSequence() {
        Jornada manha = jornada(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));

        List<LocalDateTime> slots = DisponibilidadeService.calcularSlotsDisponiveis(
                List.of(manha), 30, SEGUNDA, List.of());

        assertThat(slots).containsExactly(
                LocalDateTime.of(2026, 1, 5, 9, 0),
                LocalDateTime.of(2026, 1, 5, 9, 30));
    }

    @Test
    void slotEndingExactlyAtWindowEndIsIncluded() {
        Jornada janela = jornada(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(9, 30));

        List<LocalDateTime> slots = DisponibilidadeService.calcularSlotsDisponiveis(
                List.of(janela), 30, SEGUNDA, List.of());

        assertThat(slots).containsExactly(LocalDateTime.of(2026, 1, 5, 9, 0));
    }

    @Test
    void existingBookingExcludesOverlappingSlotsOnly() {
        Jornada manha = jornada(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));
        Agendamento existente = agendamento(
                LocalDateTime.of(2026, 1, 5, 9, 30), LocalDateTime.of(2026, 1, 5, 10, 0));

        List<LocalDateTime> slots = DisponibilidadeService.calcularSlotsDisponiveis(
                List.of(manha), 30, SEGUNDA, List.of(existente));

        assertThat(slots).containsExactly(
                LocalDateTime.of(2026, 1, 5, 9, 0),
                LocalDateTime.of(2026, 1, 5, 10, 0),
                LocalDateTime.of(2026, 1, 5, 10, 30));
    }

    @Test
    void twoWindowsSameDayProduceSlotsFromBoth() {
        Jornada manha = jornada(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        Jornada tarde = jornada(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(15, 0));

        List<LocalDateTime> slots = DisponibilidadeService.calcularSlotsDisponiveis(
                List.of(manha, tarde), 30, SEGUNDA, List.of());

        assertThat(slots).containsExactly(
                LocalDateTime.of(2026, 1, 5, 9, 0),
                LocalDateTime.of(2026, 1, 5, 9, 30),
                LocalDateTime.of(2026, 1, 5, 14, 0),
                LocalDateTime.of(2026, 1, 5, 14, 30));
    }

    private Jornada jornada(DayOfWeek diaSemana, LocalTime inicio, LocalTime fim) {
        Jornada jornada = new Jornada();
        jornada.setDiaSemana(diaSemana);
        jornada.setHoraInicio(inicio);
        jornada.setHoraFim(fim);
        return jornada;
    }

    private Agendamento agendamento(LocalDateTime inicio, LocalDateTime fim) {
        Agendamento agendamento = new Agendamento();
        agendamento.setInicio(inicio);
        agendamento.setFim(fim);
        return agendamento;
    }
}

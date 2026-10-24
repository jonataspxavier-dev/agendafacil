package com.agendafacil.backend.service;

import com.agendafacil.backend.dto.SlotResponse;
import com.agendafacil.backend.entity.Agendamento;
import com.agendafacil.backend.entity.Jornada;
import com.agendafacil.backend.entity.Profissional;
import com.agendafacil.backend.entity.Servico;
import com.agendafacil.backend.entity.StatusAgendamento;
import com.agendafacil.backend.exception.InvalidRequestException;
import com.agendafacil.backend.exception.ResourceNotFoundException;
import com.agendafacil.backend.repository.AgendamentoRepository;
import com.agendafacil.backend.repository.ProfissionalRepository;
import com.agendafacil.backend.repository.ServicoRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DisponibilidadeService {

    private static final int GRANULARIDADE_MINUTOS = 30;

    private final ProfissionalRepository profissionalRepository;
    private final ServicoRepository servicoRepository;
    private final AgendamentoRepository agendamentoRepository;

    public DisponibilidadeService(
            ProfissionalRepository profissionalRepository,
            ServicoRepository servicoRepository,
            AgendamentoRepository agendamentoRepository) {
        this.profissionalRepository = profissionalRepository;
        this.servicoRepository = servicoRepository;
        this.agendamentoRepository = agendamentoRepository;
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> calcular(Long profissionalId, Long servicoId, LocalDate data) {
        Profissional profissional = profissionalRepository.findById(profissionalId)
                .orElseThrow(() -> new ResourceNotFoundException("Profissional não encontrado: " + profissionalId));
        Servico servico = servicoRepository.findById(servicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado: " + servicoId));

        boolean profissionalOfereceServico = profissional.getServicos().stream()
                .anyMatch(s -> s.getId().equals(servicoId));
        if (!profissionalOfereceServico) {
            throw new InvalidRequestException("Este profissional não oferece o serviço solicitado");
        }

        DayOfWeek diaSemana = data.getDayOfWeek();
        List<Jornada> jornadasDoDia = profissional.getJornada().stream()
                .filter(j -> j.getDiaSemana() == diaSemana)
                .toList();

        List<Agendamento> existentes = agendamentoRepository.findConflitantes(
                profissionalId, StatusAgendamento.AGENDADO, data.atStartOfDay(), data.plusDays(1).atStartOfDay());

        int duracaoMin = servico.getDuracaoMin();
        return calcularSlotsDisponiveis(jornadasDoDia, duracaoMin, data, existentes).stream()
                .map(inicio -> new SlotResponse(inicio, inicio.plusMinutes(duracaoMin)))
                .toList();
    }

    static List<LocalDateTime> calcularSlotsDisponiveis(
            List<Jornada> jornadasDoDia, int duracaoMin, LocalDate data, List<Agendamento> agendamentosExistentes) {
        List<LocalDateTime> slots = new ArrayList<>();

        for (Jornada jornada : jornadasDoDia) {
            LocalDateTime fimJanela = LocalDateTime.of(data, jornada.getHoraFim());
            LocalDateTime candidato = LocalDateTime.of(data, jornada.getHoraInicio());

            while (!candidato.plusMinutes(duracaoMin).isAfter(fimJanela)) {
                LocalDateTime fimCandidato = candidato.plusMinutes(duracaoMin);
                LocalDateTime inicioCandidato = candidato;

                boolean conflita = agendamentosExistentes.stream().anyMatch(
                        a -> a.getInicio().isBefore(fimCandidato) && inicioCandidato.isBefore(a.getFim()));

                if (!conflita) {
                    slots.add(candidato);
                }

                candidato = candidato.plusMinutes(GRANULARIDADE_MINUTOS);
            }
        }

        return slots;
    }
}

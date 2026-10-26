package com.agendafacil.backend.service;

import com.agendafacil.backend.dto.AgendamentoRequest;
import com.agendafacil.backend.dto.AgendamentoResponse;
import com.agendafacil.backend.entity.Agendamento;
import com.agendafacil.backend.entity.Jornada;
import com.agendafacil.backend.entity.Perfil;
import com.agendafacil.backend.entity.Profissional;
import com.agendafacil.backend.entity.Servico;
import com.agendafacil.backend.entity.StatusAgendamento;
import com.agendafacil.backend.entity.Usuario;
import com.agendafacil.backend.exception.ConflitoDeHorarioException;
import com.agendafacil.backend.exception.InvalidRequestException;
import com.agendafacil.backend.exception.ResourceNotFoundException;
import com.agendafacil.backend.repository.AgendamentoRepository;
import com.agendafacil.backend.repository.ProfissionalRepository;
import com.agendafacil.backend.repository.ServicoRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final ProfissionalRepository profissionalRepository;
    private final ServicoRepository servicoRepository;
    private final AuthService authService;

    public AgendamentoService(
            AgendamentoRepository agendamentoRepository,
            ProfissionalRepository profissionalRepository,
            ServicoRepository servicoRepository,
            AuthService authService) {
        this.agendamentoRepository = agendamentoRepository;
        this.profissionalRepository = profissionalRepository;
        this.servicoRepository = servicoRepository;
        this.authService = authService;
    }

    @Transactional
    public AgendamentoResponse criar(AgendamentoRequest request, Jwt jwt) {
        Usuario cliente = authService.getAuthenticatedUser(jwt);

        Profissional profissional = profissionalRepository.findById(request.profissionalId())
                .orElseThrow(() -> new ResourceNotFoundException("Profissional não encontrado: " + request.profissionalId()));
        Servico servico = servicoRepository.findById(request.servicoId())
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado: " + request.servicoId()));

        if (!profissional.isAtivo() || !servico.isAtivo()) {
            throw new InvalidRequestException("Profissional ou serviço indisponível");
        }

        boolean profissionalOfereceServico = profissional.getServicos().stream()
                .anyMatch(s -> s.getId().equals(servico.getId()));
        if (!profissionalOfereceServico) {
            throw new InvalidRequestException("Este profissional não oferece o serviço solicitado");
        }

        LocalDateTime inicio = request.inicio();
        if (!inicio.isAfter(LocalDateTime.now())) {
            throw new InvalidRequestException("O horário do agendamento deve ser no futuro");
        }

        LocalDateTime fim = inicio.plusMinutes(servico.getDuracaoMin());
        DayOfWeek diaSemana = inicio.getDayOfWeek();

        boolean dentroDaJornada = profissional.getJornada().stream().anyMatch(j -> cabeNaJanela(j, diaSemana, inicio, fim));
        if (!dentroDaJornada) {
            throw new InvalidRequestException("Horário fora da jornada do profissional");
        }

        List<Agendamento> conflitantes = agendamentoRepository.findConflitantes(
                profissional.getId(), StatusAgendamento.AGENDADO, inicio, fim);
        if (!conflitantes.isEmpty()) {
            throw new ConflitoDeHorarioException("Horário indisponível para este profissional");
        }

        Agendamento agendamento = new Agendamento();
        agendamento.setCliente(cliente);
        agendamento.setProfissional(profissional);
        agendamento.setServico(servico);
        agendamento.setInicio(inicio);
        agendamento.setFim(fim);
        agendamento.setStatus(StatusAgendamento.AGENDADO);
        agendamento.setPrecoCobrado(servico.getPreco());
        agendamento.setCriadoEm(LocalDateTime.now());

        try {
            agendamento = agendamentoRepository.saveAndFlush(agendamento);
        } catch (DataIntegrityViolationException e) {
            // Belt-and-suspenders: the service-layer pre-check above catches the
            // common case, but the DB exclusion constraint is the actual guarantee
            // under concurrent requests (see AgendamentoRepositoryTest).
            // Cinto e suspensório: a pré-checagem acima na camada de serviço cobre o
            // caso comum, mas a constraint de exclusão do banco é a garantia real
            // sob requisições concorrentes (ver AgendamentoRepositoryTest).
            throw new ConflitoDeHorarioException("Horário indisponível para este profissional");
        }

        return AgendamentoResponse.from(agendamento);
    }

    @Transactional
    public AgendamentoResponse cancelar(Long id, Jwt jwt) {
        Usuario usuarioAutenticado = authService.getAuthenticatedUser(jwt);
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado: " + id));

        boolean isAdmin = usuarioAutenticado.getPerfil() == Perfil.ADMIN;
        boolean isDono = agendamento.getCliente().getId().equals(usuarioAutenticado.getId());
        if (!isAdmin && !isDono) {
            throw new AccessDeniedException("Você não pode cancelar o agendamento de outro usuário");
        }

        if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
            throw new InvalidRequestException(
                    "Agendamento não pode ser cancelado (status atual: " + agendamento.getStatus() + ")");
        }

        if (!agendamento.getInicio().isAfter(LocalDateTime.now().plusHours(2))) {
            throw new InvalidRequestException("Cancelamento só é permitido até 2 horas antes do horário agendado");
        }

        agendamento.setStatus(StatusAgendamento.CANCELADO);
        return AgendamentoResponse.from(agendamentoRepository.save(agendamento));
    }

    @Transactional
    public AgendamentoResponse concluir(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado: " + id));

        if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
            throw new InvalidRequestException(
                    "Agendamento não pode ser concluído (status atual: " + agendamento.getStatus() + ")");
        }

        agendamento.setStatus(StatusAgendamento.CONCLUIDO);
        return AgendamentoResponse.from(agendamentoRepository.save(agendamento));
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> listarMeus(Jwt jwt) {
        Usuario cliente = authService.getAuthenticatedUser(jwt);
        return agendamentoRepository.findByClienteIdOrderByInicioDesc(cliente.getId()).stream()
                .map(AgendamentoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> listarAdminTodos(LocalDate data, StatusAgendamento status) {
        return buscarAdmin(data, status, Pageable.unpaged()).stream()
                .map(AgendamentoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AgendamentoResponse> listarAdmin(LocalDate data, StatusAgendamento status, Pageable pageable) {
        return buscarAdmin(data, status, pageable).map(AgendamentoResponse::from);
    }

    private Page<Agendamento> buscarAdmin(LocalDate data, StatusAgendamento status, Pageable pageable) {
        Specification<Agendamento> spec = Specification.allOf();

        if (data != null) {
            LocalDateTime inicioDia = data.atStartOfDay();
            LocalDateTime fimDia = data.plusDays(1).atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.and(
                    cb.greaterThanOrEqualTo(root.get("inicio"), inicioDia),
                    cb.lessThan(root.get("inicio"), fimDia)));
        }

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        return agendamentoRepository.findAll(spec, pageable);
    }

    private boolean cabeNaJanela(Jornada jornada, DayOfWeek diaSemana, LocalDateTime inicio, LocalDateTime fim) {
        return jornada.getDiaSemana() == diaSemana
                && !inicio.toLocalTime().isBefore(jornada.getHoraInicio())
                && !fim.toLocalTime().isAfter(jornada.getHoraFim());
    }
}

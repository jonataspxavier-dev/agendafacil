package com.agendafacil.backend.service;

import com.agendafacil.backend.dto.JornadaRequest;
import com.agendafacil.backend.dto.ProfissionalRequest;
import com.agendafacil.backend.dto.ProfissionalResponse;
import com.agendafacil.backend.entity.Jornada;
import com.agendafacil.backend.entity.Profissional;
import com.agendafacil.backend.entity.Servico;
import com.agendafacil.backend.exception.InvalidRequestException;
import com.agendafacil.backend.exception.ResourceNotFoundException;
import com.agendafacil.backend.repository.ProfissionalRepository;
import com.agendafacil.backend.repository.ServicoRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfissionalService {

    private final ProfissionalRepository profissionalRepository;
    private final ServicoRepository servicoRepository;

    public ProfissionalService(ProfissionalRepository profissionalRepository, ServicoRepository servicoRepository) {
        this.profissionalRepository = profissionalRepository;
        this.servicoRepository = servicoRepository;
    }

    @Transactional(readOnly = true)
    public List<ProfissionalResponse> listarTodosAtivos() {
        return profissionalRepository.findAllByAtivoTrue(Pageable.unpaged())
                .map(ProfissionalResponse::from)
                .getContent();
    }

    @Transactional(readOnly = true)
    public Page<ProfissionalResponse> listarAtivos(Pageable pageable) {
        return profissionalRepository.findAllByAtivoTrue(pageable).map(ProfissionalResponse::from);
    }

    @Transactional
    public ProfissionalResponse criar(ProfissionalRequest request) {
        Profissional profissional = new Profissional();
        profissional.setAtivo(true);
        aplicar(profissional, request);
        return ProfissionalResponse.from(profissionalRepository.save(profissional));
    }

    @Transactional
    public ProfissionalResponse atualizar(Long id, ProfissionalRequest request) {
        Profissional profissional = buscarOuFalhar(id);
        aplicar(profissional, request);
        return ProfissionalResponse.from(profissionalRepository.save(profissional));
    }

    @Transactional
    public void desativar(Long id) {
        Profissional profissional = buscarOuFalhar(id);
        profissional.setAtivo(false);
        profissionalRepository.save(profissional);
    }

    private Profissional buscarOuFalhar(Long id) {
        return profissionalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profissional não encontrado: " + id));
    }

    private void aplicar(Profissional profissional, ProfissionalRequest request) {
        profissional.setNome(request.nome());
        profissional.setServicos(resolverServicos(request.servicoIds()));
        substituirJornada(profissional, request.jornada());
    }

    private Set<Servico> resolverServicos(List<Long> servicoIds) {
        if (servicoIds == null || servicoIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Servico> servicos = servicoRepository.findAllById(servicoIds);
        if (servicos.size() != servicoIds.size()) {
            throw new InvalidRequestException("Um ou mais servicoIds não existem");
        }
        return new HashSet<>(servicos);
    }

    private void substituirJornada(Profissional profissional, List<JornadaRequest> jornadaRequests) {
        profissional.getJornada().clear();
        if (jornadaRequests == null) {
            return;
        }
        for (JornadaRequest jr : jornadaRequests) {
            Jornada jornada = new Jornada();
            jornada.setProfissional(profissional);
            jornada.setDiaSemana(jr.diaSemana());
            jornada.setHoraInicio(jr.horaInicio());
            jornada.setHoraFim(jr.horaFim());
            profissional.getJornada().add(jornada);
        }
    }
}

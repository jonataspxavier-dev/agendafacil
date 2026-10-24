package com.agendafacil.backend.service;

import com.agendafacil.backend.dto.ProfissionalResponse;
import com.agendafacil.backend.repository.ProfissionalRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfissionalService {

    private final ProfissionalRepository profissionalRepository;

    public ProfissionalService(ProfissionalRepository profissionalRepository) {
        this.profissionalRepository = profissionalRepository;
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
}

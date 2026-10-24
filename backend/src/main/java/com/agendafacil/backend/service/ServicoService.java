package com.agendafacil.backend.service;

import com.agendafacil.backend.dto.ServicoResponse;
import com.agendafacil.backend.repository.ServicoRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoService {

    private final ServicoRepository servicoRepository;

    public ServicoService(ServicoRepository servicoRepository) {
        this.servicoRepository = servicoRepository;
    }

    @Transactional(readOnly = true)
    public List<ServicoResponse> listarTodosAtivos() {
        return servicoRepository.findAllByAtivoTrue(Pageable.unpaged())
                .map(ServicoResponse::from)
                .getContent();
    }

    @Transactional(readOnly = true)
    public Page<ServicoResponse> listarAtivos(Pageable pageable) {
        return servicoRepository.findAllByAtivoTrue(pageable).map(ServicoResponse::from);
    }
}

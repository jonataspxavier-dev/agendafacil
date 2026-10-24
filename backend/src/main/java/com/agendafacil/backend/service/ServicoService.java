package com.agendafacil.backend.service;

import com.agendafacil.backend.dto.ServicoRequest;
import com.agendafacil.backend.dto.ServicoResponse;
import com.agendafacil.backend.entity.Servico;
import com.agendafacil.backend.exception.ResourceNotFoundException;
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

    @Transactional
    public ServicoResponse criar(ServicoRequest request) {
        Servico servico = new Servico();
        aplicar(servico, request);
        servico.setAtivo(true);
        return ServicoResponse.from(servicoRepository.save(servico));
    }

    @Transactional
    public ServicoResponse atualizar(Long id, ServicoRequest request) {
        Servico servico = buscarOuFalhar(id);
        aplicar(servico, request);
        return ServicoResponse.from(servicoRepository.save(servico));
    }

    @Transactional
    public void desativar(Long id) {
        Servico servico = buscarOuFalhar(id);
        servico.setAtivo(false);
        servicoRepository.save(servico);
    }

    private Servico buscarOuFalhar(Long id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado: " + id));
    }

    private void aplicar(Servico servico, ServicoRequest request) {
        servico.setNome(request.nome());
        servico.setDescricao(request.descricao());
        servico.setDuracaoMin(request.duracaoMin());
        servico.setPreco(request.preco());
    }
}

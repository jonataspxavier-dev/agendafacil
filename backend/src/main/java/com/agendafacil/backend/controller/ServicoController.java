package com.agendafacil.backend.controller;

import com.agendafacil.backend.dto.PageResponse;
import com.agendafacil.backend.dto.ServicoResponse;
import com.agendafacil.backend.service.ServicoService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/servicos")
public class ServicoController {

    private final ServicoService servicoService;

    public ServicoController(ServicoService servicoService) {
        this.servicoService = servicoService;
    }

    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        if (page == null && size == null) {
            return ResponseEntity.ok(servicoService.listarTodosAtivos());
        }

        Pageable pageable = buildPageable(page, size, sort);
        return ResponseEntity.ok(PageResponse.from(servicoService.listarAtivos(pageable)));
    }

    private Pageable buildPageable(Integer page, Integer size, String sort) {
        int resolvedPage = page != null ? page : 0;
        int resolvedSize = size != null ? size : 20;

        if (sort == null || sort.isBlank()) {
            return PageRequest.of(resolvedPage, resolvedSize);
        }

        String[] parts = sort.split(",");
        Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return PageRequest.of(resolvedPage, resolvedSize, Sort.by(direction, parts[0]));
    }
}

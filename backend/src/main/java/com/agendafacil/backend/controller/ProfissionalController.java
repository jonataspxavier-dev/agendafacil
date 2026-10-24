package com.agendafacil.backend.controller;

import com.agendafacil.backend.dto.PageResponse;
import com.agendafacil.backend.dto.ProfissionalRequest;
import com.agendafacil.backend.dto.ProfissionalResponse;
import com.agendafacil.backend.service.ProfissionalService;
import com.agendafacil.backend.web.PageableFactory;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profissionais")
public class ProfissionalController {

    private final ProfissionalService profissionalService;

    public ProfissionalController(ProfissionalService profissionalService) {
        this.profissionalService = profissionalService;
    }

    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        if (page == null && size == null) {
            return ResponseEntity.ok(profissionalService.listarTodosAtivos());
        }

        Pageable pageable = PageableFactory.from(page, size, sort);
        return ResponseEntity.ok(PageResponse.from(profissionalService.listarAtivos(pageable)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProfissionalResponse> criar(@Valid @RequestBody ProfissionalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profissionalService.criar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProfissionalResponse> atualizar(@PathVariable Long id, @Valid @RequestBody ProfissionalRequest request) {
        return ResponseEntity.ok(profissionalService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        profissionalService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}

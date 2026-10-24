package com.agendafacil.backend.controller;

import com.agendafacil.backend.dto.AgendamentoRequest;
import com.agendafacil.backend.dto.AgendamentoResponse;
import com.agendafacil.backend.service.AgendamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    public AgendamentoController(AgendamentoService agendamentoService) {
        this.agendamentoService = agendamentoService;
    }

    @PostMapping
    public ResponseEntity<AgendamentoResponse> criar(@Valid @RequestBody AgendamentoRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agendamentoService.criar(request, jwt));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<AgendamentoResponse> cancelar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(agendamentoService.cancelar(id, jwt));
    }

    @PatchMapping("/{id}/concluir")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AgendamentoResponse> concluir(@PathVariable Long id) {
        return ResponseEntity.ok(agendamentoService.concluir(id));
    }

    @GetMapping("/meus")
    public ResponseEntity<List<AgendamentoResponse>> meus(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(agendamentoService.listarMeus(jwt));
    }
}

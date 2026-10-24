package com.agendafacil.backend.controller;

import com.agendafacil.backend.dto.PageResponse;
import com.agendafacil.backend.entity.StatusAgendamento;
import com.agendafacil.backend.service.AgendamentoService;
import com.agendafacil.backend.web.PageableFactory;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/agendamentos")
public class AdminAgendamentoController {

    private final AgendamentoService agendamentoService;

    public AdminAgendamentoController(AgendamentoService agendamentoService) {
        this.agendamentoService = agendamentoService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listar(
            @RequestParam(required = false) LocalDate data,
            @RequestParam(required = false) StatusAgendamento status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        if (page == null && size == null) {
            return ResponseEntity.ok(agendamentoService.listarAdminTodos(data, status));
        }

        Pageable pageable = PageableFactory.from(page, size, sort);
        return ResponseEntity.ok(PageResponse.from(agendamentoService.listarAdmin(data, status, pageable)));
    }
}

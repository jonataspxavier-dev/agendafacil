package com.agendafacil.backend.controller;

import com.agendafacil.backend.dto.DisponibilidadeResponse;
import com.agendafacil.backend.service.DisponibilidadeService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/disponibilidade")
public class DisponibilidadeController {

    private final DisponibilidadeService disponibilidadeService;

    public DisponibilidadeController(DisponibilidadeService disponibilidadeService) {
        this.disponibilidadeService = disponibilidadeService;
    }

    @GetMapping
    public ResponseEntity<DisponibilidadeResponse> disponibilidade(
            @RequestParam Long profissionalId,
            @RequestParam Long servicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        var slots = disponibilidadeService.calcular(profissionalId, servicoId, data);
        return ResponseEntity.ok(new DisponibilidadeResponse(profissionalId, servicoId, data, slots));
    }
}

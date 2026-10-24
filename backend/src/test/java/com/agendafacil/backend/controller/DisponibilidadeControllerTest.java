package com.agendafacil.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agendafacil.backend.entity.Agendamento;
import com.agendafacil.backend.entity.Jornada;
import com.agendafacil.backend.entity.Perfil;
import com.agendafacil.backend.entity.Profissional;
import com.agendafacil.backend.entity.Servico;
import com.agendafacil.backend.entity.StatusAgendamento;
import com.agendafacil.backend.entity.Usuario;
import com.agendafacil.backend.repository.AgendamentoRepository;
import com.agendafacil.backend.repository.ProfissionalRepository;
import com.agendafacil.backend.repository.ServicoRepository;
import com.agendafacil.backend.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DisponibilidadeControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private ProfissionalRepository profissionalRepository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private static final LocalDate PROXIMA_SEGUNDA =
            LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

    @Test
    void returnsAvailableSlotsWithNoAuth() throws Exception {
        Servico servico = servicoRepository.save(novoServico());
        Profissional profissional = profissionalRepository.save(
                novoProfissionalComJornada(servico, LocalTime.of(9, 0), LocalTime.of(11, 0)));

        mockMvc.perform(get("/api/disponibilidade")
                        .param("profissionalId", profissional.getId().toString())
                        .param("servicoId", servico.getId().toString())
                        .param("data", PROXIMA_SEGUNDA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots.length()").value(4));
    }

    @Test
    void excludesSlotsAlreadyBooked() throws Exception {
        Servico servico = servicoRepository.save(novoServico());
        Profissional profissional = profissionalRepository.save(
                novoProfissionalComJornada(servico, LocalTime.of(9, 0), LocalTime.of(11, 0)));
        Usuario cliente = usuarioRepository.save(novoCliente());

        Agendamento ocupado = new Agendamento();
        ocupado.setCliente(cliente);
        ocupado.setProfissional(profissional);
        ocupado.setServico(servico);
        ocupado.setInicio(LocalDateTime.of(PROXIMA_SEGUNDA, LocalTime.of(9, 30)));
        ocupado.setFim(LocalDateTime.of(PROXIMA_SEGUNDA, LocalTime.of(10, 0)));
        ocupado.setStatus(StatusAgendamento.AGENDADO);
        ocupado.setPrecoCobrado(new BigDecimal("50.00"));
        ocupado.setCriadoEm(LocalDateTime.now());
        agendamentoRepository.save(ocupado);

        mockMvc.perform(get("/api/disponibilidade")
                        .param("profissionalId", profissional.getId().toString())
                        .param("servicoId", servico.getId().toString())
                        .param("data", PROXIMA_SEGUNDA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots.length()").value(3));
    }

    @Test
    void returns404WhenProfissionalDoesNotExist() throws Exception {
        Servico servico = servicoRepository.save(novoServico());

        mockMvc.perform(get("/api/disponibilidade")
                        .param("profissionalId", "999999")
                        .param("servicoId", servico.getId().toString())
                        .param("data", PROXIMA_SEGUNDA.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns400WhenProfissionalDoesNotOfferServico() throws Exception {
        Servico servico = servicoRepository.save(novoServico());
        Profissional profissional = profissionalRepository.save(novoProfissionalSemServicos());

        mockMvc.perform(get("/api/disponibilidade")
                        .param("profissionalId", profissional.getId().toString())
                        .param("servicoId", servico.getId().toString())
                        .param("data", PROXIMA_SEGUNDA.toString()))
                .andExpect(status().isBadRequest());
    }

    private Servico novoServico() {
        Servico servico = new Servico();
        servico.setNome("Corte Teste Disponibilidade");
        servico.setDescricao("desc");
        servico.setDuracaoMin(30);
        servico.setPreco(new BigDecimal("50.00"));
        servico.setAtivo(true);
        return servico;
    }

    private Profissional novoProfissionalComJornada(Servico servico, LocalTime inicio, LocalTime fim) {
        Profissional profissional = new Profissional();
        profissional.setNome("Profissional Teste Disponibilidade");
        profissional.setAtivo(true);
        profissional.setServicos(Set.of(servico));

        Jornada jornada = new Jornada();
        jornada.setProfissional(profissional);
        jornada.setDiaSemana(DayOfWeek.MONDAY);
        jornada.setHoraInicio(inicio);
        jornada.setHoraFim(fim);
        profissional.setJornada(List.of(jornada));

        return profissional;
    }

    private Profissional novoProfissionalSemServicos() {
        Profissional profissional = new Profissional();
        profissional.setNome("Profissional Sem Servicos");
        profissional.setAtivo(true);
        return profissional;
    }

    private Usuario novoCliente() {
        Usuario usuario = new Usuario();
        usuario.setNome("Cliente Disponibilidade");
        usuario.setEmail("cliente-disponibilidade-" + System.nanoTime() + "@example.com");
        usuario.setSenhaHash("hash-fake");
        usuario.setPerfil(Perfil.CLIENTE);
        usuario.setCriadoEm(Instant.now());
        return usuario;
    }
}

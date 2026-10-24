package com.agendafacil.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agendafacil.backend.dto.LoginRequest;
import com.agendafacil.backend.dto.RegisterRequest;
import com.agendafacil.backend.entity.Jornada;
import com.agendafacil.backend.entity.Profissional;
import com.agendafacil.backend.entity.Servico;
import com.agendafacil.backend.repository.ProfissionalRepository;
import com.agendafacil.backend.repository.ServicoRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

// Note: the DB-level conflict guarantee itself (the exclusion constraint rejecting
// an overlapping insert even when application code is bypassed) is proven in
// AgendamentoRepositoryTest. This test proves the HTTP-facing behavior is correct
// in the normal request flow: book once, then the same/overlapping slot 409s cleanly.
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgendamentoControllerCreateTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private ProfissionalRepository profissionalRepository;

    private static final LocalDate PROXIMA_SEGUNDA =
            LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

    @Test
    void createReturns401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/agendamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"profissionalId": 1, "servicoId": 1, "inicio": "2026-01-05T09:00:00"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void booksAnOpenSlotAndReturns201WithSnapshottedPrice() throws Exception {
        Servico servico = servicoRepository.save(novoServico(new BigDecimal("75.00")));
        Profissional profissional = profissionalRepository.save(
                novoProfissionalComJornada(servico, LocalTime.of(9, 0), LocalTime.of(12, 0)));
        String token = registerAndLogin("cliente-book1@example.com");

        String body = requestBody(profissional.getId(), servico.getId(), LocalDateTime.of(PROXIMA_SEGUNDA, LocalTime.of(9, 0)));

        mockMvc.perform(post("/api/agendamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGENDADO"))
                .andExpect(jsonPath("$.precoCobrado").value(75.00))
                .andExpect(jsonPath("$.profissionalId").value(profissional.getId()));
    }

    @Test
    void secondOverlappingBookingForSameProfissionalReturns409() throws Exception {
        Servico servico = servicoRepository.save(novoServico(new BigDecimal("50.00")));
        Profissional profissional = profissionalRepository.save(
                novoProfissionalComJornada(servico, LocalTime.of(9, 0), LocalTime.of(12, 0)));
        String token = registerAndLogin("cliente-book2@example.com");

        LocalDateTime inicio = LocalDateTime.of(PROXIMA_SEGUNDA, LocalTime.of(10, 0));
        String body = requestBody(profissional.getId(), servico.getId(), inicio);

        mockMvc.perform(post("/api/agendamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/agendamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void backToBackBookingsDoNotFalselyConflict() throws Exception {
        Servico servico = servicoRepository.save(novoServico(new BigDecimal("50.00")));
        Profissional profissional = profissionalRepository.save(
                novoProfissionalComJornada(servico, LocalTime.of(9, 0), LocalTime.of(12, 0)));
        String token = registerAndLogin("cliente-book3@example.com");

        String primeiro = requestBody(profissional.getId(), servico.getId(),
                LocalDateTime.of(PROXIMA_SEGUNDA, LocalTime.of(9, 0)));
        String segundo = requestBody(profissional.getId(), servico.getId(),
                LocalDateTime.of(PROXIMA_SEGUNDA, LocalTime.of(9, 30)));

        mockMvc.perform(post("/api/agendamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(primeiro))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/agendamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(segundo))
                .andExpect(status().isCreated());
    }

    @Test
    void bookingOutsideJornadaReturns400() throws Exception {
        Servico servico = servicoRepository.save(novoServico(new BigDecimal("50.00")));
        Profissional profissional = profissionalRepository.save(
                novoProfissionalComJornada(servico, LocalTime.of(9, 0), LocalTime.of(12, 0)));
        String token = registerAndLogin("cliente-book4@example.com");

        String body = requestBody(profissional.getId(), servico.getId(),
                LocalDateTime.of(PROXIMA_SEGUNDA, LocalTime.of(20, 0)));

        mockMvc.perform(post("/api/agendamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    private String requestBody(Long profissionalId, Long servicoId, LocalDateTime inicio) {
        return """
                {"profissionalId": %d, "servicoId": %d, "inicio": "%s"}
                """.formatted(profissionalId, servicoId, inicio);
    }

    private String registerAndLogin(String email) throws Exception {
        String registerBody = objectMapper.writeValueAsString(new RegisterRequest("Teste", email, "senha123"));
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(new LoginRequest(email, "senha123"));
        String loginResponse = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(loginResponse).get("accessToken").asString();
    }

    private Servico novoServico(BigDecimal preco) {
        Servico servico = new Servico();
        servico.setNome("Corte Teste Criar Agendamento");
        servico.setDescricao("desc");
        servico.setDuracaoMin(30);
        servico.setPreco(preco);
        servico.setAtivo(true);
        return servico;
    }

    private Profissional novoProfissionalComJornada(Servico servico, LocalTime inicio, LocalTime fim) {
        Profissional profissional = new Profissional();
        profissional.setNome("Profissional Teste Criar Agendamento");
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
}

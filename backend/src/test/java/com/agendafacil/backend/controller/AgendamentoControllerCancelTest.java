package com.agendafacil.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgendamentoControllerCancelTest {

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

    @Test
    void cancelsMoreThanTwoHoursBeforeStart() throws Exception {
        Profissional profissional = profissionalWithFullDayJornada();
        String clienteToken = registerAndLogin("cliente-cancel1@example.com");
        long id = book(clienteToken, profissional, LocalDateTime.now().plusHours(3).withMinute(0).withSecond(0).withNano(0));

        mockMvc.perform(patch("/api/agendamentos/" + id + "/cancelar").header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
    }

    @Test
    void rejectsCancelInsideTwoHourWindow() throws Exception {
        Profissional profissional = profissionalWithFullDayJornada();
        String clienteToken = registerAndLogin("cliente-cancel2@example.com");
        long id = book(clienteToken, profissional, LocalDateTime.now().plusHours(1).withSecond(0).withNano(0));

        mockMvc.perform(patch("/api/agendamentos/" + id + "/cancelar").header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anotherClienteCannotCancelSomeoneElsesBooking() throws Exception {
        Profissional profissional = profissionalWithFullDayJornada();
        String donoToken = registerAndLogin("cliente-cancel3-dono@example.com");
        long id = book(donoToken, profissional, LocalDateTime.now().plusHours(3).withMinute(0).withSecond(0).withNano(0));

        String outroToken = registerAndLogin("cliente-cancel3-outro@example.com");

        mockMvc.perform(patch("/api/agendamentos/" + id + "/cancelar").header("Authorization", "Bearer " + outroToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCancelAnyonesBooking() throws Exception {
        Profissional profissional = profissionalWithFullDayJornada();
        String clienteToken = registerAndLogin("cliente-cancel4@example.com");
        long id = book(clienteToken, profissional, LocalDateTime.now().plusHours(3).withMinute(0).withSecond(0).withNano(0));

        String loginBody = objectMapper.writeValueAsString(new LoginRequest("admin@agendafacil.com", "AdminAgenda123!"));
        String loginResponse = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String adminToken = objectMapper.readTree(loginResponse).get("accessToken").asString();

        mockMvc.perform(patch("/api/agendamentos/" + id + "/cancelar").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
    }

    @Test
    void cannotCancelAnAlreadyCancelledBooking() throws Exception {
        Profissional profissional = profissionalWithFullDayJornada();
        String clienteToken = registerAndLogin("cliente-cancel5@example.com");
        long id = book(clienteToken, profissional, LocalDateTime.now().plusHours(3).withMinute(0).withSecond(0).withNano(0));

        mockMvc.perform(patch("/api/agendamentos/" + id + "/cancelar").header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/agendamentos/" + id + "/cancelar").header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isBadRequest());
    }

    private long book(String token, Profissional profissional, LocalDateTime inicio) throws Exception {
        String body = """
                {"profissionalId": %d, "servicoId": %d, "inicio": "%s"}
                """.formatted(profissional.getId(), profissional.getServicos().iterator().next().getId(),
                inicio.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        String response = mockMvc.perform(post("/api/agendamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private Profissional profissionalWithFullDayJornada() {
        Servico servico = servicoRepository.save(novoServico());
        Profissional profissional = new Profissional();
        profissional.setNome("Profissional Teste Cancelamento");
        profissional.setAtivo(true);
        profissional.setServicos(Set.of(servico));

        List<Jornada> jornadas = List.of(DayOfWeek.values()).stream()
                .map(dia -> {
                    Jornada j = new Jornada();
                    j.setProfissional(profissional);
                    j.setDiaSemana(dia);
                    j.setHoraInicio(LocalTime.of(0, 0));
                    j.setHoraFim(LocalTime.of(23, 30));
                    return j;
                })
                .toList();
        profissional.setJornada(jornadas);

        return profissionalRepository.save(profissional);
    }

    private Servico novoServico() {
        Servico servico = new Servico();
        servico.setNome("Corte Teste Cancelamento");
        servico.setDescricao("desc");
        servico.setDuracaoMin(30);
        servico.setPreco(new BigDecimal("50.00"));
        servico.setAtivo(true);
        return servico;
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
}

package com.agendafacil.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class AgendamentoControllerMeusTest {

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
    void meusReturns401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/agendamentos/meus")).andExpect(status().isUnauthorized());
    }

    @Test
    void meusReturnsOnlyTheCallersOwnBookings() throws Exception {
        Profissional profissional = profissionalWithFullDayJornada();
        String tokenA = registerAndLogin("cliente-meus-a@example.com");
        String tokenB = registerAndLogin("cliente-meus-b@example.com");

        book(tokenA, profissional, LocalDateTime.now().plusHours(3).withMinute(0).withSecond(0).withNano(0));
        book(tokenB, profissional, LocalDateTime.now().plusHours(4).withMinute(0).withSecond(0).withNano(0));

        mockMvc.perform(get("/api/agendamentos/meus").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    private void book(String token, Profissional profissional, LocalDateTime inicio) throws Exception {
        String body = """
                {"profissionalId": %d, "servicoId": %d, "inicio": "%s"}
                """.formatted(profissional.getId(), profissional.getServicos().iterator().next().getId(),
                inicio.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        mockMvc.perform(post("/api/agendamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private Profissional profissionalWithFullDayJornada() {
        Servico servico = servicoRepository.save(novoServico());
        Profissional profissional = new Profissional();
        profissional.setNome("Profissional Teste Meus");
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
        servico.setNome("Corte Teste Meus");
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

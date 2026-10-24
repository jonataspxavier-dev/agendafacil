package com.agendafacil.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agendafacil.backend.dto.LoginRequest;
import com.agendafacil.backend.dto.RegisterRequest;
import com.agendafacil.backend.entity.Servico;
import com.agendafacil.backend.repository.ServicoRepository;
import java.math.BigDecimal;
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
class ProfissionalControllerWriteTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ServicoRepository servicoRepository;

    @Test
    void createReturns401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/profissionais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Karla"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReturns403WithClienteToken() throws Exception {
        String clienteToken = registerAndLogin("cliente-profissional@example.com");

        mockMvc.perform(post("/api/profissionais")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Karla"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void fullCrudCycleWithServicosAndJornadaReplace() throws Exception {
        Servico corte = servicoRepository.save(novoServico("Corte"));
        Servico barba = servicoRepository.save(novoServico("Barba"));
        String adminToken = adminToken();

        String createBody = """
                {
                  "nome": "Lucas",
                  "servicoIds": [%d, %d],
                  "jornada": [
                    {"diaSemana": "MONDAY", "horaInicio": "09:00", "horaFim": "18:00"},
                    {"diaSemana": "TUESDAY", "horaInicio": "09:00", "horaFim": "18:00"},
                    {"diaSemana": "WEDNESDAY", "horaInicio": "09:00", "horaFim": "18:00"}
                  ]
                }
                """.formatted(corte.getId(), barba.getId());

        String createResponse = mockMvc.perform(post("/api/profissionais")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Lucas"))
                .andExpect(jsonPath("$.servicoIds.length()").value(2))
                .andExpect(jsonPath("$.jornada.length()").value(3))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = objectMapper.readTree(createResponse).get("id").asLong();

        String updateBody = """
                {
                  "nome": "Lucas Silva",
                  "servicoIds": [%d],
                  "jornada": [
                    {"diaSemana": "SATURDAY", "horaInicio": "10:00", "horaFim": "14:00"}
                  ]
                }
                """.formatted(corte.getId());

        mockMvc.perform(put("/api/profissionais/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Lucas Silva"))
                .andExpect(jsonPath("$.servicoIds.length()").value(1))
                .andExpect(jsonPath("$.jornada.length()").value(1))
                .andExpect(jsonPath("$.jornada[0].diaSemana").value("SATURDAY"));

        mockMvc.perform(delete("/api/profissionais/" + id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/profissionais"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").isEmpty());
    }

    @Test
    void createWithUnknownServicoIdReturns400() throws Exception {
        String adminToken = adminToken();

        mockMvc.perform(post("/api/profissionais")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Marcos", "servicoIds": [999999]}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateNonexistentReturns404() throws Exception {
        String adminToken = adminToken();

        mockMvc.perform(put("/api/profissionais/999999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Não existe"}"""))
                .andExpect(status().isNotFound());
    }

    private String adminToken() throws Exception {
        String loginBody = objectMapper.writeValueAsString(new LoginRequest("admin@agendafacil.com", "AdminAgenda123!"));
        String loginResponse = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(loginResponse).get("accessToken").asString();
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

    private Servico novoServico(String nome) {
        Servico servico = new Servico();
        servico.setNome(nome);
        servico.setDescricao("desc");
        servico.setDuracaoMin(30);
        servico.setPreco(new BigDecimal("40.00"));
        servico.setAtivo(true);
        return servico;
    }
}

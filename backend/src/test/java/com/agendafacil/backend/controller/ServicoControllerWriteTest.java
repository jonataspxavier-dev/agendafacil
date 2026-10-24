package com.agendafacil.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agendafacil.backend.dto.LoginRequest;
import com.agendafacil.backend.dto.RegisterRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ServicoControllerWriteTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createReturns401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/servicos").contentType(MediaType.APPLICATION_JSON).content(servicoBody("Corte")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReturns403WithClienteToken() throws Exception {
        String clienteToken = registerAndLogin("cliente-servico@example.com");

        mockMvc.perform(post("/api/servicos")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(servicoBody("Corte")))
                .andExpect(status().isForbidden());
    }

    @Test
    void fullCrudCycleAsAdmin() throws Exception {
        String adminToken = adminToken();

        String createResponse = mockMvc.perform(post("/api/servicos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(servicoBody("Depilação")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Depilação"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(put("/api/servicos/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(servicoBody("Depilação a Laser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Depilação a Laser"));

        mockMvc.perform(delete("/api/servicos/" + id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/servicos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").isEmpty());
    }

    @Test
    void updateNonexistentReturns404() throws Exception {
        String adminToken = adminToken();

        mockMvc.perform(put("/api/servicos/999999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(servicoBody("Não existe")))
                .andExpect(status().isNotFound());
    }

    @Test
    void createWithBlankNomeReturns400() throws Exception {
        String adminToken = adminToken();

        mockMvc.perform(post("/api/servicos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(servicoBody("")))
                .andExpect(status().isBadRequest());
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

    private String servicoBody(String nome) {
        return """
                {"nome": "%s", "descricao": "desc", "duracaoMin": 30, "preco": 50.00}
                """.formatted(nome);
    }
}

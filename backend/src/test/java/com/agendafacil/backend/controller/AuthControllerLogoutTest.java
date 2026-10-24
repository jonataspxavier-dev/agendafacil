package com.agendafacil.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agendafacil.backend.dto.LoginRequest;
import com.agendafacil.backend.dto.RefreshRequest;
import com.agendafacil.backend.dto.RegisterRequest;
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
import tools.jackson.databind.ObjectMapper;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerLogoutTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void logoutRevokesTheRefreshTokenSoItCannotBeUsedAgain() throws Exception {
        LoginResult login = registerAndLogin("julia@example.com");

        String logoutBody = objectMapper.writeValueAsString(new RefreshRequest(login.refreshToken()));
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + login.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutBody))
                .andExpect(status().isNoContent());

        String refreshBody = objectMapper.writeValueAsString(new RefreshRequest(login.refreshToken()));
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutWithoutAccessTokenReturns401() throws Exception {
        String body = objectMapper.writeValueAsString(new RefreshRequest("qualquer-token"));
        mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutCannotRevokeAnotherUsersRefreshToken() throws Exception {
        LoginResult vitima = registerAndLogin("vitima@example.com");
        LoginResult atacante = registerAndLogin("atacante@example.com");

        String logoutBody = objectMapper.writeValueAsString(new RefreshRequest(vitima.refreshToken()));
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + atacante.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutBody))
                .andExpect(status().isNoContent());

        String refreshBody = objectMapper.writeValueAsString(new RefreshRequest(vitima.refreshToken()));
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody))
                .andExpect(status().isOk());
    }

    private LoginResult registerAndLogin(String email) throws Exception {
        String registerBody = objectMapper.writeValueAsString(new RegisterRequest("Teste", email, "senha123"));
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(new LoginRequest(email, "senha123"));
        String loginResponse = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = objectMapper.readTree(loginResponse).get("accessToken").asString();
        String refreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asString();
        return new LoginResult(accessToken, refreshToken);
    }

    private record LoginResult(String accessToken, String refreshToken) {
    }
}

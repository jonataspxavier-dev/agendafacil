package com.agendafacil.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agendafacil.backend.dto.LoginRequest;
import com.agendafacil.backend.entity.Perfil;
import com.agendafacil.backend.entity.Usuario;
import com.agendafacil.backend.repository.UsuarioRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerLoginTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedUser() {
        if (usuarioRepository.findByEmail("login@example.com").isPresent()) {
            return;
        }
        Usuario usuario = new Usuario();
        usuario.setNome("Login Teste");
        usuario.setEmail("login@example.com");
        usuario.setSenhaHash(passwordEncoder.encode("senhaCerta"));
        usuario.setPerfil(Perfil.CLIENTE);
        usuario.setCriadoEm(Instant.now());
        usuarioRepository.save(usuario);
    }

    @Test
    void loginWithCorrectPasswordReturns200WithAccessToken() throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest("login@example.com", "senhaCerta"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresInSeconds").value(15 * 60));
    }

    @Test
    void accessTokenHasExpectedClaims() throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest("login@example.com", "senhaCerta"));

        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = objectMapper.readTree(response).get("accessToken").asString();
        String[] parts = accessToken.split("\\.");
        assertThat(parts).hasSize(3);
        String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        assertThat(payloadJson).contains("\"email\":\"login@example.com\"");
        assertThat(payloadJson).contains("\"role\":\"CLIENTE\"");
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest("login@example.com", "senhaErrada"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void loginWithUnknownEmailReturns401() throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest("naoexiste@example.com", "qualquer123"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }
}

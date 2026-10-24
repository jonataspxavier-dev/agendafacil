package com.agendafacil.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agendafacil.backend.dto.LoginRequest;
import com.agendafacil.backend.dto.RefreshRequest;
import com.agendafacil.backend.dto.RegisterRequest;
import com.agendafacil.backend.entity.RefreshToken;
import com.agendafacil.backend.entity.Usuario;
import com.agendafacil.backend.repository.RefreshTokenRepository;
import com.agendafacil.backend.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class AuthControllerRefreshTest {

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
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void refreshWithValidTokenReturnsNewTokenPair() throws Exception {
        String refreshToken = registerAndLoginReturningRefreshToken("fernanda@example.com");

        String body = objectMapper.writeValueAsString(new RefreshRequest(refreshToken));
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void refreshingRevokesTheOldTokenSoItCannotBeReused() throws Exception {
        String refreshToken = registerAndLoginReturningRefreshToken("gustavo@example.com");
        String body = objectMapper.writeValueAsString(new RefreshRequest(refreshToken));

        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithExpiredTokenReturns401() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setNome("Helena Expirada");
        usuario.setEmail("helena@example.com");
        usuario.setSenhaHash("hash-fake");
        usuario.setPerfil(com.agendafacil.backend.entity.Perfil.CLIENTE);
        usuario.setCriadoEm(Instant.now());
        usuarioRepository.save(usuario);

        String rawToken = "raw-token-expirado";
        RefreshToken expired = new RefreshToken();
        expired.setUsuario(usuario);
        expired.setTokenHash(sha256Hex(rawToken));
        expired.setExpiraEm(Instant.now().minus(1, ChronoUnit.DAYS));
        expired.setRevogado(false);
        expired.setCriadoEm(Instant.now());
        refreshTokenRepository.save(expired);

        String body = objectMapper.writeValueAsString(new RefreshRequest(rawToken));
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithUnknownTokenReturns401() throws Exception {
        String body = objectMapper.writeValueAsString(new RefreshRequest("token-que-nao-existe"));
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    private String registerAndLoginReturningRefreshToken(String email) throws Exception {
        String registerBody = objectMapper.writeValueAsString(new RegisterRequest("Teste", email, "senha123"));
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(new LoginRequest(email, "senha123"));
        String loginResponse = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(loginResponse).get("refreshToken").asString();
    }

    private static String sha256Hex(String raw) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

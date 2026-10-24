package com.agendafacil.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.agendafacil.backend.entity.Perfil;
import com.agendafacil.backend.entity.RefreshToken;
import com.agendafacil.backend.entity.Usuario;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RefreshTokenRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void savesAndFindsByTokenHash() {
        Usuario usuario = usuarioRepository.save(novoUsuario());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUsuario(usuario);
        refreshToken.setTokenHash("hash-abc123");
        refreshToken.setExpiraEm(Instant.now().plus(7, ChronoUnit.DAYS));
        refreshToken.setRevogado(false);
        refreshToken.setCriadoEm(Instant.now());

        refreshTokenRepository.save(refreshToken);

        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash("hash-abc123");

        assertThat(found).isPresent();
        assertThat(found.get().getUsuario().getEmail()).isEqualTo("refresh@example.com");
        assertThat(found.get().isRevogado()).isFalse();
    }

    @Test
    void returnsEmptyWhenTokenHashNotFound() {
        assertThat(refreshTokenRepository.findByTokenHash("nao-existe")).isEmpty();
    }

    private Usuario novoUsuario() {
        Usuario usuario = new Usuario();
        usuario.setNome("Refresh Teste");
        usuario.setEmail("refresh@example.com");
        usuario.setSenhaHash("hash-fake");
        usuario.setPerfil(Perfil.CLIENTE);
        usuario.setCriadoEm(Instant.now());
        return usuario;
    }
}

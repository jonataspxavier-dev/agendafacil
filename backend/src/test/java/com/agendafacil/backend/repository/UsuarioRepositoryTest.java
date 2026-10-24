package com.agendafacil.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.agendafacil.backend.entity.Perfil;
import com.agendafacil.backend.entity.Usuario;
import java.time.Instant;
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
class UsuarioRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void savesAndFindsByEmail() {
        Usuario usuario = new Usuario();
        usuario.setNome("Ana Souza");
        usuario.setEmail("ana@example.com");
        usuario.setSenhaHash("hash-fake");
        usuario.setPerfil(Perfil.CLIENTE);
        usuario.setCriadoEm(Instant.now());

        usuarioRepository.save(usuario);

        Optional<Usuario> found = usuarioRepository.findByEmail("ana@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getNome()).isEqualTo("Ana Souza");
        assertThat(found.get().getPerfil()).isEqualTo(Perfil.CLIENTE);
    }

    @Test
    void returnsEmptyWhenEmailNotFound() {
        assertThat(usuarioRepository.findByEmail("naoexiste@example.com")).isEmpty();
    }
}

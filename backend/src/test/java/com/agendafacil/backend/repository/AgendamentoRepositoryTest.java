package com.agendafacil.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agendafacil.backend.entity.Agendamento;
import com.agendafacil.backend.entity.Perfil;
import com.agendafacil.backend.entity.Profissional;
import com.agendafacil.backend.entity.Servico;
import com.agendafacil.backend.entity.StatusAgendamento;
import com.agendafacil.backend.entity.Usuario;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AgendamentoRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProfissionalRepository profissionalRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Test
    void preventsOverlappingAgendadoBookingsForSameProfissional() {
        Usuario cliente = usuarioRepository.save(novoCliente());
        Servico servico = servicoRepository.save(novoServico());
        Profissional profissional = profissionalRepository.save(novoProfissional());

        Agendamento a1 = novoAgendamento(cliente, profissional, servico,
                LocalDateTime.of(2026, 1, 5, 9, 0), LocalDateTime.of(2026, 1, 5, 9, 30));
        agendamentoRepository.saveAndFlush(a1);

        Agendamento a2 = novoAgendamento(cliente, profissional, servico,
                LocalDateTime.of(2026, 1, 5, 9, 15), LocalDateTime.of(2026, 1, 5, 9, 45));

        assertThatThrownBy(() -> agendamentoRepository.saveAndFlush(a2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsBackToBackBookingsThatDoNotOverlap() {
        Usuario cliente = usuarioRepository.save(novoCliente());
        Servico servico = servicoRepository.save(novoServico());
        Profissional profissional = profissionalRepository.save(novoProfissional());

        Agendamento a1 = novoAgendamento(cliente, profissional, servico,
                LocalDateTime.of(2026, 1, 5, 9, 0), LocalDateTime.of(2026, 1, 5, 9, 30));
        agendamentoRepository.saveAndFlush(a1);

        Agendamento a2 = novoAgendamento(cliente, profissional, servico,
                LocalDateTime.of(2026, 1, 5, 9, 30), LocalDateTime.of(2026, 1, 5, 10, 0));
        agendamentoRepository.saveAndFlush(a2);

        assertThat(agendamentoRepository.findAll()).hasSize(2);
    }

    @Test
    void allowsOverlappingBookingAfterTheFirstOneIsCancelled() {
        Usuario cliente = usuarioRepository.save(novoCliente());
        Servico servico = servicoRepository.save(novoServico());
        Profissional profissional = profissionalRepository.save(novoProfissional());

        Agendamento a1 = novoAgendamento(cliente, profissional, servico,
                LocalDateTime.of(2026, 1, 5, 9, 0), LocalDateTime.of(2026, 1, 5, 9, 30));
        a1 = agendamentoRepository.saveAndFlush(a1);
        a1.setStatus(StatusAgendamento.CANCELADO);
        agendamentoRepository.saveAndFlush(a1);

        Agendamento a2 = novoAgendamento(cliente, profissional, servico,
                LocalDateTime.of(2026, 1, 5, 9, 0), LocalDateTime.of(2026, 1, 5, 9, 30));
        agendamentoRepository.saveAndFlush(a2);

        assertThat(agendamentoRepository.findAll()).hasSize(2);
    }

    private Agendamento novoAgendamento(Usuario cliente, Profissional profissional, Servico servico,
            LocalDateTime inicio, LocalDateTime fim) {
        Agendamento agendamento = new Agendamento();
        agendamento.setCliente(cliente);
        agendamento.setProfissional(profissional);
        agendamento.setServico(servico);
        agendamento.setInicio(inicio);
        agendamento.setFim(fim);
        agendamento.setStatus(StatusAgendamento.AGENDADO);
        agendamento.setPrecoCobrado(new BigDecimal("50.00"));
        agendamento.setCriadoEm(LocalDateTime.now());
        return agendamento;
    }

    private Usuario novoCliente() {
        Usuario usuario = new Usuario();
        usuario.setNome("Cliente Teste");
        usuario.setEmail("cliente-agendamento-" + System.nanoTime() + "@example.com");
        usuario.setSenhaHash("hash-fake");
        usuario.setPerfil(Perfil.CLIENTE);
        usuario.setCriadoEm(java.time.Instant.now());
        return usuario;
    }

    private Servico novoServico() {
        Servico servico = new Servico();
        servico.setNome("Corte");
        servico.setDescricao("desc");
        servico.setDuracaoMin(30);
        servico.setPreco(new BigDecimal("50.00"));
        servico.setAtivo(true);
        return servico;
    }

    private Profissional novoProfissional() {
        Profissional profissional = new Profissional();
        profissional.setNome("Profissional Teste");
        profissional.setAtivo(true);
        return profissional;
    }
}

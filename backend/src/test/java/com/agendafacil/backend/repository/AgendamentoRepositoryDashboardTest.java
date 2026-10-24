package com.agendafacil.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.agendafacil.backend.dto.TopServicoResponse;
import com.agendafacil.backend.entity.Agendamento;
import com.agendafacil.backend.entity.Perfil;
import com.agendafacil.backend.entity.Profissional;
import com.agendafacil.backend.entity.Servico;
import com.agendafacil.backend.entity.StatusAgendamento;
import com.agendafacil.backend.entity.Usuario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AgendamentoRepositoryDashboardTest {

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
    void countsOnlyAgendadoAndConcluidoBookingsWithinTheGivenRange() {
        Usuario cliente = usuarioRepository.save(novoCliente());
        Servico servico = servicoRepository.save(novoServico("Corte Dashboard Count"));

        LocalDateTime inicioHoje = LocalDate.now().atStartOfDay();
        LocalDateTime inicioAmanha = inicioHoje.plusDays(1);

        salvar(cliente, servico, StatusAgendamento.AGENDADO, inicioHoje.plusHours(9));
        salvar(cliente, servico, StatusAgendamento.CONCLUIDO, inicioHoje.plusHours(10));
        salvar(cliente, servico, StatusAgendamento.CANCELADO, inicioHoje.plusHours(11));
        salvar(cliente, servico, StatusAgendamento.AGENDADO, inicioHoje.minusHours(1));

        long total = agendamentoRepository.countByInicioBetweenAndStatusIn(
                inicioHoje, inicioAmanha, List.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONCLUIDO));

        assertThat(total).isEqualTo(2);
    }

    @Test
    void sumsPrecoCobradoOnlyForConcluidoWithinTheGivenRange() {
        Usuario cliente = usuarioRepository.save(novoCliente());
        Servico servico = servicoRepository.save(novoServico("Corte Dashboard Soma"));

        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime inicioMesSeguinte = inicioMes.plusMonths(1);
        LocalDateTime mesPassado = inicioMes.minusDays(1);

        salvar(cliente, servico, StatusAgendamento.CONCLUIDO, inicioMes.plusDays(2), new BigDecimal("100.00"));
        salvar(cliente, servico, StatusAgendamento.AGENDADO, inicioMes.plusDays(3), new BigDecimal("999.00"));
        salvar(cliente, servico, StatusAgendamento.CONCLUIDO, mesPassado, new BigDecimal("999.00"));

        BigDecimal soma = agendamentoRepository.somarFaturamento(inicioMes, inicioMesSeguinte, StatusAgendamento.CONCLUIDO);

        assertThat(soma).isEqualByComparingTo("100.00");
    }

    @Test
    void sumsZeroWhenNothingMatches() {
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        BigDecimal soma = agendamentoRepository.somarFaturamento(
                inicioMes, inicioMes.plusMonths(1), StatusAgendamento.CONCLUIDO);

        assertThat(soma).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void ranksTopServicosByConcluidoCountDescending() {
        Usuario cliente = usuarioRepository.save(novoCliente());
        Servico servicoA = servicoRepository.save(novoServico("Corte Dashboard Top A"));
        Servico servicoB = servicoRepository.save(novoServico("Corte Dashboard Top B"));
        Servico servicoC = servicoRepository.save(novoServico("Corte Dashboard Top C"));

        LocalDateTime base = LocalDateTime.of(2026, 3, 10, 9, 0);

        salvar(cliente, servicoA, StatusAgendamento.CONCLUIDO, base);
        salvar(cliente, servicoA, StatusAgendamento.CONCLUIDO, base.plusHours(1));
        salvar(cliente, servicoA, StatusAgendamento.CONCLUIDO, base.plusHours(2));
        salvar(cliente, servicoB, StatusAgendamento.CONCLUIDO, base.plusHours(3));
        salvar(cliente, servicoC, StatusAgendamento.AGENDADO, base.plusHours(4));
        salvar(cliente, servicoC, StatusAgendamento.AGENDADO, base.plusHours(5));

        List<TopServicoResponse> top = agendamentoRepository.buscarTopServicos(
                StatusAgendamento.CONCLUIDO, PageRequest.of(0, 5));

        assertThat(top).extracting(TopServicoResponse::servicoId)
                .containsExactly(servicoA.getId(), servicoB.getId());
        assertThat(top.get(0).quantidade()).isEqualTo(3);
        assertThat(top.get(1).quantidade()).isEqualTo(1);
    }

    @Test
    void capsTopServicosAtTheGivenLimit() {
        Usuario cliente = usuarioRepository.save(novoCliente());
        Servico servicoA = servicoRepository.save(novoServico("Corte Dashboard Limit A"));
        Servico servicoB = servicoRepository.save(novoServico("Corte Dashboard Limit B"));

        LocalDateTime base = LocalDateTime.of(2026, 4, 10, 9, 0);

        salvar(cliente, servicoA, StatusAgendamento.CONCLUIDO, base);
        salvar(cliente, servicoB, StatusAgendamento.CONCLUIDO, base.plusHours(1));
        salvar(cliente, servicoB, StatusAgendamento.CONCLUIDO, base.plusHours(2));

        List<TopServicoResponse> top = agendamentoRepository.buscarTopServicos(
                StatusAgendamento.CONCLUIDO, PageRequest.of(0, 1));

        assertThat(top).hasSize(1);
        assertThat(top.get(0).servicoId()).isEqualTo(servicoB.getId());
    }

    private void salvar(Usuario cliente, Servico servico, StatusAgendamento status, LocalDateTime inicio) {
        salvar(cliente, servico, status, inicio, servico.getPreco());
    }

    private void salvar(Usuario cliente, Servico servico, StatusAgendamento status, LocalDateTime inicio, BigDecimal preco) {
        Agendamento agendamento = new Agendamento();
        agendamento.setCliente(cliente);
        agendamento.setProfissional(profissionalRepository.save(novoProfissional()));
        agendamento.setServico(servico);
        agendamento.setInicio(inicio);
        agendamento.setFim(inicio.plusMinutes(servico.getDuracaoMin()));
        agendamento.setStatus(status);
        agendamento.setPrecoCobrado(preco);
        agendamento.setCriadoEm(LocalDateTime.now());
        agendamentoRepository.save(agendamento);
    }

    private Usuario novoCliente() {
        Usuario usuario = new Usuario();
        usuario.setNome("Cliente Dashboard");
        usuario.setEmail("cliente-dashboard-" + System.nanoTime() + "@example.com");
        usuario.setSenhaHash("hash-fake");
        usuario.setPerfil(Perfil.CLIENTE);
        usuario.setCriadoEm(java.time.Instant.now());
        return usuario;
    }

    private Servico novoServico(String nome) {
        Servico servico = new Servico();
        servico.setNome(nome);
        servico.setDescricao("desc");
        servico.setDuracaoMin(30);
        servico.setPreco(new BigDecimal("50.00"));
        servico.setAtivo(true);
        return servico;
    }

    private Profissional novoProfissional() {
        Profissional profissional = new Profissional();
        profissional.setNome("Profissional Dashboard " + System.nanoTime());
        profissional.setAtivo(true);
        return profissional;
    }
}

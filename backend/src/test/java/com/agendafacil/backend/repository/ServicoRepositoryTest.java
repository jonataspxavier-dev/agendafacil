package com.agendafacil.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.agendafacil.backend.entity.Servico;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ServicoRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private ServicoRepository servicoRepository;

    @Test
    void savesAndFindsOnlyActiveServices() {
        servicoRepository.save(novoServico("Corte Teste Repositorio", true));
        servicoRepository.save(novoServico("Serviço descontinuado Teste Repositorio", false));

        // findAllByAtivoTrue also returns catalog's seed data (V8), so assert
        // presence/absence by name instead of an exact page size.
        Page<Servico> ativos = servicoRepository.findAllByAtivoTrue(PageRequest.of(0, 50));

        assertThat(ativos.getContent())
                .extracting(Servico::getNome)
                .contains("Corte Teste Repositorio")
                .doesNotContain("Serviço descontinuado Teste Repositorio");
    }

    private Servico novoServico(String nome, boolean ativo) {
        Servico servico = new Servico();
        servico.setNome(nome);
        servico.setDescricao("Descrição de " + nome);
        servico.setDuracaoMin(30);
        servico.setPreco(new BigDecimal("50.00"));
        servico.setAtivo(ativo);
        return servico;
    }
}

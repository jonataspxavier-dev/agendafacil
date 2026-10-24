package com.agendafacil.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.agendafacil.backend.entity.Jornada;
import com.agendafacil.backend.entity.Profissional;
import com.agendafacil.backend.entity.Servico;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProfissionalRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private ProfissionalRepository profissionalRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesProfissionalWithServicosAndJornada() {
        Servico corte = servicoRepository.save(novoServico("Corte"));
        Servico barba = servicoRepository.save(novoServico("Barba"));

        Profissional profissional = new Profissional();
        profissional.setNome("Fernanda");
        profissional.setAtivo(true);
        profissional.setServicos(Set.of(corte, barba));

        Jornada segunda = new Jornada();
        segunda.setProfissional(profissional);
        segunda.setDiaSemana(DayOfWeek.MONDAY);
        segunda.setHoraInicio(LocalTime.of(9, 0));
        segunda.setHoraFim(LocalTime.of(18, 0));
        profissional.setJornada(List.of(segunda));

        profissionalRepository.save(profissional);
        entityManager.flush();
        entityManager.clear();

        Optional<Profissional> found = profissionalRepository.findById(profissional.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getServicos()).hasSize(2);
        assertThat(found.get().getJornada()).hasSize(1);
        assertThat(found.get().getJornada().get(0).getDiaSemana()).isEqualTo(DayOfWeek.MONDAY);
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

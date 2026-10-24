package com.agendafacil.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agendafacil.backend.entity.Jornada;
import com.agendafacil.backend.entity.Profissional;
import com.agendafacil.backend.entity.Servico;
import com.agendafacil.backend.repository.ProfissionalRepository;
import com.agendafacil.backend.repository.ServicoRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProfissionalControllerListTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private ProfissionalRepository profissionalRepository;

    @BeforeEach
    void seed() {
        Servico corte = servicoRepository.save(novoServico("Corte"));

        Profissional ativo = new Profissional();
        ativo.setNome("Fernanda");
        ativo.setAtivo(true);
        ativo.setServicos(Set.of(corte));
        Jornada segunda = new Jornada();
        segunda.setProfissional(ativo);
        segunda.setDiaSemana(DayOfWeek.MONDAY);
        segunda.setHoraInicio(LocalTime.of(9, 0));
        segunda.setHoraFim(LocalTime.of(18, 0));
        ativo.setJornada(List.of(segunda));
        profissionalRepository.save(ativo);

        Profissional inativo = new Profissional();
        inativo.setNome("Inativo");
        inativo.setAtivo(false);
        profissionalRepository.save(inativo);
    }

    @Test
    void listsOnlyActiveProfissionaisWithServicosAndJornadaNoAuth() throws Exception {
        mockMvc.perform(get("/api/profissionais"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Fernanda"))
                .andExpect(jsonPath("$[0].servicoIds.length()").value(1))
                .andExpect(jsonPath("$[0].jornada.length()").value(1))
                .andExpect(jsonPath("$[0].jornada[0].diaSemana").value("MONDAY"));
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

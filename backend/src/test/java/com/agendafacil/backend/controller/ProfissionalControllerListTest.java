package com.agendafacil.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// Note: catalog's seed migration (V8) permanently adds 3 active professionals
// to every test database, so assertions here locate this test's own fixture
// by name via a JSONPath filter instead of assuming index [0] or an exact
// total row count.
// Nota: a migration de seed do catalog (V8) adiciona permanentemente 3
// profissionais ativos em todo banco de teste, então as asserções aqui
// localizam a fixture deste teste pelo nome via filtro JSONPath, em vez de
// assumir o índice [0] ou uma contagem total exata de linhas.
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

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void seed() {
        Servico corte = servicoRepository.save(novoServico("Corte Teste Lista Profissional"));

        Profissional ativo = new Profissional();
        ativo.setNome("Fernanda Teste Lista");
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
        inativo.setNome("Inativo Teste Lista");
        inativo.setAtivo(false);
        profissionalRepository.save(inativo);
    }

    @Test
    void listsOnlyActiveProfissionaisWithServicosAndJornadaNoAuth() throws Exception {
        String body = mockMvc.perform(get("/api/profissionais"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode list = objectMapper.readTree(body);
        JsonNode fernanda = null;
        for (JsonNode item : list) {
            String nome = item.get("nome").asString();
            assertThat(nome).isNotEqualTo("Inativo Teste Lista");
            if ("Fernanda Teste Lista".equals(nome)) {
                fernanda = item;
            }
        }

        assertThat(fernanda).as("Fernanda Teste Lista deve aparecer na listagem pública").isNotNull();
        assertThat(fernanda.get("servicoIds")).hasSize(1);
        assertThat(fernanda.get("jornada")).hasSize(1);
        assertThat(fernanda.get("jornada").get(0).get("diaSemana").asString()).isEqualTo("MONDAY");
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

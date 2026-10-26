package com.agendafacil.backend.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agendafacil.backend.entity.Servico;
import com.agendafacil.backend.repository.ServicoRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// Note: catalog's seed migration (V8) permanently adds 5 active services to
// every test database, so assertions here check for presence/absence of this
// test's own fixtures (by a distinctive name) rather than an exact row count.
// Nota: a migration de seed do catalog (V8) adiciona permanentemente 5
// serviços ativos em todo banco de teste, então as asserções aqui checam a
// presença/ausência das fixtures deste teste (por um nome distintivo), em
// vez de uma contagem exata de linhas.
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ServicoControllerListTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServicoRepository servicoRepository;

    private long baselineAtivos;

    @BeforeEach
    void seed() {
        baselineAtivos = servicoRepository.findAllByAtivoTrue(Pageable.unpaged()).getTotalElements();
        servicoRepository.save(novoServico("Corte Teste Lista", true));
        servicoRepository.save(novoServico("Manicure Teste Lista", true));
        servicoRepository.save(novoServico("Serviço Antigo Teste Lista", false));
    }

    @Test
    void listsOnlyActiveServicesWithNoAuth() throws Exception {
        mockMvc.perform(get("/api/servicos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome", hasItems("Corte Teste Lista", "Manicure Teste Lista")))
                .andExpect(jsonPath("$[*].nome", not(hasItem("Serviço Antigo Teste Lista"))));
    }

    @Test
    void supportsOptionalPaging() throws Exception {
        long expectedTotal = baselineAtivos + 2;

        mockMvc.perform(get("/api/servicos").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(expectedTotal));
    }

    private Servico novoServico(String nome, boolean ativo) {
        Servico servico = new Servico();
        servico.setNome(nome);
        servico.setDescricao("desc");
        servico.setDuracaoMin(30);
        servico.setPreco(new BigDecimal("40.00"));
        servico.setAtivo(ativo);
        return servico;
    }
}

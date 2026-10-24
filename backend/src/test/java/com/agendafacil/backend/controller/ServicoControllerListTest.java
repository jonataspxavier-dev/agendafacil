package com.agendafacil.backend.controller;

import static org.hamcrest.Matchers.hasItems;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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

    @BeforeEach
    void seed() {
        servicoRepository.save(novoServico("Corte", true));
        servicoRepository.save(novoServico("Manicure", true));
        servicoRepository.save(novoServico("Serviço antigo", false));
    }

    @Test
    void listsOnlyActiveServicesWithNoAuth() throws Exception {
        mockMvc.perform(get("/api/servicos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].nome", hasItems("Corte", "Manicure")));
    }

    @Test
    void supportsOptionalPaging() throws Exception {
        mockMvc.perform(get("/api/servicos").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
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

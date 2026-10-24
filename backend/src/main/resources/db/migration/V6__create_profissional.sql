CREATE TABLE profissional (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE profissional_servico (
    profissional_id BIGINT NOT NULL REFERENCES profissional (id),
    servico_id BIGINT NOT NULL REFERENCES servico (id),
    PRIMARY KEY (profissional_id, servico_id)
);

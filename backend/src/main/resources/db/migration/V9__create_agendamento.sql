CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE agendamento (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL REFERENCES usuario (id),
    profissional_id BIGINT NOT NULL REFERENCES profissional (id),
    servico_id BIGINT NOT NULL REFERENCES servico (id),
    inicio TIMESTAMP NOT NULL,
    fim TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    preco_cobrado NUMERIC(10, 2) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT now(),
    periodo tsrange GENERATED ALWAYS AS (tsrange(inicio, fim, '[)')) STORED
);

CREATE INDEX idx_agendamento_profissional_id ON agendamento (profissional_id);
CREATE INDEX idx_agendamento_cliente_id ON agendamento (cliente_id);

-- The actual correctness guarantee: no two AGENDADO rows for the same
-- professional may have overlapping [inicio, fim) ranges, enforced by
-- Postgres itself so it holds even under concurrent requests.
ALTER TABLE agendamento
    ADD CONSTRAINT no_overlapping_agendamentos
    EXCLUDE USING gist (profissional_id WITH =, periodo WITH &&)
    WHERE (status = 'AGENDADO');

CREATE TABLE jornada (
    id BIGSERIAL PRIMARY KEY,
    profissional_id BIGINT NOT NULL REFERENCES profissional (id),
    dia_semana VARCHAR(10) NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fim TIME NOT NULL
);

CREATE INDEX idx_jornada_profissional_id ON jornada (profissional_id);

CREATE TABLE servico (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    descricao VARCHAR(1000),
    duracao_min INTEGER NOT NULL,
    preco NUMERIC(10, 2) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT true
);

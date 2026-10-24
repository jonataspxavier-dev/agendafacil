DO $$
DECLARE
    id_corte BIGINT;
    id_manicure BIGINT;
    id_depilacao BIGINT;
    id_massagem BIGINT;
    id_limpeza BIGINT;
    id_ana BIGINT;
    id_bruno BIGINT;
    id_carla BIGINT;
    dias TEXT[] := ARRAY['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
    dia TEXT;
BEGIN
    INSERT INTO servico (nome, descricao, duracao_min, preco, ativo)
        VALUES ('Corte de Cabelo', 'Corte de cabelo feminino ou masculino', 30, 50.00, true)
        RETURNING id INTO id_corte;
    INSERT INTO servico (nome, descricao, duracao_min, preco, ativo)
        VALUES ('Manicure', 'Cuidado e esmaltação das unhas das mãos', 40, 35.00, true)
        RETURNING id INTO id_manicure;
    INSERT INTO servico (nome, descricao, duracao_min, preco, ativo)
        VALUES ('Depilação', 'Depilação com cera', 45, 60.00, true)
        RETURNING id INTO id_depilacao;
    INSERT INTO servico (nome, descricao, duracao_min, preco, ativo)
        VALUES ('Massagem', 'Massagem relaxante', 60, 120.00, true)
        RETURNING id INTO id_massagem;
    INSERT INTO servico (nome, descricao, duracao_min, preco, ativo)
        VALUES ('Limpeza de Pele', 'Limpeza de pele profunda', 50, 90.00, true)
        RETURNING id INTO id_limpeza;

    INSERT INTO profissional (nome, ativo) VALUES ('Ana Souza', true) RETURNING id INTO id_ana;
    INSERT INTO profissional (nome, ativo) VALUES ('Bruno Lima', true) RETURNING id INTO id_bruno;
    INSERT INTO profissional (nome, ativo) VALUES ('Carla Mendes', true) RETURNING id INTO id_carla;

    INSERT INTO profissional_servico (profissional_id, servico_id) VALUES
        (id_ana, id_corte), (id_ana, id_manicure),
        (id_bruno, id_depilacao), (id_bruno, id_massagem),
        (id_carla, id_limpeza), (id_carla, id_corte);

    FOREACH dia IN ARRAY dias LOOP
        INSERT INTO jornada (profissional_id, dia_semana, hora_inicio, hora_fim) VALUES
            (id_ana, dia, '09:00', '18:00'),
            (id_bruno, dia, '09:00', '18:00'),
            (id_carla, dia, '09:00', '18:00');
    END LOOP;
END $$;

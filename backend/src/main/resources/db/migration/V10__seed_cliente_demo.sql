INSERT INTO usuario (nome, email, senha_hash, perfil, criado_em)
VALUES (
    'Cliente Demo',
    'cliente@agendafacil.com',
    '$2a$10$a0Vytd1IlLeKKdQnRoYUWO1bcSLSGG8q68C3b2QX3BYSyvUmCaWC.',
    'CLIENTE',
    now()
);

INSERT INTO usuario (nome, email, senha_hash, perfil, criado_em)
VALUES (
    'Administrador',
    'admin@agendafacil.com',
    '$2a$10$AlVNJVHtj2CopHWMKzavNOhN2A1ScDY.FNR4hiOst/2EdJSBVbTze',
    'ADMIN',
    now()
);

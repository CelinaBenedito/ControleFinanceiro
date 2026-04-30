INSERT INTO usuario (id, nome, sobrenome, data_nascimento, sexo, imagem, email, senha)
VALUES ('21eb5d2f-3fd8-439e-b647-5cc1f753ae58','Marcio', 'Pereira Costa', '1987-05-16', 'Masculino', null, 'marcinho.pereira@gmail.com', 'todosContra!'),
       ('b5135981-72fc-46ee-a6c6-4072495f7afd','Jonas', 'Pinto', '1956-09-26', 'Masculino', null, 'jonas@gmail.com', 'antigamenteEraMelhor'),
       ('2f1ed17a-c8b6-425b-adb1-b28126be88de','Mariana', 'Rosa da Silva', '2000-12-05', 'Feminino', null, 'am4nd1nh4@gmail.com', 'iLoveBT2'),
       ('8a2f1c3d-5b44-4f9a-9d2a-1c8f7a9e6b11','Carla','Souza Lima','1995-03-22','Feminino',null,'carla.lima95@gmail.com','senhaSegura123'),
       ('d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22','Felipe','Almeida Santos','1982-11-09','Masculino',null,'felipe.almeida82@gmail.com','Felipe@1982'),
       ('3c6f9a8b-7d55-4e2a-9f88-4a7c1b9d6e33','Luciana','Ferreira','1978-07-14','Feminino',null,'luciana.ferreira78@gmail.com','lucyF78'),
       ('a4d8e9c0-2b66-4f3d-9a99-5b8d2c7f4d44','Rafael','Mendes Oliveira','2001-01-30','Masculino',null,'rafa.mendes01@gmail.com','rafa2001'),
       ('f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55','Patrícia','Costa','1990-08-18','Feminino',null,'patricia.costa90@gmail.com','patyC90');

INSERT INTO instituicao(nome)
VALUES ('itau'),
        ('Nubank'),
        ('Santander'),
        ('Bradesco'),
        ('Banco do Brasil'),
        ('Inter'),
        ('Safra'),
        ('Alelo Alimentação'),
        ('Alelo Refeição'),
        ('Aelo Multibeneficios'),
        ('Pluxee'),
        ('Ticket'),
        ('Vale Refeição'),
        ('Vale Alimentação');

INSERT INTO instituicao_usuario(usuario_id, instituicao_id, is_ativo)
VALUES ('21eb5d2f-3fd8-439e-b647-5cc1f753ae58', '13',true),
       ('21eb5d2f-3fd8-439e-b647-5cc1f753ae58','1',true),
       ('21eb5d2f-3fd8-439e-b647-5cc1f753ae58','5',true),
       ('b5135981-72fc-46ee-a6c6-4072495f7afd','2',true),
       ('b5135981-72fc-46ee-a6c6-4072495f7afd','4',true),
       ('b5135981-72fc-46ee-a6c6-4072495f7afd','7',true),
       ('2f1ed17a-c8b6-425b-adb1-b28126be88de','3',true),
       ('2f1ed17a-c8b6-425b-adb1-b28126be88de','6',true),
       ('2f1ed17a-c8b6-425b-adb1-b28126be88de','8',true),
       ('2f1ed17a-c8b6-425b-adb1-b28126be88de','12',true),
       ('8a2f1c3d-5b44-4f9a-9d2a-1c8f7a9e6b11','9',true),
       ('8a2f1c3d-5b44-4f9a-9d2a-1c8f7a9e6b11','14',true),
       ('d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22','1',true),
       ('d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22','2',true),
       ('d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22','3',true),
       ('d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22','4',true),
       ('d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22','5',true),
       ('3c6f9a8b-7d55-4e2a-9f88-4a7c1b9d6e33','6',true),
       ('3c6f9a8b-7d55-4e2a-9f88-4a7c1b9d6e33','10',true),
       ('3c6f9a8b-7d55-4e2a-9f88-4a7c1b9d6e33','11',true),
       ('a4d8e9c0-2b66-4f3d-9a99-5b8d2c7f4d44','7',true),
       ('a4d8e9c0-2b66-4f3d-9a99-5b8d2c7f4d44','12',true),
       ('f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55','8',true),
       ('f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55','9',true),
       ('f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55','13',true),
       ('f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55','14',true);

INSERT INTO categoria(titulo)
VALUES('Roupas'),
      ('Comida'),
      ('Mercado'),
      ('Conta de Água'),
      ('Conta de Luz'),
      ('Transporte'),
      ('Bilhete Unico'),
      ('Salário'),
      ('Carro'),
      ('Recorrente'),
      ('Lazer'),
      ('Jogos'),
      ('Esportes'),
      ('Academia'),
      ('Aplicativo'),
      ('Faculdade'),
      ('Restaurante'),
      ('Praia'),
      ('Outros');

INSERT INTO configuracoes (id, usuario_id, inicio_mes_fiscal, ultima_atualizacao, limite_desejado_mensal)
VALUES
    ('8f1a2c3d-4b5e-678f-9012-3456789abcde', '21eb5d2f-3fd8-439e-b647-5cc1f753ae58', 15, CURRENT_TIMESTAMP, 5000.00),
    ('9a2b3c4d-5e6f-7890-1234-56789abcdef0', 'b5135981-72fc-46ee-a6c6-4072495f7afd', 5,CURRENT_TIMESTAMP, 4000.00),
    ('1b2c3d4e-5f6a-789b-0123-456789abcdef', '2f1ed17a-c8b6-425b-adb1-b28126be88de', 1,CURRENT_TIMESTAMP, 3500.00),
    ('2c3d4e5f-6a7b-890c-1234-56789abcdef1', '8a2f1c3d-5b44-4f9a-9d2a-1c8f7a9e6b11', 20,CURRENT_TIMESTAMP, 3000.00),
    ('3d4e5f6a-7b8c-901d-2345-6789abcdef12', 'd9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22', 15,CURRENT_TIMESTAMP, 6000.00),
    ('4e5f6a7b-8c9d-012e-3456-789abcdef123', '3c6f9a8b-7d55-4e2a-9f88-4a7c1b9d6e33', 5,CURRENT_TIMESTAMP, 4500.00),
    ('5f6a7b8c-9d01-23ef-4567-89abcdef1234', 'a4d8e9c0-2b66-4f3d-9a99-5b8d2c7f4d44', 10,CURRENT_TIMESTAMP, 3200.00),
    ('6a7b8c9d-0123-4f56-789a-bcdef1234567', 'f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55', 1,CURRENT_TIMESTAMP, 3800.00);


INSERT INTO limite_por_instituicao (id, instituicao_usuario_id, limite_desejado, configuracoes_id)
VALUES
    ('7b8c9d0e-1234-567f-890a-bcdef1234567', 1, 2000.00,'8f1a2c3d-4b5e-678f-9012-3456789abcde' ),
    ('8c9d0e1f-2345-6780-901b-cdef12345678', 2, 1500.00, '8f1a2c3d-4b5e-678f-9012-3456789abcde'),
    ('9d0e1f2a-3456-7891-012c-def123456789', 3, 1500.00, '8f1a2c3d-4b5e-678f-9012-3456789abcde'),

    ('0e1f2a3b-4567-8902-123d-ef123456789a', 4, 1200.00, '9a2b3c4d-5e6f-7890-1234-56789abcdef0'),
    ('1f2a3b4c-5678-9013-234e-f123456789ab', 5, 1000.00,'9a2b3c4d-5e6f-7890-1234-56789abcdef0'),
    ('2a3b4c5d-6789-0124-345f-123456789abc', 6, 800.00,'9a2b3c4d-5e6f-7890-1234-56789abcdef0'),

    ('3b4c5d6e-7890-1235-4560-23456789abcd', 7, 1000.00, '1b2c3d4e-5f6a-789b-0123-456789abcdef'),
    ('4c5d6e7f-8901-2346-5671-3456789abcde', 8, 900.00,'1b2c3d4e-5f6a-789b-0123-456789abcdef'),
    ('5d6e7f80-9012-3457-6782-456789abcdef', 9, 700.00,'1b2c3d4e-5f6a-789b-0123-456789abcdef'),
    ('6e7f8091-0123-4568-7893-56789abcdef0', 10, 600.00,'1b2c3d4e-5f6a-789b-0123-456789abcdef'),

    ('7f809102-1234-5679-8904-6789abcdef01', 11, 800.00, '2c3d4e5f-6a7b-890c-1234-56789abcdef1'),
    ('80910213-2345-678a-9015-789abcdef012', 12, 700.00,'2c3d4e5f-6a7b-890c-1234-56789abcdef1'),

    ('91021324-3456-789b-0126-89abcdef0123', 13, 1200.00,'3d4e5f6a-7b8c-901d-2345-6789abcdef12'),
    ('10213245-4567-89ac-1237-9abcdef01234', 14, 1100.00,'3d4e5f6a-7b8c-901d-2345-6789abcdef12'),
    ('21324556-5678-9abd-2348-abcdef012345', 15, 1000.00,'3d4e5f6a-7b8c-901d-2345-6789abcdef12'),
    ('32455667-6789-0ace-3459-bcdef0123456', 16, 900.00,'3d4e5f6a-7b8c-901d-2345-6789abcdef12'),
    ('45566778-7890-1adf-456a-cdef01234567', 17, 800.00,'3d4e5f6a-7b8c-901d-2345-6789abcdef12'),

    ('56677889-8901-2b10-567b-def012345678', 18, 1000.00,'4e5f6a7b-8c9d-012e-3456-789abcdef123'),
    ('67788990-9012-3c21-678c-ef0123456789', 19, 700.00,'4e5f6a7b-8c9d-012e-3456-789abcdef123'),
    ('77899001-0123-4d32-789d-f0123456789a', 20, 600.00,'4e5f6a7b-8c9d-012e-3456-789abcdef123'),

    ('88900112-1234-5e43-890e-0123456789ab', 21, 900.00,'5f6a7b8c-9d01-23ef-4567-89abcdef1234'),
    ('99011223-2345-6f54-901f-123456789abc', 22, 800.00,'5f6a7b8c-9d01-23ef-4567-89abcdef1234'),

    ('00112234-3456-7056-0120-23456789abcd', 23, 1000.00,'6a7b8c9d-0123-4f56-789a-bcdef1234567'),
    ('11223345-4567-8167-1231-3456789abcde', 24, 900.00,'6a7b8c9d-0123-4f56-789a-bcdef1234567'),
    ('22334456-5678-9278-2342-456789abcdef', 25, 800.00,'6a7b8c9d-0123-4f56-789a-bcdef1234567'),
    ('33445567-6789-0389-3453-56789abcdef0', 26, 700.00,'6a7b8c9d-0123-4f56-789a-bcdef1234567');


INSERT INTO categoria_usuario ( usuario_id, categoria_id, is_ativo)
VALUES
    ( '21eb5d2f-3fd8-439e-b647-5cc1f753ae58', 1,true), -- Marcio - Roupas
    ( '21eb5d2f-3fd8-439e-b647-5cc1f753ae58', 2,true), -- Marcio - Comida
    ( '21eb5d2f-3fd8-439e-b647-5cc1f753ae58', 6,true), -- Marcio - Transporte

    ( 'b5135981-72fc-46ee-a6c6-4072495f7afd', 3,true), -- Jonas - Mercado
    ( 'b5135981-72fc-46ee-a6c6-4072495f7afd', 5,true), -- Jonas - Conta de Luz

    ( '2f1ed17a-c8b6-425b-adb1-b28126be88de', 2,true), -- Mariana - Comida
    ( '2f1ed17a-c8b6-425b-adb1-b28126be88de', 4,true), -- Mariana - Conta de Água

    ( '8a2f1c3d-5b44-4f9a-9d2a-1c8f7a9e6b11', 3,true), -- Carla - Mercado
    ('8a2f1c3d-5b44-4f9a-9d2a-1c8f7a9e6b11', 6,true), -- Carla - Transporte

    ( 'd9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22', 1,true), -- Felipe - Roupas
    ('d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22', 7,true), -- Felipe - Bilhete Único

    ( '3c6f9a8b-7d55-4e2a-9f88-4a7c1b9d6e33', 2,true), -- Luciana - Comida
    ('3c6f9a8b-7d55-4e2a-9f88-4a7c1b9d6e33', 4,true), -- Luciana - Conta de Água

    ('a4d8e9c0-2b66-4f3d-9a99-5b8d2c7f4d44', 6,true), -- Rafael - Transporte
    ( 'a4d8e9c0-2b66-4f3d-9a99-5b8d2c7f4d44', 5,true), -- Rafael - Conta de Luz

    ('f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55', 3,true), -- Patrícia - Mercado
    ('f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55', 1,true); -- Patrícia - Roupas


INSERT INTO limite_por_categoria (id, categoria_usuario_id, limite_desejado,configuracoes_id)
VALUES
    ('aaaa1111-bbbb-2222-cccc-333333333333', 1, 800.00, '8f1a2c3d-4b5e-678f-9012-3456789abcde'),
    ('bbbb2222-cccc-3333-dddd-444444444444', 2, 1200.00, '8f1a2c3d-4b5e-678f-9012-3456789abcde'),
    ('cccc3333-dddd-4444-eeee-555555555555', 3, 600.00, '8f1a2c3d-4b5e-678f-9012-3456789abcde'),

    ('dddd4444-eeee-5555-ffff-666666666666', 4, 500.00,'9a2b3c4d-5e6f-7890-1234-56789abcdef0'),
    ('eeee5555-ffff-6666-0000-777777777777', 5, 400.00,'9a2b3c4d-5e6f-7890-1234-56789abcdef0'),

    ('ffff6666-0000-7777-1111-888888888888', 6, 700.00, '1b2c3d4e-5f6a-789b-0123-456789abcdef'),
    ('00007777-1111-8888-2222-999999999999', 7, 600.00, '1b2c3d4e-5f6a-789b-0123-456789abcdef'),

    ('11118888-2222-9999-3333-aaaaaaaaaaaa', 8, 900.00, '2c3d4e5f-6a7b-890c-1234-56789abcdef1'),
    ('22229999-3333-aaaa-4444-bbbbbbbbbbbb', 9, 500.00, '2c3d4e5f-6a7b-890c-1234-56789abcdef1'),

    ('3333aaaa-4444-bbbb-5555-cccccccccccc', 10, 1000.00, '3d4e5f6a-7b8c-901d-2345-6789abcdef12'),
    ('4444bbbb-5555-cccc-6666-dddddddddddd', 11, 800.00, '3d4e5f6a-7b8c-901d-2345-6789abcdef12'),

    ('5555cccc-6666-dddd-7777-eeeeeeeeeeee', 12, 700.00, '4e5f6a7b-8c9d-012e-3456-789abcdef123'),
    ('6666dddd-7777-eeee-8888-ffffffffffff', 13, 600.00, '4e5f6a7b-8c9d-012e-3456-789abcdef123'),

    ('7777eeee-8888-ffff-9999-000000000000', 14, 500.00, '5f6a7b8c-9d01-23ef-4567-89abcdef1234'),
    ('8888ffff-9999-0000-aaaa-111111111111', 15, 400.00,'5f6a7b8c-9d01-23ef-4567-89abcdef1234'),

    ('99990000-aaaa-1111-bbbb-222222222222', 16, 800.00,'6a7b8c9d-0123-4f56-789a-bcdef1234567'),
    ('aaaa1111-bbbb-2222-cccc-333333333334', 17, 700.00,'6a7b8c9d-0123-4f56-789a-bcdef1234567');

-- Márcio recebendo salário
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('9b2e1f4c-8a7d-4c3b-9f12-7d8e5a6b7c90',
        '21eb5d2f-3fd8-439e-b647-5cc1f753ae58',
        'Recebimento',
        5000.00,
        'Recebimento de salário mensal',
        '2026-04-01',
        CURRENT_TIMESTAMP);

INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id , tipo_movimento, valor, parcelas)
VALUES ('9b2e1f4c-8a7d-4c3b-9f12-7d8e5a6b7c90',
        2,
        'Debito',
        5000.00,
        1);

INSERT INTO gasto_detalhe (fk_evento, titulo_gasto)
VALUES ('9b2e1f4c-8a7d-4c3b-9f12-7d8e5a6b7c90',
        'Salário');

INSERT INTO gasto_detalhe_categoria (gasto_detalhe_id, categoria_usuario_id)
VALUES(1,1);


-- Márcio comprando roupas de inverno

INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('44444444-dddd-eeee-ffff-000000000004',
        '21eb5d2f-3fd8-439e-b647-5cc1f753ae58',
        'Gasto',
        350.00,
        'Compra de roupas de inverno',
        '2026-02-15',
        CURRENT_TIMESTAMP);

INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id , tipo_movimento, valor, parcelas)
VALUES ('44444444-dddd-eeee-ffff-000000000004',
        2,
        'Credito',
        350.00,
        1);

INSERT INTO gasto_detalhe (fk_evento, titulo_gasto)
VALUES ('44444444-dddd-eeee-ffff-000000000004',
        'Casaco de lã');

INSERT INTO gasto_detalhe_categoria (gasto_detalhe_id, categoria_usuario_id)
VALUES(2, 1);

-- Márcio - Recarga de bilhete único
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('550e8400-e29b-41d4-a716-446655440000',
        '21eb5d2f-3fd8-439e-b647-5cc1f753ae58',
        'Gasto',
        80.00,
        'Recarga de bilhete único',
        '2026-03-01',
        CURRENT_TIMESTAMP);

INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id , tipo_movimento, valor, parcelas)
VALUES ('550e8400-e29b-41d4-a716-446655440000',
        1,
        'Pix',
        80.00,
        1);

INSERT INTO gasto_detalhe (fk_evento, titulo_gasto)
VALUES ('550e8400-e29b-41d4-a716-446655440000',
        'Transporte público');

INSERT INTO gasto_detalhe_categoria (gasto_detalhe_id, categoria_usuario_id)
VALUES(3,6);

-- Márcio - Jantar em restaurante italiano
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('123e4567-e89b-12d3-a456-426614174000',
        '21eb5d2f-3fd8-439e-b647-5cc1f753ae58',
        'Gasto',
        120.00,
        'Jantar em restaurante italiano',
        '2026-03-20',
        CURRENT_TIMESTAMP);

INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id , tipo_movimento, valor, parcelas)
VALUES ('123e4567-e89b-12d3-a456-426614174000',
        5,
        'Debito',
        120.00,
        1);

INSERT INTO gasto_detalhe (fk_evento, titulo_gasto)
VALUES ('123e4567-e89b-12d3-a456-426614174000',
        'Restaurante');
INSERT INTO gasto_detalhe_categoria (gasto_detalhe_id, categoria_usuario_id)
VALUES(4,2);

-- Jonas comprando carne no mercado
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('11111111-aaaa-bbbb-cccc-000000000001',
        'b5135981-72fc-46ee-a6c6-4072495f7afd',
        'Gasto',
        250.00,
        'Compra de carne no mercado',
        '2026-02-10',
        CURRENT_TIMESTAMP);

INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id , tipo_movimento, valor,parcelas)
VALUES ('11111111-aaaa-bbbb-cccc-000000000001',
        4,
        'Debito',
        250.00,
        1);

INSERT INTO gasto_detalhe (fk_evento, titulo_gasto)
VALUES ('11111111-aaaa-bbbb-cccc-000000000001',
        'Carne bovina');
INSERT INTO gasto_detalhe_categoria (gasto_detalhe_id, categoria_usuario_id)
VALUES(5,3);

-- Mariana pagando conta de água
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('22222222-bbbb-cccc-dddd-000000000002',
        '2f1ed17a-c8b6-425b-adb1-b28126be88de',
        'Gasto',
        120.00,
        'Conta de água mensal',
        '2026-03-05',
        CURRENT_TIMESTAMP);

INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id , tipo_movimento, valor, parcelas)
VALUES ('22222222-bbbb-cccc-dddd-000000000002',
        7,
        'Boleto',
        120.00,
        1);

INSERT INTO gasto_detalhe (fk_evento, titulo_gasto)
VALUES ('22222222-bbbb-cccc-dddd-000000000002',
        'Conta de Água');

INSERT INTO gasto_detalhe_categoria (gasto_detalhe_id, categoria_usuario_id)
VALUES (6, 4);

-- Carla comprando passagem de ônibus
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('33333333-cccc-dddd-eeee-000000000003',
        '8a2f1c3d-5b44-4f9a-9d2a-1c8f7a9e6b11',
        'Gasto',
        50.00,
        'Passagem de ônibus',
        '2026-04-01',
        CURRENT_TIMESTAMP);

INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor,parcelas)
VALUES ('33333333-cccc-dddd-eeee-000000000003',
        11,
        'Pix',
        50.00,
        1);

-- Carla comprando passagem de ônibus
INSERT INTO gasto_detalhe (fk_evento, titulo_gasto)
VALUES ('33333333-cccc-dddd-eeee-000000000003',
        'Transporte público');

INSERT INTO gasto_detalhe_categoria (gasto_detalhe_id, categoria_usuario_id)
VALUES (7, 6);



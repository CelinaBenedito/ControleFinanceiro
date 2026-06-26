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

-- ============================================================
-- OFX Config por instituicao
-- Placeholders substituidos pelo JS via localStorage:
--   {{CPF}}      → CPF do usuario (somente digitos)
--   {{CNPJ}}     → CNPJ da empresa
--   {{SENHA}}    → senha do internet banking
--   {{AGENCIA}}  → numero da agencia
--   {{CONTA}}    → numero da conta
--   {{DIGITO}}   → digito verificador da conta
--   {{TOKEN}}    → codigo do token/MFA (para bancos sem MFA automatico)
--
-- navigation_steps_json = null → modo MANUAL (browser abre, usuario navega sozinho)
-- ofx_supported = true         → banco suporta exportacao OFX
-- ============================================================

-- Itau: MANUAL (iToken push no app)
UPDATE instituicao SET
    ofx_supported = true, login_mode = 'MANUAL', python_endpoint = '/capture',
    bank_url = 'https://www.itau.com.br/', navigation_steps_json = null
WHERE id = 1;

-- Nubank: MANUAL (autentica pelo app - API so para PJ)
UPDATE instituicao SET
    ofx_supported = true, login_mode = 'MANUAL', python_endpoint = '/capture/nubank/sync',
    bank_url = 'https://app.nubank.com.br/', navigation_steps_json = null
WHERE id = 2;

-- Santander: AUTOMATED (sem MFA para a maioria dos usuarios)
UPDATE instituicao SET
    ofx_supported = true, login_mode = 'AUTOMATED', python_endpoint = '/capture',
    bank_url = 'https://www.santander.com.br/',
    navigation_steps_json = '[{"action":"wait_for_selector","selector":"input[id*=cpf]","timeout":15000},{"action":"fill","selector":"input[id*=cpf]","text":"{{CPF}}"},{"action":"click","selector":"button[type=submit]"},{"action":"wait_for_selector","selector":"input[type=password]","timeout":15000},{"action":"fill","selector":"input[type=password]","text":"{{SENHA}}"},{"action":"click","selector":"button[type=submit]"},{"action":"wait_for_selector","selector":"a[href*=extrato]","timeout":30000},{"action":"click","selector":"a[href*=extrato]"},{"action":"wait_for_selector","selector":"a[href*=ofx]","timeout":30000},{"action":"download","selector":"a[href*=ofx]","timeout":30000}]'
WHERE id = 3;

-- Bradesco: AUTOMATED
UPDATE instituicao SET
    ofx_supported = true, login_mode = 'AUTOMATED', python_endpoint = '/capture',
    bank_url = 'https://banco.bradesco/',
    navigation_steps_json = '[{"action":"wait_for_selector","selector":"input[id*=agencia]","timeout":15000},{"action":"fill","selector":"input[id*=agencia]","text":"{{AGENCIA}}"},{"action":"fill","selector":"input[id*=conta]","text":"{{CONTA}}"},{"action":"click","selector":"button[type=submit]"},{"action":"wait_for_selector","selector":"input[type=password]","timeout":20000},{"action":"fill","selector":"input[type=password]","text":"{{SENHA}}"},{"action":"click","selector":"button[type=submit]"},{"action":"wait_for_selector","selector":"a[href*=extrato]","timeout":30000},{"action":"click","selector":"a[href*=extrato]"},{"action":"wait_for_selector","selector":"a[href*=ofx]","timeout":30000},{"action":"download","selector":"a[href*=ofx]","timeout":30000}]'
WHERE id = 4;

-- Banco do Brasil: AUTOMATED
UPDATE instituicao SET
    ofx_supported = true, login_mode = 'AUTOMATED', python_endpoint = '/capture',
    bank_url = 'https://www2.bancobrasil.com.br/aapf/login.jsp',
    navigation_steps_json = '[{"action":"wait_for_selector","selector":"input[id*=cpf]","timeout":15000},{"action":"fill","selector":"input[id*=cpf]","text":"{{CPF}}"},{"action":"click","selector":"button[type=submit]"},{"action":"wait_for_selector","selector":"input[type=password]","timeout":15000},{"action":"fill","selector":"input[type=password]","text":"{{SENHA}}"},{"action":"click","selector":"button[type=submit]"},{"action":"wait_for_selector","selector":"a[href*=extrato]","timeout":30000},{"action":"click","selector":"a[href*=extrato]"},{"action":"wait_for_selector","selector":"a[href*=ofx]","timeout":30000},{"action":"download","selector":"a[href*=ofx]","timeout":30000}]'
WHERE id = 5;

-- Inter: MANUAL (QR Code com celular - confirmar se necessario)
UPDATE instituicao SET
    ofx_supported = true, login_mode = 'MANUAL', python_endpoint = '/capture',
    bank_url = 'https://internetbanking.bancointer.com.br/login',
    navigation_steps_json = null
WHERE id = 6;

-- Safra: MANUAL
UPDATE instituicao SET
    ofx_supported = true, login_mode = 'MANUAL', python_endpoint = '/capture',
    bank_url = 'https://www.safra.com.br/pessoa-fisica/internet-banking/',
    navigation_steps_json = null
WHERE id = 7;

-- Alelo Alimentacao: AUTOMATED via interceptacao de API
-- Nao tem OFX nativo — o Python intercepta o XHR e gera OFX
UPDATE instituicao SET
    ofx_supported = true, login_mode = 'AUTOMATED', python_endpoint = '/capture/alelo',
    bank_url = 'https://www.meualelo.com.br/',
    navigation_steps_json = '[{"action":"fill","selector":"input[id=username]","text":"{{CPF}}"},{"action":"fill","selector":"input[id=password]","text":"{{SENHA}}"}]'
WHERE id = 8;

-- Alelo Refeicao: mesmo endpoint do Alelo
UPDATE instituicao SET
    ofx_supported = true, login_mode = 'AUTOMATED', python_endpoint = '/capture/alelo',
    bank_url = 'https://www.meualelo.com.br/',
    navigation_steps_json = '[{"action":"fill","selector":"input[id=username]","text":"{{CPF}}"},{"action":"fill","selector":"input[id=password]","text":"{{SENHA}}"}]'
WHERE id = 9;

-- Alelo Multibeneficios: mesmo endpoint do Alelo
UPDATE instituicao SET
    ofx_supported = true, login_mode = 'AUTOMATED', python_endpoint = '/capture/alelo',
    bank_url = 'https://www.meualelo.com.br/',
    navigation_steps_json = '[{"action":"fill","selector":"input[id=username]","text":"{{CPF}}"},{"action":"fill","selector":"input[id=password]","text":"{{SENHA}}"}]'
WHERE id = 10;

-- Pluxee, Ticket, Vale: MANUAL (validar suporte a OFX)
UPDATE instituicao SET ofx_supported = false, login_mode = 'MANUAL', python_endpoint = '/capture', bank_url = 'https://www.pluxee.com.br/' WHERE id = 11;
UPDATE instituicao SET ofx_supported = false, login_mode = 'MANUAL', python_endpoint = '/capture', bank_url = 'https://www.ticket.com.br/' WHERE id = 12;
UPDATE instituicao SET ofx_supported = false, login_mode = 'MANUAL', python_endpoint = '/capture' WHERE id = 13;
UPDATE instituicao SET ofx_supported = false, login_mode = 'MANUAL', python_endpoint = '/capture' WHERE id = 14;

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

INSERT INTO evento_detalhe (fk_evento, titulo_gasto)
VALUES ('9b2e1f4c-8a7d-4c3b-9f12-7d8e5a6b7c90',
        'Salário');

INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id)
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

INSERT INTO evento_detalhe (fk_evento, titulo_gasto)
VALUES ('44444444-dddd-eeee-ffff-000000000004',
        'Casaco de lã');

INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id)
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

INSERT INTO evento_detalhe (fk_evento, titulo_gasto)
VALUES ('550e8400-e29b-41d4-a716-446655440000',
        'Transporte público');

INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id)
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

INSERT INTO evento_detalhe (fk_evento, titulo_gasto)
VALUES ('123e4567-e89b-12d3-a456-426614174000',
        'Restaurante');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id)
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

INSERT INTO evento_detalhe (fk_evento, titulo_gasto)
VALUES ('11111111-aaaa-bbbb-cccc-000000000001',
        'Carne bovina');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id)
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

INSERT INTO evento_detalhe (fk_evento, titulo_gasto)
VALUES ('22222222-bbbb-cccc-dddd-000000000002',
        'Conta de Água');

INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id)
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
INSERT INTO evento_detalhe (fk_evento, titulo_gasto)
VALUES ('33333333-cccc-dddd-eeee-000000000003',
        'Transporte público');

INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id)
VALUES (7, 6);


-- ============================================================
-- MASSA DE DADOS ADICIONAIS
-- Eventos financeiros mensais (Janeiro a Junho 2026)
-- Recebimentos e Gastos variados por usuário
--
-- Referências de instituicao_usuario (iu=):
--   1=Marcio/ValeRef  2=Marcio/Itaú       3=Marcio/BancoBrasil
--   4=Jonas/Nubank    5=Jonas/Bradesco     6=Jonas/Safra
--   7=Mariana/Santander  8=Mariana/Inter   9=Mariana/AleloAlim  10=Mariana/Ticket
--   11=Carla/AleloRef    12=Carla/ValeAlim
--   13=Felipe/Itaú    14=Felipe/Nubank     15=Felipe/Santander
--   16=Felipe/Bradesco   17=Felipe/BancoBrasil
--   18=Luciana/Inter  19=Luciana/AleloMulti  20=Luciana/Pluxee
--   21=Rafael/Safra   22=Rafael/Ticket
--   23=Patricia/AleloAlim  24=Patricia/AleloRef  25=Patricia/ValeRef  26=Patricia/ValeAlim
--
-- Referências de categoria_usuario (cu=):
--   1=Marcio/Roupas  2=Marcio/Comida  3=Marcio/Transporte
--   4=Jonas/Mercado  5=Jonas/ContaLuz
--   6=Mariana/Comida  7=Mariana/ContaÁgua
--   8=Carla/Mercado   9=Carla/Transporte
--   10=Felipe/Roupas  11=Felipe/BilheteÚnico
--   12=Luciana/Comida  13=Luciana/ContaÁgua
--   14=Rafael/Transporte  15=Rafael/ContaLuz
--   16=Patricia/Mercado   17=Patricia/Roupas
--
-- evento_detalhe auto-increment continua a partir de 8
-- ============================================================


-- ============================================================
-- MÁRCIO PEREIRA COSTA
-- ============================================================

-- Marcio | Janeiro | Salário [detalhe=8]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a0010100-2026-0000-0000-000000000000','21eb5d2f-3fd8-439e-b647-5cc1f753ae58','Recebimento',5000.00,'Salário de janeiro','2026-01-05',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a0010100-2026-0000-0000-000000000000',2,'Debito',5000.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a0010100-2026-0000-0000-000000000000','Salário janeiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(8,2);

-- Marcio | Janeiro | Gasto Roupas [detalhe=9]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a0010200-2026-0000-0000-000000000000','21eb5d2f-3fd8-439e-b647-5cc1f753ae58','Gasto',150.00,'Compra de tênis novo','2026-01-18',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a0010200-2026-0000-0000-000000000000',2,'Credito',150.00,3);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a0010200-2026-0000-0000-000000000000','Tênis esportivo');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(9,1);

-- Marcio | Fevereiro | Salário [detalhe=10]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a0020100-2026-0000-0000-000000000000','21eb5d2f-3fd8-439e-b647-5cc1f753ae58','Recebimento',5000.00,'Salário de fevereiro','2026-02-05',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a0020100-2026-0000-0000-000000000000',2,'Debito',5000.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a0020100-2026-0000-0000-000000000000','Salário fevereiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(10,2);

-- Marcio | Março | Salário [detalhe=11]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a0030100-2026-0000-0000-000000000000','21eb5d2f-3fd8-439e-b647-5cc1f753ae58','Recebimento',5000.00,'Salário de março','2026-03-05',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a0030100-2026-0000-0000-000000000000',2,'Debito',5000.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a0030100-2026-0000-0000-000000000000','Salário março');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(11,2);

-- Marcio | Abril | Gasto Comida (abril já tem salário) [detalhe=12]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a0040200-2026-0000-0000-000000000000','21eb5d2f-3fd8-439e-b647-5cc1f753ae58','Gasto',220.00,'Refeições com vale refeição','2026-04-12',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a0040200-2026-0000-0000-000000000000',1,'Debito',220.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a0040200-2026-0000-0000-000000000000','Alimentação abril');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(12,2);

-- Marcio | Abril | Gasto Transporte [detalhe=13]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a0040300-2026-0000-0000-000000000000','21eb5d2f-3fd8-439e-b647-5cc1f753ae58','Gasto',80.00,'Recarga bilhete único abril','2026-04-20',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a0040300-2026-0000-0000-000000000000',2,'Pix',80.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a0040300-2026-0000-0000-000000000000','Bilhete único abril');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(13,3);

-- Marcio | Maio | Salário [detalhe=14]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a0050100-2026-0000-0000-000000000000','21eb5d2f-3fd8-439e-b647-5cc1f753ae58','Recebimento',5000.00,'Salário de maio','2026-05-05',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a0050100-2026-0000-0000-000000000000',2,'Debito',5000.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a0050100-2026-0000-0000-000000000000','Salário maio');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(14,2);

-- Marcio | Maio | Gasto Roupas [detalhe=15]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a0050200-2026-0000-0000-000000000000','21eb5d2f-3fd8-439e-b647-5cc1f753ae58','Gasto',280.00,'Compra de roupas de inverno','2026-05-22',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a0050200-2026-0000-0000-000000000000',2,'Credito',280.00,2);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a0050200-2026-0000-0000-000000000000','Jaqueta e calça jeans');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(15,1);

-- Marcio | Junho | Salário com reajuste [detalhe=16]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a0060100-2026-0000-0000-000000000000','21eb5d2f-3fd8-439e-b647-5cc1f753ae58','Recebimento',5200.00,'Salário de junho com reajuste','2026-06-05',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a0060100-2026-0000-0000-000000000000',2,'Debito',5200.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a0060100-2026-0000-0000-000000000000','Salário junho reajustado');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(16,2);

-- Marcio | Junho | Gasto Comida [detalhe=17]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a0060200-2026-0000-0000-000000000000','21eb5d2f-3fd8-439e-b647-5cc1f753ae58','Gasto',190.00,'Refeições de junho','2026-06-18',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a0060200-2026-0000-0000-000000000000',1,'Debito',190.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a0060200-2026-0000-0000-000000000000','Almoços e lanches junho');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(17,2);


-- ============================================================
-- JONAS PINTO
-- ============================================================

-- Jonas | Janeiro | Aposentadoria [detalhe=18]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a1010100-2026-0000-0000-000000000000','b5135981-72fc-46ee-a6c6-4072495f7afd','Recebimento',4200.00,'Aposentadoria de janeiro','2026-01-10',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a1010100-2026-0000-0000-000000000000',4,'Debito',4200.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a1010100-2026-0000-0000-000000000000','Aposentadoria janeiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(18,4);

-- Jonas | Janeiro | Gasto Mercado [detalhe=19]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a1010200-2026-0000-0000-000000000000','b5135981-72fc-46ee-a6c6-4072495f7afd','Gasto',280.00,'Compras semanais no mercado','2026-01-22',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a1010200-2026-0000-0000-000000000000',5,'Debito',280.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a1010200-2026-0000-0000-000000000000','Mercado semana');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(19,4);

-- Jonas | Janeiro | Gasto Conta de Luz [detalhe=20]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a1010300-2026-0000-0000-000000000000','b5135981-72fc-46ee-a6c6-4072495f7afd','Gasto',145.00,'Conta de luz de janeiro','2026-01-25',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a1010300-2026-0000-0000-000000000000',6,'Boleto',145.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a1010300-2026-0000-0000-000000000000','Conta de luz janeiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(20,5);

-- Jonas | Fevereiro | Aposentadoria [detalhe=21]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a1020100-2026-0000-0000-000000000000','b5135981-72fc-46ee-a6c6-4072495f7afd','Recebimento',4200.00,'Aposentadoria de fevereiro','2026-02-10',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a1020100-2026-0000-0000-000000000000',4,'Debito',4200.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a1020100-2026-0000-0000-000000000000','Aposentadoria fevereiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(21,4);

-- Jonas | Fevereiro | Gasto Conta de Luz [detalhe=22]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a1020200-2026-0000-0000-000000000000','b5135981-72fc-46ee-a6c6-4072495f7afd','Gasto',150.00,'Conta de luz de fevereiro','2026-02-22',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a1020200-2026-0000-0000-000000000000',6,'Boleto',150.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a1020200-2026-0000-0000-000000000000','Conta de luz fevereiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(22,5);

-- Jonas | Março | Aposentadoria [detalhe=23]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a1030100-2026-0000-0000-000000000000','b5135981-72fc-46ee-a6c6-4072495f7afd','Recebimento',4200.00,'Aposentadoria de março','2026-03-10',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a1030100-2026-0000-0000-000000000000',4,'Debito',4200.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a1030100-2026-0000-0000-000000000000','Aposentadoria março');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(23,4);

-- Jonas | Março | Gasto Mercado [detalhe=24]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a1030200-2026-0000-0000-000000000000','b5135981-72fc-46ee-a6c6-4072495f7afd','Gasto',320.00,'Compras mensais no mercado','2026-03-18',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a1030200-2026-0000-0000-000000000000',5,'Debito',320.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a1030200-2026-0000-0000-000000000000','Mercado março');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(24,4);

-- Jonas | Abril | Aposentadoria [detalhe=25]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a1040100-2026-0000-0000-000000000000','b5135981-72fc-46ee-a6c6-4072495f7afd','Recebimento',4200.00,'Aposentadoria de abril','2026-04-10',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a1040100-2026-0000-0000-000000000000',4,'Debito',4200.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a1040100-2026-0000-0000-000000000000','Aposentadoria abril');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(25,4);

-- Jonas | Abril | Gasto Conta de Luz [detalhe=26]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a1040200-2026-0000-0000-000000000000','b5135981-72fc-46ee-a6c6-4072495f7afd','Gasto',160.00,'Conta de luz de abril','2026-04-25',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a1040200-2026-0000-0000-000000000000',6,'Boleto',160.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a1040200-2026-0000-0000-000000000000','Conta de luz abril');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(26,5);

-- Jonas | Maio | Aposentadoria [detalhe=27]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a1050100-2026-0000-0000-000000000000','b5135981-72fc-46ee-a6c6-4072495f7afd','Recebimento',4200.00,'Aposentadoria de maio','2026-05-10',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a1050100-2026-0000-0000-000000000000',4,'Debito',4200.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a1050100-2026-0000-0000-000000000000','Aposentadoria maio');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(27,4);

-- Jonas | Maio | Gasto Mercado [detalhe=28]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a1050200-2026-0000-0000-000000000000','b5135981-72fc-46ee-a6c6-4072495f7afd','Gasto',290.00,'Compras do mês de maio','2026-05-15',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a1050200-2026-0000-0000-000000000000',5,'Debito',290.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a1050200-2026-0000-0000-000000000000','Supermercado maio');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(28,4);

-- Jonas | Junho | Aposentadoria [detalhe=29]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a1060100-2026-0000-0000-000000000000','b5135981-72fc-46ee-a6c6-4072495f7afd','Recebimento',4200.00,'Aposentadoria de junho','2026-06-10',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a1060100-2026-0000-0000-000000000000',4,'Debito',4200.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a1060100-2026-0000-0000-000000000000','Aposentadoria junho');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(29,4);

-- Jonas | Junho | Gasto Conta de Luz [detalhe=30]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a1060200-2026-0000-0000-000000000000','b5135981-72fc-46ee-a6c6-4072495f7afd','Gasto',175.00,'Conta de luz de junho','2026-06-20',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a1060200-2026-0000-0000-000000000000',6,'Boleto',175.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a1060200-2026-0000-0000-000000000000','Conta de luz junho');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(30,5);


-- ============================================================
-- MARIANA ROSA DA SILVA
-- ============================================================

-- Mariana | Janeiro | Salário [detalhe=31]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a2010100-2026-0000-0000-000000000000','2f1ed17a-c8b6-425b-adb1-b28126be88de','Recebimento',3800.00,'Salário de janeiro','2026-01-05',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a2010100-2026-0000-0000-000000000000',7,'Debito',3800.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a2010100-2026-0000-0000-000000000000','Salário janeiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(31,6);

-- Mariana | Janeiro | Gasto Conta de Água [detalhe=32]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a2010200-2026-0000-0000-000000000000','2f1ed17a-c8b6-425b-adb1-b28126be88de','Gasto',110.00,'Conta de água de janeiro','2026-01-15',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a2010200-2026-0000-0000-000000000000',8,'Boleto',110.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a2010200-2026-0000-0000-000000000000','Conta de água janeiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(32,7);

-- Mariana | Fevereiro | Salário [detalhe=33]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a2020100-2026-0000-0000-000000000000','2f1ed17a-c8b6-425b-adb1-b28126be88de','Recebimento',3800.00,'Salário de fevereiro','2026-02-05',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a2020100-2026-0000-0000-000000000000',7,'Debito',3800.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a2020100-2026-0000-0000-000000000000','Salário fevereiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(33,6);

-- Mariana | Fevereiro | Gasto Comida [detalhe=34]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a2020200-2026-0000-0000-000000000000','2f1ed17a-c8b6-425b-adb1-b28126be88de','Gasto',200.00,'Alimentação de fevereiro','2026-02-20',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a2020200-2026-0000-0000-000000000000',9,'Debito',200.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a2020200-2026-0000-0000-000000000000','Restaurantes e lanches');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(34,6);

-- Mariana | Março | Salário (março já tem conta de água) [detalhe=35]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a2030100-2026-0000-0000-000000000000','2f1ed17a-c8b6-425b-adb1-b28126be88de','Recebimento',3800.00,'Salário de março','2026-03-05',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a2030100-2026-0000-0000-000000000000',7,'Debito',3800.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a2030100-2026-0000-0000-000000000000','Salário março');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(35,6);

-- Mariana | Abril | Salário [detalhe=36]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a2040100-2026-0000-0000-000000000000','2f1ed17a-c8b6-425b-adb1-b28126be88de','Recebimento',3800.00,'Salário de abril','2026-04-05',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a2040100-2026-0000-0000-000000000000',7,'Debito',3800.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a2040100-2026-0000-0000-000000000000','Salário abril');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(36,6);

-- Mariana | Abril | Gasto Conta de Água [detalhe=37]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a2040200-2026-0000-0000-000000000000','2f1ed17a-c8b6-425b-adb1-b28126be88de','Gasto',95.00,'Conta de água de abril','2026-04-18',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a2040200-2026-0000-0000-000000000000',8,'Boleto',95.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a2040200-2026-0000-0000-000000000000','Conta de água abril');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(37,7);

-- Mariana | Maio | Salário [detalhe=38]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a2050100-2026-0000-0000-000000000000','2f1ed17a-c8b6-425b-adb1-b28126be88de','Recebimento',3800.00,'Salário de maio','2026-05-05',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a2050100-2026-0000-0000-000000000000',7,'Debito',3800.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a2050100-2026-0000-0000-000000000000','Salário maio');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(38,6);

-- Mariana | Maio | Gasto Comida [detalhe=39]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a2050200-2026-0000-0000-000000000000','2f1ed17a-c8b6-425b-adb1-b28126be88de','Gasto',185.00,'Alimentação de maio','2026-05-22',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a2050200-2026-0000-0000-000000000000',9,'Debito',185.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a2050200-2026-0000-0000-000000000000','Alelo alimentação maio');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(39,6);

-- Mariana | Junho | Salário reajustado [detalhe=40]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a2060100-2026-0000-0000-000000000000','2f1ed17a-c8b6-425b-adb1-b28126be88de','Recebimento',3950.00,'Salário de junho reajustado','2026-06-05',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a2060100-2026-0000-0000-000000000000',7,'Debito',3950.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a2060100-2026-0000-0000-000000000000','Salário junho');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(40,6);

-- Mariana | Junho | Gasto Conta de Água [detalhe=41]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a2060200-2026-0000-0000-000000000000','2f1ed17a-c8b6-425b-adb1-b28126be88de','Gasto',105.00,'Conta de água de junho','2026-06-12',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a2060200-2026-0000-0000-000000000000',8,'Boleto',105.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a2060200-2026-0000-0000-000000000000','Conta de água junho');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(41,7);


-- ============================================================
-- CARLA SOUZA LIMA
-- ============================================================

-- Carla | Janeiro | Salário [detalhe=42]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a3010100-2026-0000-0000-000000000000','8a2f1c3d-5b44-4f9a-9d2a-1c8f7a9e6b11','Recebimento',3200.00,'Salário de janeiro','2026-01-08',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a3010100-2026-0000-0000-000000000000',12,'Debito',3200.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a3010100-2026-0000-0000-000000000000','Salário janeiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(42,8);

-- Carla | Janeiro | Gasto Mercado [detalhe=43]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a3010200-2026-0000-0000-000000000000','8a2f1c3d-5b44-4f9a-9d2a-1c8f7a9e6b11','Gasto',400.00,'Compras mensais no supermercado','2026-01-22',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a3010200-2026-0000-0000-000000000000',12,'Debito',400.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a3010200-2026-0000-0000-000000000000','Supermercado janeiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(43,8);

-- Carla | Fevereiro | Salário [detalhe=44]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a3020100-2026-0000-0000-000000000000','8a2f1c3d-5b44-4f9a-9d2a-1c8f7a9e6b11','Recebimento',3200.00,'Salário de fevereiro','2026-02-08',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a3020100-2026-0000-0000-000000000000',12,'Debito',3200.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a3020100-2026-0000-0000-000000000000','Salário fevereiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(44,8);

-- Carla | Fevereiro | Gasto Transporte [detalhe=45]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a3020200-2026-0000-0000-000000000000','8a2f1c3d-5b44-4f9a-9d2a-1c8f7a9e6b11','Gasto',60.00,'Recarga de transporte fevereiro','2026-02-25',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a3020200-2026-0000-0000-000000000000',11,'Pix',60.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a3020200-2026-0000-0000-000000000000','Alelo refeição fevereiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(45,9);

-- Carla | Março | Salário [detalhe=46]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a3030100-2026-0000-0000-000000000000','8a2f1c3d-5b44-4f9a-9d2a-1c8f7a9e6b11','Recebimento',3200.00,'Salário de março','2026-03-08',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a3030100-2026-0000-0000-000000000000',12,'Debito',3200.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a3030100-2026-0000-0000-000000000000','Salário março');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(46,8);

-- Carla | Março | Gasto Mercado [detalhe=47]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a3030200-2026-0000-0000-000000000000','8a2f1c3d-5b44-4f9a-9d2a-1c8f7a9e6b11','Gasto',380.00,'Mercado de março','2026-03-25',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a3030200-2026-0000-0000-000000000000',12,'Debito',380.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a3030200-2026-0000-0000-000000000000','Vale alimentação março');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(47,8);

-- Carla | Abril | Salário (abril já tem gasto de ônibus) [detalhe=48]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a3040100-2026-0000-0000-000000000000','8a2f1c3d-5b44-4f9a-9d2a-1c8f7a9e6b11','Recebimento',3200.00,'Salário de abril','2026-04-08',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a3040100-2026-0000-0000-000000000000',12,'Debito',3200.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a3040100-2026-0000-0000-000000000000','Salário abril');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(48,8);

-- Carla | Maio | Salário [detalhe=49]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a3050100-2026-0000-0000-000000000000','8a2f1c3d-5b44-4f9a-9d2a-1c8f7a9e6b11','Recebimento',3200.00,'Salário de maio','2026-05-08',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a3050100-2026-0000-0000-000000000000',12,'Debito',3200.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a3050100-2026-0000-0000-000000000000','Salário maio');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(49,8);

-- Carla | Maio | Gasto Mercado [detalhe=50]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a3050200-2026-0000-0000-000000000000','8a2f1c3d-5b44-4f9a-9d2a-1c8f7a9e6b11','Gasto',420.00,'Mercado de maio','2026-05-20',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a3050200-2026-0000-0000-000000000000',12,'Debito',420.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a3050200-2026-0000-0000-000000000000','Compras mensais maio');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(50,8);

-- Carla | Maio | Gasto Transporte [detalhe=51]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a3050300-2026-0000-0000-000000000000','8a2f1c3d-5b44-4f9a-9d2a-1c8f7a9e6b11','Gasto',55.00,'Recarga de transporte maio','2026-05-28',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a3050300-2026-0000-0000-000000000000',11,'Pix',55.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a3050300-2026-0000-0000-000000000000','Recarga ônibus maio');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(51,9);

-- Carla | Junho | Salário [detalhe=52]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a3060100-2026-0000-0000-000000000000','8a2f1c3d-5b44-4f9a-9d2a-1c8f7a9e6b11','Recebimento',3200.00,'Salário de junho','2026-06-08',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a3060100-2026-0000-0000-000000000000',12,'Debito',3200.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a3060100-2026-0000-0000-000000000000','Salário junho');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(52,8);

-- Carla | Junho | Gasto Mercado [detalhe=53]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a3060200-2026-0000-0000-000000000000','8a2f1c3d-5b44-4f9a-9d2a-1c8f7a9e6b11','Gasto',350.00,'Compras de junho','2026-06-18',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a3060200-2026-0000-0000-000000000000',12,'Debito',350.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a3060200-2026-0000-0000-000000000000','Supermercado junho');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(53,8);


-- ============================================================
-- FELIPE ALMEIDA SANTOS
-- ============================================================

-- Felipe | Janeiro | Salário [detalhe=54]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a4010100-2026-0000-0000-000000000000','d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22','Recebimento',6500.00,'Salário de janeiro','2026-01-05',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a4010100-2026-0000-0000-000000000000',17,'Debito',6500.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a4010100-2026-0000-0000-000000000000','Salário janeiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(54,10);

-- Felipe | Janeiro | Gasto Roupas [detalhe=55]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a4010200-2026-0000-0000-000000000000','d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22','Gasto',200.00,'Compra de roupas de trabalho','2026-01-18',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a4010200-2026-0000-0000-000000000000',13,'Credito',200.00,2);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a4010200-2026-0000-0000-000000000000','Camisa social e calça');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(55,10);

-- Felipe | Janeiro | Gasto Bilhete Único [detalhe=56]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a4010300-2026-0000-0000-000000000000','d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22','Gasto',150.00,'Bilhete único mensal janeiro','2026-01-30',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a4010300-2026-0000-0000-000000000000',14,'Pix',150.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a4010300-2026-0000-0000-000000000000','Bilhete único janeiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(56,11);

-- Felipe | Fevereiro | Salário [detalhe=57]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a4020100-2026-0000-0000-000000000000','d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22','Recebimento',6500.00,'Salário de fevereiro','2026-02-05',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a4020100-2026-0000-0000-000000000000',17,'Debito',6500.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a4020100-2026-0000-0000-000000000000','Salário fevereiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(57,10);

-- Felipe | Fevereiro | Gasto Bilhete Único [detalhe=58]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a4020200-2026-0000-0000-000000000000','d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22','Gasto',150.00,'Bilhete único de fevereiro','2026-02-28',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a4020200-2026-0000-0000-000000000000',14,'Pix',150.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a4020200-2026-0000-0000-000000000000','Bilhete único fevereiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(58,11);

-- Felipe | Março | Salário [detalhe=59]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a4030100-2026-0000-0000-000000000000','d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22','Recebimento',6500.00,'Salário de março','2026-03-05',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a4030100-2026-0000-0000-000000000000',17,'Debito',6500.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a4030100-2026-0000-0000-000000000000','Salário março');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(59,10);

-- Felipe | Março | Gasto Roupas [detalhe=60]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a4030200-2026-0000-0000-000000000000','d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22','Gasto',350.00,'Roupas de outono','2026-03-20',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a4030200-2026-0000-0000-000000000000',15,'Credito',350.00,3);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a4030200-2026-0000-0000-000000000000','Roupas outono');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(60,10);

-- Felipe | Abril | Salário [detalhe=61]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a4040100-2026-0000-0000-000000000000','d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22','Recebimento',6500.00,'Salário de abril','2026-04-05',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a4040100-2026-0000-0000-000000000000',17,'Debito',6500.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a4040100-2026-0000-0000-000000000000','Salário abril');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(61,10);

-- Felipe | Abril | Gasto Bilhete Único [detalhe=62]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a4040200-2026-0000-0000-000000000000','d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22','Gasto',155.00,'Bilhete único de abril','2026-04-25',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a4040200-2026-0000-0000-000000000000',14,'Pix',155.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a4040200-2026-0000-0000-000000000000','Bilhete único abril');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(62,11);

-- Felipe | Maio | Salário [detalhe=63]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a4050100-2026-0000-0000-000000000000','d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22','Recebimento',6500.00,'Salário de maio','2026-05-05',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a4050100-2026-0000-0000-000000000000',17,'Debito',6500.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a4050100-2026-0000-0000-000000000000','Salário maio');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(63,10);

-- Felipe | Maio | Gasto Roupas [detalhe=64]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a4050200-2026-0000-0000-000000000000','d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22','Gasto',300.00,'Compra de roupas em maio','2026-05-18',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a4050200-2026-0000-0000-000000000000',16,'Credito',300.00,2);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a4050200-2026-0000-0000-000000000000','Roupas inverno maio');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(64,10);

-- Felipe | Junho | Salário com bônus semestral [detalhe=65]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a4060100-2026-0000-0000-000000000000','d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22','Recebimento',7500.00,'Salário de junho com bônus semestral','2026-06-05',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a4060100-2026-0000-0000-000000000000',17,'Debito',7500.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a4060100-2026-0000-0000-000000000000','Salário junho + bônus');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(65,10);

-- Felipe | Junho | Gasto Bilhete Único [detalhe=66]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a4060200-2026-0000-0000-000000000000','d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22','Gasto',160.00,'Bilhete único de junho','2026-06-20',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a4060200-2026-0000-0000-000000000000',14,'Pix',160.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a4060200-2026-0000-0000-000000000000','Bilhete único junho');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(66,11);


-- ============================================================
-- LUCIANA FERREIRA
-- ============================================================

-- Luciana | Janeiro | Salário [detalhe=67]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a5010100-2026-0000-0000-000000000000','3c6f9a8b-7d55-4e2a-9f88-4a7c1b9d6e33','Recebimento',4800.00,'Salário de janeiro','2026-01-10',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a5010100-2026-0000-0000-000000000000',18,'Debito',4800.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a5010100-2026-0000-0000-000000000000','Salário janeiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(67,12);

-- Luciana | Janeiro | Gasto Comida [detalhe=68]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a5010200-2026-0000-0000-000000000000','3c6f9a8b-7d55-4e2a-9f88-4a7c1b9d6e33','Gasto',130.00,'Refeições de janeiro','2026-01-20',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a5010200-2026-0000-0000-000000000000',20,'Debito',130.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a5010200-2026-0000-0000-000000000000','Pluxee alimentação');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(68,12);

-- Luciana | Fevereiro | Salário [detalhe=69]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a5020100-2026-0000-0000-000000000000','3c6f9a8b-7d55-4e2a-9f88-4a7c1b9d6e33','Recebimento',4800.00,'Salário de fevereiro','2026-02-10',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a5020100-2026-0000-0000-000000000000',18,'Debito',4800.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a5020100-2026-0000-0000-000000000000','Salário fevereiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(69,12);

-- Luciana | Fevereiro | Gasto Conta de Água [detalhe=70]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a5020200-2026-0000-0000-000000000000','3c6f9a8b-7d55-4e2a-9f88-4a7c1b9d6e33','Gasto',100.00,'Conta de água de fevereiro','2026-02-18',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a5020200-2026-0000-0000-000000000000',18,'Boleto',100.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a5020200-2026-0000-0000-000000000000','Conta de água fevereiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(70,13);

-- Luciana | Março | Salário [detalhe=71]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a5030100-2026-0000-0000-000000000000','3c6f9a8b-7d55-4e2a-9f88-4a7c1b9d6e33','Recebimento',4800.00,'Salário de março','2026-03-10',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a5030100-2026-0000-0000-000000000000',18,'Debito',4800.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a5030100-2026-0000-0000-000000000000','Salário março');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(71,12);

-- Luciana | Março | Gasto Comida [detalhe=72]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a5030200-2026-0000-0000-000000000000','3c6f9a8b-7d55-4e2a-9f88-4a7c1b9d6e33','Gasto',160.00,'Alimentação de março','2026-03-25',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a5030200-2026-0000-0000-000000000000',20,'Debito',160.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a5030200-2026-0000-0000-000000000000','Refeições março');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(72,12);

-- Luciana | Abril | Salário [detalhe=73]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a5040100-2026-0000-0000-000000000000','3c6f9a8b-7d55-4e2a-9f88-4a7c1b9d6e33','Recebimento',4800.00,'Salário de abril','2026-04-10',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a5040100-2026-0000-0000-000000000000',18,'Debito',4800.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a5040100-2026-0000-0000-000000000000','Salário abril');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(73,12);

-- Luciana | Abril | Gasto Conta de Água [detalhe=74]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a5040200-2026-0000-0000-000000000000','3c6f9a8b-7d55-4e2a-9f88-4a7c1b9d6e33','Gasto',110.00,'Conta de água de abril','2026-04-22',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a5040200-2026-0000-0000-000000000000',18,'Boleto',110.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a5040200-2026-0000-0000-000000000000','Conta de água abril');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(74,13);

-- Luciana | Maio | Salário [detalhe=75]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a5050100-2026-0000-0000-000000000000','3c6f9a8b-7d55-4e2a-9f88-4a7c1b9d6e33','Recebimento',4800.00,'Salário de maio','2026-05-10',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a5050100-2026-0000-0000-000000000000',18,'Debito',4800.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a5050100-2026-0000-0000-000000000000','Salário maio');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(75,12);

-- Luciana | Maio | Gasto Comida [detalhe=76]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a5050200-2026-0000-0000-000000000000','3c6f9a8b-7d55-4e2a-9f88-4a7c1b9d6e33','Gasto',145.00,'Refeições de maio','2026-05-15',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a5050200-2026-0000-0000-000000000000',20,'Debito',145.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a5050200-2026-0000-0000-000000000000','Almoços maio');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(76,12);

-- Luciana | Maio | Gasto Beneficio Multi [detalhe=77]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a5050300-2026-0000-0000-000000000000','3c6f9a8b-7d55-4e2a-9f88-4a7c1b9d6e33','Gasto',200.00,'Utilização benefício multi','2026-05-28',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a5050300-2026-0000-0000-000000000000',19,'Debito',200.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a5050300-2026-0000-0000-000000000000','Benefício alimentação multi');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(77,12);

-- Luciana | Junho | Salário [detalhe=78]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a5060100-2026-0000-0000-000000000000','3c6f9a8b-7d55-4e2a-9f88-4a7c1b9d6e33','Recebimento',4800.00,'Salário de junho','2026-06-10',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a5060100-2026-0000-0000-000000000000',18,'Debito',4800.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a5060100-2026-0000-0000-000000000000','Salário junho');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(78,12);

-- Luciana | Junho | Gasto Conta de Água [detalhe=79]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a5060200-2026-0000-0000-000000000000','3c6f9a8b-7d55-4e2a-9f88-4a7c1b9d6e33','Gasto',115.00,'Conta de água de junho','2026-06-15',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a5060200-2026-0000-0000-000000000000',18,'Boleto',115.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a5060200-2026-0000-0000-000000000000','Conta de água junho');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(79,13);


-- ============================================================
-- RAFAEL MENDES OLIVEIRA
-- ============================================================

-- Rafael | Janeiro | Salário [detalhe=80]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a6010100-2026-0000-0000-000000000000','a4d8e9c0-2b66-4f3d-9a99-5b8d2c7f4d44','Recebimento',3500.00,'Salário de janeiro','2026-01-07',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a6010100-2026-0000-0000-000000000000',21,'Debito',3500.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a6010100-2026-0000-0000-000000000000','Salário janeiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(80,14);

-- Rafael | Janeiro | Gasto Transporte [detalhe=81]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a6010200-2026-0000-0000-000000000000','a4d8e9c0-2b66-4f3d-9a99-5b8d2c7f4d44','Gasto',80.00,'Transporte de janeiro','2026-01-15',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a6010200-2026-0000-0000-000000000000',22,'Pix',80.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a6010200-2026-0000-0000-000000000000','Ticket transporte janeiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(81,14);

-- Rafael | Fevereiro | Salário [detalhe=82]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a6020100-2026-0000-0000-000000000000','a4d8e9c0-2b66-4f3d-9a99-5b8d2c7f4d44','Recebimento',3500.00,'Salário de fevereiro','2026-02-07',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a6020100-2026-0000-0000-000000000000',21,'Debito',3500.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a6020100-2026-0000-0000-000000000000','Salário fevereiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(82,14);

-- Rafael | Fevereiro | Gasto Conta de Luz [detalhe=83]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a6020200-2026-0000-0000-000000000000','a4d8e9c0-2b66-4f3d-9a99-5b8d2c7f4d44','Gasto',170.00,'Conta de luz de fevereiro','2026-02-20',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a6020200-2026-0000-0000-000000000000',21,'Boleto',170.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a6020200-2026-0000-0000-000000000000','Conta de luz fevereiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(83,15);

-- Rafael | Março | Salário [detalhe=84]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a6030100-2026-0000-0000-000000000000','a4d8e9c0-2b66-4f3d-9a99-5b8d2c7f4d44','Recebimento',3500.00,'Salário de março','2026-03-07',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a6030100-2026-0000-0000-000000000000',21,'Debito',3500.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a6030100-2026-0000-0000-000000000000','Salário março');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(84,14);

-- Rafael | Março | Gasto Transporte [detalhe=85]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a6030200-2026-0000-0000-000000000000','a4d8e9c0-2b66-4f3d-9a99-5b8d2c7f4d44','Gasto',90.00,'Transporte de março','2026-03-22',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a6030200-2026-0000-0000-000000000000',22,'Pix',90.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a6030200-2026-0000-0000-000000000000','Ticket transporte março');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(85,14);

-- Rafael | Abril | Salário [detalhe=86]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a6040100-2026-0000-0000-000000000000','a4d8e9c0-2b66-4f3d-9a99-5b8d2c7f4d44','Recebimento',3500.00,'Salário de abril','2026-04-07',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a6040100-2026-0000-0000-000000000000',21,'Debito',3500.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a6040100-2026-0000-0000-000000000000','Salário abril');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(86,14);

-- Rafael | Abril | Gasto Conta de Luz [detalhe=87]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a6040200-2026-0000-0000-000000000000','a4d8e9c0-2b66-4f3d-9a99-5b8d2c7f4d44','Gasto',185.00,'Conta de luz de abril','2026-04-20',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a6040200-2026-0000-0000-000000000000',21,'Boleto',185.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a6040200-2026-0000-0000-000000000000','Conta de luz abril');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(87,15);

-- Rafael | Maio | Salário [detalhe=88]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a6050100-2026-0000-0000-000000000000','a4d8e9c0-2b66-4f3d-9a99-5b8d2c7f4d44','Recebimento',3500.00,'Salário de maio','2026-05-07',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a6050100-2026-0000-0000-000000000000',21,'Debito',3500.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a6050100-2026-0000-0000-000000000000','Salário maio');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(88,14);

-- Rafael | Maio | Gasto Transporte [detalhe=89]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a6050200-2026-0000-0000-000000000000','a4d8e9c0-2b66-4f3d-9a99-5b8d2c7f4d44','Gasto',100.00,'Transporte de maio','2026-05-16',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a6050200-2026-0000-0000-000000000000',22,'Pix',100.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a6050200-2026-0000-0000-000000000000','Ticket transporte maio');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(89,14);

-- Rafael | Junho | Salário [detalhe=90]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a6060100-2026-0000-0000-000000000000','a4d8e9c0-2b66-4f3d-9a99-5b8d2c7f4d44','Recebimento',3500.00,'Salário de junho','2026-06-07',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a6060100-2026-0000-0000-000000000000',21,'Debito',3500.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a6060100-2026-0000-0000-000000000000','Salário junho');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(90,14);

-- Rafael | Junho | Gasto Conta de Luz [detalhe=91]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a6060200-2026-0000-0000-000000000000','a4d8e9c0-2b66-4f3d-9a99-5b8d2c7f4d44','Gasto',190.00,'Conta de luz de junho','2026-06-18',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a6060200-2026-0000-0000-000000000000',21,'Boleto',190.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a6060200-2026-0000-0000-000000000000','Conta de luz junho');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(91,15);


-- ============================================================
-- PATRÍCIA COSTA
-- ============================================================

-- Patricia | Janeiro | Salário [detalhe=92]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a7010100-2026-0000-0000-000000000000','f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55','Recebimento',4000.00,'Salário de janeiro','2026-01-06',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a7010100-2026-0000-0000-000000000000',25,'Debito',4000.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a7010100-2026-0000-0000-000000000000','Salário janeiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(92,16);

-- Patricia | Janeiro | Gasto Mercado [detalhe=93]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a7010200-2026-0000-0000-000000000000','f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55','Gasto',350.00,'Compras no mercado de janeiro','2026-01-25',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a7010200-2026-0000-0000-000000000000',26,'Debito',350.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a7010200-2026-0000-0000-000000000000','Vale alimentação mercado');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(93,16);

-- Patricia | Fevereiro | Salário [detalhe=94]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a7020100-2026-0000-0000-000000000000','f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55','Recebimento',4000.00,'Salário de fevereiro','2026-02-06',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a7020100-2026-0000-0000-000000000000',25,'Debito',4000.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a7020100-2026-0000-0000-000000000000','Salário fevereiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(94,16);

-- Patricia | Fevereiro | Gasto Alimentação [detalhe=95]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a7020200-2026-0000-0000-000000000000','f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55','Gasto',180.00,'Alimentação de fevereiro','2026-02-18',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a7020200-2026-0000-0000-000000000000',23,'Debito',180.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a7020200-2026-0000-0000-000000000000','Alelo alimentação fevereiro');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(95,16);

-- Patricia | Março | Salário [detalhe=96]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a7030100-2026-0000-0000-000000000000','f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55','Recebimento',4000.00,'Salário de março','2026-03-06',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a7030100-2026-0000-0000-000000000000',25,'Debito',4000.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a7030100-2026-0000-0000-000000000000','Salário março');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(96,16);

-- Patricia | Março | Gasto Restaurante [detalhe=97]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a7030200-2026-0000-0000-000000000000','f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55','Gasto',220.00,'Jantar em restaurante','2026-03-28',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a7030200-2026-0000-0000-000000000000',24,'Debito',220.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a7030200-2026-0000-0000-000000000000','Restaurante aniversário');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(97,16);

-- Patricia | Abril | Salário [detalhe=98]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a7040100-2026-0000-0000-000000000000','f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55','Recebimento',4000.00,'Salário de abril','2026-04-06',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a7040100-2026-0000-0000-000000000000',25,'Debito',4000.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a7040100-2026-0000-0000-000000000000','Salário abril');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(98,16);

-- Patricia | Abril | Gasto Mercado [detalhe=99]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a7040200-2026-0000-0000-000000000000','f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55','Gasto',300.00,'Compras de mercado em abril','2026-04-22',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a7040200-2026-0000-0000-000000000000',26,'Debito',300.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a7040200-2026-0000-0000-000000000000','Compras mensais abril');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(99,16);

-- Patricia | Abril | Gasto Roupas [detalhe=100]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a7040300-2026-0000-0000-000000000000','f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55','Gasto',120.00,'Compra de roupas em abril','2026-04-28',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a7040300-2026-0000-0000-000000000000',23,'Credito',120.00,2);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a7040300-2026-0000-0000-000000000000','Blusa e saia');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(100,17);

-- Patricia | Maio | Salário [detalhe=101]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a7050100-2026-0000-0000-000000000000','f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55','Recebimento',4000.00,'Salário de maio','2026-05-06',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a7050100-2026-0000-0000-000000000000',25,'Debito',4000.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a7050100-2026-0000-0000-000000000000','Salário maio');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(101,16);

-- Patricia | Maio | Gasto Mercado [detalhe=102]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a7050200-2026-0000-0000-000000000000','f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55','Gasto',380.00,'Compras de mercado em maio','2026-05-20',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a7050200-2026-0000-0000-000000000000',26,'Debito',380.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a7050200-2026-0000-0000-000000000000','Supermercado maio');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(102,16);

-- Patricia | Junho | Salário [detalhe=103]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a7060100-2026-0000-0000-000000000000','f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55','Recebimento',4000.00,'Salário de junho','2026-06-06',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a7060100-2026-0000-0000-000000000000',25,'Debito',4000.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a7060100-2026-0000-0000-000000000000','Salário junho');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(103,16);

-- Patricia | Junho | Gasto Alimentação [detalhe=104]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a7060200-2026-0000-0000-000000000000','f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55','Gasto',160.00,'Alelo refeição junho','2026-06-12',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a7060200-2026-0000-0000-000000000000',24,'Debito',160.00,1);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a7060200-2026-0000-0000-000000000000','Refeições junho');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(104,16);

-- Patricia | Junho | Gasto Roupas [detalhe=105]
INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro)
VALUES ('a7060300-2026-0000-0000-000000000000','f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55','Gasto',200.00,'Compra de roupas de inverno','2026-06-22',CURRENT_TIMESTAMP);
INSERT INTO evento_instituicao (fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas)
VALUES ('a7060300-2026-0000-0000-000000000000',23,'Credito',200.00,3);
INSERT INTO evento_detalhe (fk_evento, titulo_gasto) VALUES ('a7060300-2026-0000-0000-000000000000','Casaco e botas inverno');
INSERT INTO evento_detalhe_categoria (evento_detalhe_id, categoria_usuario_id) VALUES(105,17);

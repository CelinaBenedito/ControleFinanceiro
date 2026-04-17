INSERT INTO usuario (id, nome, sobrenome, data_nascimento, sexo, imagem, email, senha)
VALUES ('21eb5d2f-3fd8-439e-b647-5cc1f753ae58','Marcio', 'Pereira Costa', '1987-05-16', 'Masculino', null, 'marcinho.pereira@gmail.com', 'todosContra!'),
       ('b5135981-72fc-46ee-a6c6-4072495f7afd','Jonas', 'Pinto', '1956-09-26', 'Masculino', null, 'jonas@gmail.com', 'antigamenteEraMelhor'),
       ('2f1ed17a-c8b6-425b-adb1-b28126be88de','Mariana', 'Rosa da Silva', '2000-12-05', 'Feminino', null, 'am4nd1nh4@gmail.com', 'iLoveBT2'),
       ('8a2f1c3d-5b44-4f9a-9d2a-1c8f7a9e6b11','Carla','Souza Lima','1995-03-22','Feminino',null,'carla.lima95@gmail.com','senhaSegura123'),
       ('d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22','Felipe','Almeida Santos','1982-11-09','Masculino',null,'felipe.almeida82@gmail.com','Felipe@1982'),
       ('3c6f9a8b-7d55-4e2a-9f88-4a7c1b9d6e33','Luciana','Ferreira','1978-07-14','Feminino',null,'luciana.ferreira78@gmail.com','lucyF78'),
       ('a4d8e9c0-2b66-4f3d-9a99-5b8d2c7f4d44','Rafael','Mendes Oliveira','2001-01-30','Masculino',null,'rafa.mendes01@gmail.com','rafa2001'),
       ('f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55','Patrícia','Costa','1990-08-18','Feminino',null,'patricia.costa90@gmail.com','patyC90');

INSERT INTO instituicao(id,nome)
VALUES ('1', 'itau'),
        ('2', 'Nubank'),
        ('3', 'Santander'),
        ('4', 'Bradesco'),
        ('5', 'Banco do Brasil'),
        ('6', 'Inter'),
        ('7', 'Safra'),
        ('8', 'Alelo Alimentação'),
        ('9', 'Alelo Refeição'),
        ('10', 'Aelo Multibeneficios'),
        ('11', 'Pluxee'),
        ('12', 'Ticket'),
        ('13', 'Vale Refeição'),
        ('14', 'Vale Alimentação');

INSERT INTO instituicao_usuario(usuario_id, instituicao_id)
VALUES ('21eb5d2f-3fd8-439e-b647-5cc1f753ae58', '13'),
       ('21eb5d2f-3fd8-439e-b647-5cc1f753ae58','1'),
       ('21eb5d2f-3fd8-439e-b647-5cc1f753ae58','5'),
       ('b5135981-72fc-46ee-a6c6-4072495f7afd','2'),
       ('b5135981-72fc-46ee-a6c6-4072495f7afd','4'),
       ('b5135981-72fc-46ee-a6c6-4072495f7afd','7'),
       ('2f1ed17a-c8b6-425b-adb1-b28126be88de','3'),
       ('2f1ed17a-c8b6-425b-adb1-b28126be88de','6'),
       ('2f1ed17a-c8b6-425b-adb1-b28126be88de','8'),
       ('2f1ed17a-c8b6-425b-adb1-b28126be88de','12'),
       ('8a2f1c3d-5b44-4f9a-9d2a-1c8f7a9e6b11','9'),
       ('8a2f1c3d-5b44-4f9a-9d2a-1c8f7a9e6b11','14'),
       ('d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22','1'),
       ('d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22','2'),
       ('d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22','3'),
       ('d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22','4'),
       ('d9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22','5'),
       ('3c6f9a8b-7d55-4e2a-9f88-4a7c1b9d6e33','6'),
       ('3c6f9a8b-7d55-4e2a-9f88-4a7c1b9d6e33','10'),
       ('3c6f9a8b-7d55-4e2a-9f88-4a7c1b9d6e33','11'),
       ('a4d8e9c0-2b66-4f3d-9a99-5b8d2c7f4d44','7'),
       ('a4d8e9c0-2b66-4f3d-9a99-5b8d2c7f4d44','12'),
       ('f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55','8'),
       ('f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55','9'),
       ('f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55','13'),
       ('f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55','14');

INSERT INTO categoria(titulo)
VALUES('Roupas'),
      ('Comida'),
      ('Mercado'),
      ('Conta de Água'),
      ('Conta de Luz'),
      ('Transporte'),
      ('Bilhete Unico');


INSERT INTO configuracoes (id, usuario_id, inicio_mes_fiscal, final_mes_fiscal, ultima_atualizacao, limite_desejado_mensal)
VALUES
    ('8f1a2c3d-4b5e-678f-9012-3456789abcde', '21eb5d2f-3fd8-439e-b647-5cc1f753ae58', 1, 30, CURRENT_TIMESTAMP, 5000.00),
    ('9a2b3c4d-5e6f-7890-1234-56789abcdef0', 'b5135981-72fc-46ee-a6c6-4072495f7afd', 1, 30, CURRENT_TIMESTAMP, 4000.00),
    ('1b2c3d4e-5f6a-789b-0123-456789abcdef', '2f1ed17a-c8b6-425b-adb1-b28126be88de', 1, 30, CURRENT_TIMESTAMP, 3500.00),
    ('2c3d4e5f-6a7b-890c-1234-56789abcdef1', '8a2f1c3d-5b44-4f9a-9d2a-1c8f7a9e6b11', 1, 30, CURRENT_TIMESTAMP, 3000.00),
    ('3d4e5f6a-7b8c-901d-2345-6789abcdef12', 'd9b7e2f4-1a33-4c8d-8e77-2f4c9a7b5c22', 1, 30, CURRENT_TIMESTAMP, 6000.00),
    ('4e5f6a7b-8c9d-012e-3456-789abcdef123', '3c6f9a8b-7d55-4e2a-9f88-4a7c1b9d6e33', 1, 30, CURRENT_TIMESTAMP, 4500.00),
    ('5f6a7b8c-9d01-23ef-4567-89abcdef1234', 'a4d8e9c0-2b66-4f3d-9a99-5b8d2c7f4d44', 1, 30, CURRENT_TIMESTAMP, 3200.00),
    ('6a7b8c9d-0123-4f56-789a-bcdef1234567', 'f5e9d0a1-3c77-4a4e-8b00-6c9e3d8f5e55', 1, 30, CURRENT_TIMESTAMP, 3800.00);

INSERT INTO limite_por_instituicao (id, institucao_usuario_id, limite_desejado)
VALUES
    ('7b8c9d0e-1234-567f-890a-bcdef1234567', 1, 2000.00),
    ('8c9d0e1f-2345-6780-901b-cdef12345678', 2, 1500.00),
    ('9d0e1f2a-3456-7891-012c-def123456789', 3, 1500.00),

    ('0e1f2a3b-4567-8902-123d-ef123456789a', 4, 1200.00),
    ('1f2a3b4c-5678-9013-234e-f123456789ab', 5, 1000.00),
    ('2a3b4c5d-6789-0124-345f-123456789abc', 6, 800.00),

    ('3b4c5d6e-7890-1235-4560-23456789abcd', 7, 1000.00),
    ('4c5d6e7f-8901-2346-5671-3456789abcde', 8, 900.00),
    ('5d6e7f80-9012-3457-6782-456789abcdef', 9, 700.00),
    ('6e7f8091-0123-4568-7893-56789abcdef0', 10, 600.00),

    ('7f809102-1234-5679-8904-6789abcdef01', 11, 800.00),
    ('80910213-2345-678a-9015-789abcdef012', 12, 700.00),

    ('91021324-3456-789b-0126-89abcdef0123', 13, 1200.00),
    ('10213245-4567-89ac-1237-9abcdef01234', 14, 1100.00),
    ('21324556-5678-9abd-2348-abcdef012345', 15, 1000.00),
    ('32455667-6789-0ace-3459-bcdef0123456', 16, 900.00),
    ('45566778-7890-1adf-456a-cdef01234567', 17, 800.00),

    ('56677889-8901-2b10-567b-def012345678', 18, 1000.00),
    ('67788990-9012-3c21-678c-ef0123456789', 19, 700.00),
    ('77899001-0123-4d32-789d-f0123456789a', 20, 600.00),

    ('88900112-1234-5e43-890e-0123456789ab', 21, 900.00),
    ('99011223-2345-6f54-901f-123456789abc', 22, 800.00),

    ('00112234-3456-7056-0120-23456789abcd', 23, 1000.00),
    ('11223345-4567-8167-1231-3456789abcde', 24, 900.00),
    ('22334456-5678-9278-2342-456789abcdef', 25, 800.00),
    ('33445567-6789-0389-3453-56789abcdef0', 26, 700.00);

INSERT INTO limite_por_categoria (id, categoria_usuario_id, limite_desejado)
VALUES
    ('44556678-7890-149a-4564-6789abcdef01', 1, 800.00),
    ('55667789-8901-25ab-5675-789abcdef012', 2, 1200.00),
    ('66778890-9012-36bc-6786-89abcdef0123', 3, 600.00),

    ('77889901-0123-47cd-7897-9abcdef01234', 4, 500.00),
    ('88990112-1234-58de-8908-abcdef012345', 5, 400.00),

    ('99011223-2345-69ef-9019-bcdef0123456', 6, 700.00),
    ('00112234-3456-70f0-012a-cdef01234567', 7, 600.00),

    ('11223345-4567-81a1-123b-def012345678', 8, 900.00),
    ('22334456-5678-92b2-234c-ef0123456789', 9, 500.00),

    ('33445567-6789-03c3-345d-f0123456789a', 10, 1000.00),
    ('44556678-7890-14d4-456e-0123456789ab', 11, 800.00),

    ('55667789-8901-25e5-567f-123456789abc', 12, 700.00),
    ('66778890-9012-36f6-6780-23456789abcd', 13, 600.00),

    ('77889901-0123-47a7-7891-3456789abcde', 14, 500.00),
    ('88990112-1234-58b8-8902-456789abcdef', 15, 400.00),

    ('99011223-2345-69c9-9013-56789abcdef0', 16, 800.00),
    ('00112234-3456-70da-0124-6789abcdef01', 17, 700.00);

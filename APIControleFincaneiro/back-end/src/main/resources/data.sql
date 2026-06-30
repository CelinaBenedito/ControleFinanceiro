-- =============================================================
-- Dados iniciais da aplicacao (carregados automaticamente)
-- Seguro para reexecutar: INSERT IGNORE ignora duplicatas.
-- =============================================================

-- Instituicoes financeiras
INSERT IGNORE INTO instituicao (nome) VALUES
    ('Itau'),
    ('Nubank'),
    ('Santander'),
    ('Bradesco'),
    ('Banco do Brasil'),
    ('Inter'),
    ('Safra'),
    ('Alelo Alimentacao'),
    ('Alelo Refeicao'),
    ('Alelo Multibeneficios'),
    ('Pluxee'),
    ('Ticket'),
    ('Vale Refeicao'),
    ('Vale Alimentacao');

-- Categorias de gastos
INSERT IGNORE INTO categoria (titulo) VALUES
    ('Roupas'),
    ('Comida'),
    ('Mercado'),
    ('Conta de Agua'),
    ('Conta de Luz'),
    ('Transporte'),
    ('Bilhete Unico'),
    ('Salario'),
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


-- =============================================================
-- Dados iniciais da aplicacao (carregados automaticamente)
-- Seguro para reexecutar: INSERT IGNORE ignora duplicatas.
-- =============================================================

-- Instituicoes financeiras
INSERT IGNORE INTO instituicao (nome, is_voucher) VALUES
    ('Itau', false),
    ('Nubank', false),
    ('Santander', false),
    ('Bradesco', false),
    ('Banco do Brasil', false),
    ('Inter', false),
    ('Safra', false),
    ('Alelo Alimentacao', true),
    ('Alelo Refeicao', true),
    ('Alelo Multibeneficios', true),
    ('Pluxee', true),
    ('Ticket', true),
    ('Vale Refeicao', true),
    ('Vale Alimentacao', true);

-- Garante que instituicoes ja existentes (de execucoes anteriores) sejam marcadas como voucher
UPDATE instituicao SET is_voucher = true
WHERE nome IN ('Alelo Alimentacao', 'Alelo Refeicao', 'Alelo Multibeneficios', 'Pluxee', 'Ticket', 'Vale Refeicao', 'Vale Alimentacao');

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
    ('Parcela Empréstimo'),
    ('Outros');


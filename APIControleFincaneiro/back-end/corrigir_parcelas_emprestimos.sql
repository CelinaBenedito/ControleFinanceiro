-- Script para verificar e corrigir parcelas de empréstimos existentes
-- Este script ajusta as datas das parcelas para que a primeira parcela seja no mesmo mês do empréstimo

-- 1. Verificar se há registros de empréstimos
SELECT
    ef.id as emprestimo_id,
    ef.data_evento as data_emprestimo,
    ef.descricao,
    ef.valor
FROM evento_financeiro ef
WHERE ef.tipo = 'Emprestimo'
ORDER BY ef.data_evento DESC;

-- 2. Verificar parcelas (gastos com descrição contendo "Parcela" e "empréstimo")
SELECT
    ef.id,
    ef.data_evento,
    ef.descricao,
    ef.valor,
    ef.tipo,
    ed.titulo_gasto
FROM evento_financeiro ef
LEFT JOIN evento_detalhe ed ON ed.fk_evento = ef.id
WHERE ef.tipo = 'Gasto'
  AND ef.descricao LIKE '%Parcela%empréstimo%'
ORDER BY ef.data_evento;

-- 3. Se quiser corrigir as datas das parcelas existentes (execute com cuidado!)
-- Descomente as linhas abaixo APENAS se tiver certeza

/*
-- Para cada parcela, ajustar a data para ser (i-1) meses após a data do empréstimo
-- Este é um exemplo - você precisaria adaptar para sua situação específica
UPDATE evento_financeiro ef
SET data_evento = DATE_SUB(data_evento, INTERVAL 1 MONTH)
WHERE ef.tipo = 'Gasto'
  AND ef.descricao LIKE '%Parcela%empréstimo%'
  AND ef.data_evento > CURDATE();
*/


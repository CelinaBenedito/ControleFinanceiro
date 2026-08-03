package controle.api.back_end.dto.emprestimo.out;

import java.math.BigDecimal;

/**
 * DTO com resumo dos empréstimos para notificações.
 */
public record EmprestimoResumoDTO(
        Long quantidadePessoasDevem,
        BigDecimal valorTotalAReceber,
        Long quantidadePessoasDevo,
        BigDecimal valorTotalAPagar
) {
}


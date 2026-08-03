package controle.api.back_end.dto.emprestimo.in;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * DTO para registrar um pagamento parcial de empréstimo.
 */
public record EmprestimoPagamentoParcialDTO(
        @NotNull(message = "Valor do pagamento é obrigatório")
        @PositiveOrZero(message = "Valor do pagamento deve ser positivo ou zero")
        BigDecimal valorPago
) {
}


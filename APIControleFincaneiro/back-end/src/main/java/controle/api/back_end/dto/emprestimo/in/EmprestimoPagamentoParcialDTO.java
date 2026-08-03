package controle.api.back_end.dto.emprestimo.in;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmprestimoPagamentoParcialDTO(
        @NotNull(message = "Valor do pagamento \u00e9 obrigat\u00f3rio")
        @PositiveOrZero(message = "Valor do pagamento deve ser positivo ou zero")
        BigDecimal valorPago,

        @NotNull(message = "Institui\u00e7\u00e3o \u00e9 obrigat\u00f3ria")
        Integer instituicaoUsuarioId,

        LocalDate dataPagamento
) {
}


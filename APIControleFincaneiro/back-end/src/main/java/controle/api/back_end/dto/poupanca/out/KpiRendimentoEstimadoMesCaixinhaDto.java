package controle.api.back_end.dto.poupanca.out;

import java.math.BigDecimal;

public record KpiRendimentoEstimadoMesCaixinhaDto(
        BigDecimal rendimentoEstimadoMes,
        String mesReferencia
) {
}


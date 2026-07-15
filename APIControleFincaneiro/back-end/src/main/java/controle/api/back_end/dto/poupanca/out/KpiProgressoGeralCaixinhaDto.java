package controle.api.back_end.dto.poupanca.out;

import java.math.BigDecimal;

public record KpiProgressoGeralCaixinhaDto(
        BigDecimal totalAcumulado,
        BigDecimal totalMetas,
        int percentualProgressoGeral
) {
}


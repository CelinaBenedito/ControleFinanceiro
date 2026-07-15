package controle.api.back_end.dto.poupanca.out;

import java.math.BigDecimal;

public record GraficoProgressoConsolidadoCaixinhaDto(
        int percentualProgressoGeral,
        BigDecimal totalAcumulado,
        BigDecimal totalMetas
) {
}


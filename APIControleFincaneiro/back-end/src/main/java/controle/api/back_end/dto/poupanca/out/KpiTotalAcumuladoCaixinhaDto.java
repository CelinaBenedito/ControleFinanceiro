package controle.api.back_end.dto.poupanca.out;

import java.math.BigDecimal;

public record KpiTotalAcumuladoCaixinhaDto(
        BigDecimal totalAcumulado,
        int quantidadeCaixinhasAtivas
) {
}


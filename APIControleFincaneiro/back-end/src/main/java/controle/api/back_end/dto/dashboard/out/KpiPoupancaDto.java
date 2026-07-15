package controle.api.back_end.dto.dashboard.out;

import java.math.BigDecimal;

public record KpiPoupancaDto(
        String nomeCaixinha,
        BigDecimal valorGuardado,
        BigDecimal valorMeta,
        int percentualMeta,
        BigDecimal valorFaltante,
        String descricao
) {}


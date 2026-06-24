package controle.api.back_end.dto.dashboard.out;

import java.math.BigDecimal;
import java.time.LocalDate;

/** KPI 3 — Maior gasto isolado do período. */
public record MaiorGastoDto(
        String titulo,
        String categoria,
        BigDecimal valor,
        /** % que este gasto representa sobre o total de gastos do período */
        int percentualDoTotal,
        LocalDate data
) {}


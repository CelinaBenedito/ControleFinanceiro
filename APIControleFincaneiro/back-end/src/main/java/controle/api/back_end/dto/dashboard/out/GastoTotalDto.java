package controle.api.back_end.dto.dashboard.out;

import java.math.BigDecimal;

/** KPI 2 — Total de gastos no período e variação vs período anterior. */
public record GastoTotalDto(
        BigDecimal totalGastos,
        BigDecimal totalGastosAnterior,
        /** Positivo = gastou mais; negativo = gastou menos */
        int variacaoPercentual,
        String labelPeriodo,
        String labelPeriodoAnterior
) {}


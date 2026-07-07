package controle.api.back_end.dto.dashboard.out;

import java.math.BigDecimal;

/** KPI 4 — Categoria com maior impacto no período e variação vs anterior. */
public record CategoriaImpactoDto(
        String categoria,
        BigDecimal valorAtual,
        BigDecimal valorAnterior,
        /** Positivo = gastou mais; negativo = gastou menos */
        int variacaoPercentual
) {}


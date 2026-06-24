package controle.api.back_end.dto.dashboard.out;

import java.math.BigDecimal;
import java.util.List;

/** Gráfico 3 — Comparação de gastos entre período atual e anterior (multiple line chart). */
public record ComparacaoPeriodoDto(
        String labelPeriodoAtual,
        String labelPeriodoAnterior,
        /** Ambas as séries têm o mesmo número de pontos, normalizados por posição (Dia 1, Dia 2, …) */
        List<PontoComparacao> dados
) {
    public record PontoComparacao(
            String label,
            BigDecimal valorAtual,
            BigDecimal valorAnterior
    ) {}
}


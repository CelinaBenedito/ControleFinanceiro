package controle.api.back_end.dto.dashboard.out;

import java.math.BigDecimal;
import java.util.List;

/** Gráfico 2 — Gastos agrupados por categoria (stacked bar chart). */
public record CategoriasGraficoDto(
        String labelPeriodo,
        List<CategoriaData> categorias
) {
    public record CategoriaData(
            String nome,
            BigDecimal valorTotal,
            /** % que esta categoria representa sobre o total de gastos do período */
            int percentualDoTotal,
            /** Quantas vezes a categoria apareceu em eventos no período */
            int ocorrencias
    ) {}
}


package controle.api.back_end.dto.dashboard.out;

import java.math.BigDecimal;
import java.util.List;

/** Gráfico 4 — Distribuição de gastos por dia da semana (heat map). */
public record HeatMapDiaSemanaDto(
        String labelPeriodo,
        List<DiaDado> dias
) {
    public record DiaDado(
            String dia,            // "segunda-feira", "terça-feira", ...
            BigDecimal valorTotal,
            /** 0.0 a 1.0 — normalizado pelo dia de maior gasto no período */
            double normalizado
    ) {}
}


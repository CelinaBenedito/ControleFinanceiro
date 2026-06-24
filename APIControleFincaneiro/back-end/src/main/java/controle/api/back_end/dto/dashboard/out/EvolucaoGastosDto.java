package controle.api.back_end.dto.dashboard.out;

import java.math.BigDecimal;
import java.util.List;

/** Gráfico 1 — Evolução dos gastos ao longo do período (line chart). */
public record EvolucaoGastosDto(
        String labelPeriodo,
        /** DIARIO | SEMANAL | MENSAL dependendo do tipo de período */
        String granularidade,
        List<Ponto> dados
) {
    public record Ponto(String label, BigDecimal valor) {}
}


package controle.api.back_end.dto.dashboard.out;

import java.math.BigDecimal;
import java.util.List;

/** Gráfico 1 — Evolução dos gastos e recebimentos ao longo do período. */
public record EvolucaoGastosDto(
        String labelPeriodo,
        /** DIARIO | SEMANAL | MENSAL dependendo do tipo de período */
        String granularidade,
        List<Ponto> dados,
        /** Série de recebimentos (Recebimento + Empréstimo) — mesmos labels que dados */
        List<Ponto> dadosRecebimentos,
        /** Limite mensal total configurado, ajustado para a duração do período selecionado. Null se não configurado. */
        BigDecimal limitePeriodo
) {
    public record Ponto(String label, BigDecimal valor) {}
}

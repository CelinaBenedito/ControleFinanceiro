package controle.api.back_end.dto.dashboard.out;

import java.math.BigDecimal;
import java.util.List;

/**
 * Gráfico 5 — Fluxo de entradas e saídas (Sankey/Sankey-like).
 * <p>
 * Estrutura:
 * <ul>
 *   <li>Nós tipo {@code INSTITUICAO}: bancos/carteiras onde o dinheiro circulou</li>
 *   <li>Nós tipo {@code CATEGORIA}: onde os gastos foram realizados</li>
 *   <li>Nós tipo {@code SAIDA}: saídas especiais (Transferência, Poupança)</li>
 *   <li>Links: representam o fluxo de valor entre os nós</li>
 * </ul>
 */
public record FluxoFinanceiroDto(
        String labelPeriodo,
        List<No> nos,
        List<Link> links
) {
    public record No(
            String id,
            String label,
            /** INSTITUICAO | CATEGORIA | SAIDA */
            String tipo,
            BigDecimal totalEntrada,
            BigDecimal totalSaida
    ) {}

    public record Link(
            String de,
            String para,
            BigDecimal valor
    ) {}
}


package controle.api.back_end.dto.dashboard.out;

import java.math.BigDecimal;
import java.util.List;

/** Painel — Limite de gastos por instituição no período selecionado. */
public record LimitesInstituicaoDto(
        String labelPeriodo,
        List<InstituicaoLimiteData> instituicoes
) {
    public record InstituicaoLimiteData(
            Integer instituicaoUsuarioId,
            String nome,
            BigDecimal gastoAtual,
            /** Limite mensal configurado, já ajustado para a duração do período. Null se não configurado. */
            BigDecimal limite,
            /** Percentual do limite já consumido no período. Null se não houver limite configurado. */
            Integer percentualConsumido,
            /** SEM_LIMITE | NORMAL | ATENCAO | EXCEDIDO */
            String status
    ) {}
}

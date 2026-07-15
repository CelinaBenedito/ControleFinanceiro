package controle.api.back_end.dto.dashboard.out;

import controle.api.back_end.model.dashboard.NivelSaudeFinanceira;

public record KpiSaudeFinanceiraDto(
        int pontuacao,
        NivelSaudeFinanceira nivel,
        String descricao,
        String labelPeriodo
) {}


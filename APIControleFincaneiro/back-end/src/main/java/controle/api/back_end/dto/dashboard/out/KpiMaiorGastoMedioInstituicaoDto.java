package controle.api.back_end.dto.dashboard.out;

import java.math.BigDecimal;

public record KpiMaiorGastoMedioInstituicaoDto(
        String nomeInstituicao,
        BigDecimal valorMedioTransacao,
        int totalTransacoes,
        String labelPeriodo
) {}


package controle.api.back_end.dto.dashboard.out;

import java.math.BigDecimal;

public record KpiEmprestimoDto(
        boolean temEmprestimoAtivo,
        BigDecimal valorTotal,
        BigDecimal valorPago,
        BigDecimal valorRestante,
        BigDecimal jurosPagos,
        int parcelasTotal,
        int parcelasPagas,
        int parcelasFaltantes,
        int percentualQuitado,
        String nomeInstituicao
) {}


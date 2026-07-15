package controle.api.back_end.dto.instituicao.out;

import java.math.BigDecimal;

public record ResumoInstituicaoDto(
        Integer instUsuarioId,
        String nomeInstituicao,
        int quantidadeTransacoes,
        BigDecimal saldoDisponivel,
        BigDecimal totalCredito,
        BigDecimal totalDebito,
        BigDecimal limiteCredito,
        int percentualCreditoUtilizado,
        int parcelamentosAtivos,
        Double taxaJuros,
        boolean temCredito
) {}


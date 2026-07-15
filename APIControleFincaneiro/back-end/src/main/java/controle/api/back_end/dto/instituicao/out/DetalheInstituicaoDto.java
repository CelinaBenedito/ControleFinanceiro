package controle.api.back_end.dto.instituicao.out;

import java.math.BigDecimal;
import java.util.List;

public record DetalheInstituicaoDto(
        Integer instUsuarioId,
        String nomeInstituicao,
        BigDecimal limiteCredito,
        Double taxaJuros,
        List<DistribuicaoMovimentoDto> distribuicaoPorMovimento
) {
    public record DistribuicaoMovimentoDto(
            String tipoMovimento,
            BigDecimal valorTotal
    ) {}
}


package controle.api.back_end.dto.dashboard.out;

public record KpiInstituicaoMaisUtilizadaDto(
        String nomeInstituicao,
        int totalTransacoes,
        double percentualVantagem,
        String labelPeriodo
) {}


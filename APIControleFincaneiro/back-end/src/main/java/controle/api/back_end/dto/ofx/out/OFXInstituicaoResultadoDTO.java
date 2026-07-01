package controle.api.back_end.dto.ofx.out;

import java.util.List;

/**
 * Resultado de sincronização de uma única instituição dentro de um lote.
 */
public record OFXInstituicaoResultadoDTO(
        Integer instituicaoUsuarioId,
        boolean sucesso,
        String mensagem,
        int transacoesImportadas,
        int transacoesDuplicadas,
        List<String> erros
) {}


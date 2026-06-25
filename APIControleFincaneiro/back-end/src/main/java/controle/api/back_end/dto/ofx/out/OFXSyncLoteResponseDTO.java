package controle.api.back_end.dto.ofx.out;

import java.util.List;

/**
 * Resposta do endpoint POST /ofx/sync/usuario/{userId}.
 * Contém o resultado de cada instituição processada no lote.
 */
public record OFXSyncLoteResponseDTO(
        boolean sucesso,
        String mensagem,
        int totalInstituicoes,
        int totalTransacoesImportadas,
        int totalTransacoesDuplicadas,
        List<OFXInstituicaoResultadoDTO> resultados
) {}


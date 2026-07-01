package controle.api.back_end.dto.ofx.out;

import java.util.List;

/**
 * Resposta do endpoint POST /ofx/sync para o frontend.
 */
public record OFXSyncResponseDTO(
        boolean success,
        String message,
        int transacoesImportadas,
        int transacoesDuplicadas,
        List<String> erros
) {}


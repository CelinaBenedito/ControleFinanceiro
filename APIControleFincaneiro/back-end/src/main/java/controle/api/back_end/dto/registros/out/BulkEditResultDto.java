package controle.api.back_end.dto.registros.out;

import java.util.List;
import java.util.UUID;

/**
 * Resposta de uma operação de edição ou exclusão em lote.
 * Informa quantos registros foram processados com sucesso e quais falharam.
 */
public class BulkEditResultDto {

    private int totalProcessados;
    private int totalSucesso;
    private int totalErros;

    /** Registros que foram atualizados com sucesso. */
    private List<RegistroResponseDto> atualizados;

    /** IDs e mensagens de erro dos registros que falharam. */
    private List<BulkErroItemDto> erros;

    public BulkEditResultDto(int totalProcessados, int totalSucesso, int totalErros,
                              List<RegistroResponseDto> atualizados,
                              List<BulkErroItemDto> erros) {
        this.totalProcessados = totalProcessados;
        this.totalSucesso     = totalSucesso;
        this.totalErros       = totalErros;
        this.atualizados      = atualizados;
        this.erros            = erros;
    }

    public int getTotalProcessados() { return totalProcessados; }
    public int getTotalSucesso()     { return totalSucesso; }
    public int getTotalErros()       { return totalErros; }
    public List<RegistroResponseDto> getAtualizados() { return atualizados; }
    public List<BulkErroItemDto> getErros()           { return erros; }

    // ─── Item de erro ────────────────────────────────────────────────────────

    public record BulkErroItemDto(UUID id, String erro) {}
}


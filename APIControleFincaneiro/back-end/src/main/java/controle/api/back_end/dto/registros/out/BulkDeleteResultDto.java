package controle.api.back_end.dto.registros.out;

import java.util.List;
import java.util.UUID;

/**
 * Resposta de uma operação de exclusão em lote.
 */
public class BulkDeleteResultDto {

    private int totalSolicitados;
    private int totalRemovidos;
    private int totalErros;

    /** IDs removidos com sucesso. */
    private List<UUID> removidos;

    /** IDs que falharam e o motivo. */
    private List<BulkErroItemDto> erros;

    public BulkDeleteResultDto(int totalSolicitados, int totalRemovidos, int totalErros,
                                List<UUID> removidos, List<BulkErroItemDto> erros) {
        this.totalSolicitados = totalSolicitados;
        this.totalRemovidos   = totalRemovidos;
        this.totalErros       = totalErros;
        this.removidos        = removidos;
        this.erros            = erros;
    }

    public int getTotalSolicitados() { return totalSolicitados; }
    public int getTotalRemovidos()   { return totalRemovidos; }
    public int getTotalErros()       { return totalErros; }
    public List<UUID> getRemovidos() { return removidos; }
    public List<BulkErroItemDto> getErros() { return erros; }

    public record BulkErroItemDto(UUID id, String erro) {}
}


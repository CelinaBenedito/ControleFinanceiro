package controle.api.back_end.dto.registros.in;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Payload para edição em lote com as <strong>mesmas alterações</strong> aplicadas a vários registros.
 *
 * <p>Exemplo de uso:
 * <pre>{@code
 * PATCH /registros/lote
 * {
 *   "ids": ["uuid-1", "uuid-2", "uuid-3"],
 *   "alteracoes": {
 *     "tituloGasto": "Supermercado",
 *     "categoriaIds": [3, 5]
 *   }
 * }
 * }</pre>
 */
public class BulkEditPatchDto {

    /** IDs dos registros a serem alterados (mínimo 1, máximo 200). */
    @NotEmpty(message = "A lista de IDs não pode estar vazia.")
    @Size(max = 200, message = "Máximo de 200 registros por operação em lote.")
    private List<@NotNull UUID> ids;

    /** Campos a serem atualizados. Apenas os não-nulos são aplicados. */
    @NotNull(message = "O objeto 'alteracoes' é obrigatório.")
    private BulkAlteracoesDto alteracoes;

    public List<UUID> getIds() { return ids; }
    public void setIds(List<UUID> ids) { this.ids = ids; }

    public BulkAlteracoesDto getAlteracoes() { return alteracoes; }
    public void setAlteracoes(BulkAlteracoesDto alteracoes) { this.alteracoes = alteracoes; }
}


package controle.api.back_end.dto.registros.in;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Item de edição individual dentro de uma operação em lote.
 * Cada item contém o ID do registro e os dados atualizados para ele.
 *
 * <p>Os sub-objetos ({@code financeiro}, {@code instituicao}, {@code detalhe}) são opcionais.
 * Se {@code null}, aquela parte do registro <strong>não é alterada</strong>.
 */
public class RegistroCompletoEditDto {

    /** ID do registro a ser editado. */
    @NotNull(message = "O id do registro é obrigatório.")
    private UUID id;

    /**
     * Dados atualizados do evento financeiro (tipo, valor, data, descrição).
     * {@code null} → não altera o evento financeiro.
     */
    private EventoFinanceiroCreateDto financeiro;

    /**
     * Nova lista de meios de pagamento.
     * {@code null} → não altera as instituições.
     * {@code []} → remove todas as instituições vinculadas.
     */
    private List<EventoInstituicaoCreateDto> instituicao;

    /**
     * Dados atualizados do detalhe (título e categorias).
     * {@code null} → não altera o detalhe.
     */
    private EventoDetalheCreateDto detalhe;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public EventoFinanceiroCreateDto getFinanceiro() { return financeiro; }
    public void setFinanceiro(EventoFinanceiroCreateDto financeiro) { this.financeiro = financeiro; }

    public List<EventoInstituicaoCreateDto> getInstituicao() { return instituicao; }
    public void setInstituicao(List<EventoInstituicaoCreateDto> instituicao) { this.instituicao = instituicao; }

    public EventoDetalheCreateDto getDetalhe() { return detalhe; }
    public void setDetalhe(EventoDetalheCreateDto detalhe) { this.detalhe = detalhe; }
}


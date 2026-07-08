package controle.api.back_end.dto.registros.in;

import controle.api.back_end.model.eventoFinanceiro.Tipo;
import controle.api.back_end.model.eventoFinanceiro.TipoMovimento;

import java.time.LocalDate;
import java.util.List;

/**
 * Representa alterações parciais a serem aplicadas em lote.
 * Campos {@code null} são ignorados — apenas os preenchidos serão atualizados.
 *
 * <p>Exceção: {@code categoriaIds} com lista vazia ({@code []}) remove todas as categorias;
 * {@code null} mantém as categorias existentes sem alteração.
 */
public class BulkAlteracoesDto {

    /** Novo tipo do evento (null = não alterar). */
    private Tipo tipo;

    /** Nova data do evento (null = não alterar). */
    private LocalDate dataEvento;

    /** Nova descrição (null = não alterar). */
    private String descricao;

    /** Novo valor do evento (null = não alterar). */
    private Double valor;

    /** Novo tipo de movimento da instituição (null = não alterar). */
    private TipoMovimento tipoMovimento;

    /** ID da nova instituição do usuário (null = não alterar). */
    private Integer instituicaoUsuarioId;

    /** Novo número de parcelas (null = não alterar). */
    private Integer parcelas;

    /** Novo título do gasto/detalhe (null = não alterar). */
    private String tituloGasto;

    /**
     * Novos IDs de categorias:
     * <ul>
     *   <li>{@code null}  → não altera as categorias</li>
     *   <li>{@code []}    → remove todas as categorias</li>
     *   <li>{@code [1,2]} → substitui pelas categorias informadas</li>
     * </ul>
     */
    private List<Integer> categoriaIds;

    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }

    public LocalDate getDataEvento() { return dataEvento; }
    public void setDataEvento(LocalDate dataEvento) { this.dataEvento = dataEvento; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public TipoMovimento getTipoMovimento() { return tipoMovimento; }
    public void setTipoMovimento(TipoMovimento tipoMovimento) { this.tipoMovimento = tipoMovimento; }

    public Integer getInstituicaoUsuarioId() { return instituicaoUsuarioId; }
    public void setInstituicaoUsuarioId(Integer instituicaoUsuarioId) { this.instituicaoUsuarioId = instituicaoUsuarioId; }

    public Integer getParcelas() { return parcelas; }
    public void setParcelas(Integer parcelas) { this.parcelas = parcelas; }

    public String getTituloGasto() { return tituloGasto; }
    public void setTituloGasto(String tituloGasto) { this.tituloGasto = tituloGasto; }

    public List<Integer> getCategoriaIds() { return categoriaIds; }
    public void setCategoriaIds(List<Integer> categoriaIds) { this.categoriaIds = categoriaIds; }
}


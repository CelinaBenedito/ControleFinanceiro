package controle.api.back_end.dto.poupanca.out;

import controle.api.back_end.model.poupanca.TipoRendimento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Resposta rica de uma caixinha, incluindo todos os dados configurados
 * pelo usuário e todos os valores calculados pela API.
 */
public class CaixinhaResponseDTO {

    // ── Dados básicos ─────────────────────────────────────────────────────────
    private UUID id;
    private String nome;
    private String descricao;
    private Boolean isAtiva;
    private Boolean isCompartilhada;
    private LocalDate dataCriacao;
    private LocalDate dataEncerramento;
    private List<InstituicaoDTO> instituicoes;

    // ── Meta ─────────────────────────────────────────────────────────────────
    private BigDecimal valorMeta;
    private LocalDate dataPrazo;

    // ── Rendimento configurado ────────────────────────────────────────────────
    private TipoRendimento tipoRendimento;
    /** Percentual do CDI/SELIC (ex.: "100% do CDI"). */
    private Double percentualRendimento;
    /** Taxa anual fixa informada pelo usuário (PREFIXADO/PERSONALIZADO). */
    private Double taxaAnualPersonalizada;
    /** Taxa de referência atual do indexador (ex.: CDI de 10,40% a.a.). */
    private Double taxaReferenciaAtual;

    // ── Calculados em tempo real ──────────────────────────────────────────────
    /** Soma de todos os aportes já realizados nesta caixinha. */
    private BigDecimal valorAtual;
    /** Quanto ainda falta para atingir a meta. max(0, meta - atual). */
    private BigDecimal faltaParaMeta;
    /** Percentual da meta já atingido. (atual / meta) * 100. */
    private Double percentualAtingido;
    /** Meses entre hoje e o prazo. Nulo se sem prazo. */
    private Integer mesesRestantes;
    /**
     * Quanto o usuário precisa depositar por mês (a partir de hoje)
     * para atingir a meta no prazo, considerando o rendimento.
     * Nulo se sem prazo ou meta já atingida.
     */
    private BigDecimal aporteMensalSugerido;
    /**
     * Montante projetado no prazo considerando apenas o saldo atual
     * com o rendimento configurado (sem novos aportes).
     */
    private BigDecimal montanteProjetadoSemAportes;
    /**
     * Montante projetado no prazo considerando o saldo atual +
     * {@link #aporteMensalSugerido} todo mês até o prazo.
     */
    private BigDecimal montanteProjetadoComAportes;
    /** Taxa mensal efetiva usada nas projeções (em decimal, ex.: 0.00829). */
    private Double taxaMensalEfetiva;
    /** Se verdadeiro, a meta é atingível com o aporte sugerido. */
    private Boolean metaAlcancavel;

    // ── DTO interno de instituição ────────────────────────────────────────────
    public static class InstituicaoDTO {
        private Integer id;
        private String nome;
        /** Soma dos aportes feitos nesta instituição para esta caixinha. */
        private BigDecimal valorAportado;

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public BigDecimal getValorAportado() { return valorAportado; }
        public void setValorAportado(BigDecimal valorAportado) { this.valorAportado = valorAportado; }
    }

    // ── Getters e Setters ────────────────────────────────────────────────────
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Boolean getIsAtiva() { return isAtiva; }
    public void setIsAtiva(Boolean isAtiva) { this.isAtiva = isAtiva; }
    public Boolean getIsCompartilhada() { return isCompartilhada; }
    public void setIsCompartilhada(Boolean isCompartilhada) { this.isCompartilhada = isCompartilhada; }
    public LocalDate getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDate dataCriacao) { this.dataCriacao = dataCriacao; }
    public LocalDate getDataEncerramento() { return dataEncerramento; }
    public void setDataEncerramento(LocalDate dataEncerramento) { this.dataEncerramento = dataEncerramento; }
    public List<InstituicaoDTO> getInstituicoes() { return instituicoes; }
    public void setInstituicoes(List<InstituicaoDTO> instituicoes) { this.instituicoes = instituicoes; }
    public BigDecimal getValorMeta() { return valorMeta; }
    public void setValorMeta(BigDecimal valorMeta) { this.valorMeta = valorMeta; }
    public LocalDate getDataPrazo() { return dataPrazo; }
    public void setDataPrazo(LocalDate dataPrazo) { this.dataPrazo = dataPrazo; }
    public TipoRendimento getTipoRendimento() { return tipoRendimento; }
    public void setTipoRendimento(TipoRendimento tipoRendimento) { this.tipoRendimento = tipoRendimento; }
    public Double getPercentualRendimento() { return percentualRendimento; }
    public void setPercentualRendimento(Double percentualRendimento) { this.percentualRendimento = percentualRendimento; }
    public Double getTaxaAnualPersonalizada() { return taxaAnualPersonalizada; }
    public void setTaxaAnualPersonalizada(Double taxaAnualPersonalizada) { this.taxaAnualPersonalizada = taxaAnualPersonalizada; }
    public Double getTaxaReferenciaAtual() { return taxaReferenciaAtual; }
    public void setTaxaReferenciaAtual(Double taxaReferenciaAtual) { this.taxaReferenciaAtual = taxaReferenciaAtual; }
    public BigDecimal getValorAtual() { return valorAtual; }
    public void setValorAtual(BigDecimal valorAtual) { this.valorAtual = valorAtual; }
    public BigDecimal getFaltaParaMeta() { return faltaParaMeta; }
    public void setFaltaParaMeta(BigDecimal faltaParaMeta) { this.faltaParaMeta = faltaParaMeta; }
    public Double getPercentualAtingido() { return percentualAtingido; }
    public void setPercentualAtingido(Double percentualAtingido) { this.percentualAtingido = percentualAtingido; }
    public Integer getMesesRestantes() { return mesesRestantes; }
    public void setMesesRestantes(Integer mesesRestantes) { this.mesesRestantes = mesesRestantes; }
    public BigDecimal getAporteMensalSugerido() { return aporteMensalSugerido; }
    public void setAporteMensalSugerido(BigDecimal aporteMensalSugerido) { this.aporteMensalSugerido = aporteMensalSugerido; }
    public BigDecimal getMontanteProjetadoSemAportes() { return montanteProjetadoSemAportes; }
    public void setMontanteProjetadoSemAportes(BigDecimal montanteProjetadoSemAportes) { this.montanteProjetadoSemAportes = montanteProjetadoSemAportes; }
    public BigDecimal getMontanteProjetadoComAportes() { return montanteProjetadoComAportes; }
    public void setMontanteProjetadoComAportes(BigDecimal montanteProjetadoComAportes) { this.montanteProjetadoComAportes = montanteProjetadoComAportes; }
    public Double getTaxaMensalEfetiva() { return taxaMensalEfetiva; }
    public void setTaxaMensalEfetiva(Double taxaMensalEfetiva) { this.taxaMensalEfetiva = taxaMensalEfetiva; }
    public Boolean getMetaAlcancavel() { return metaAlcancavel; }
    public void setMetaAlcancavel(Boolean metaAlcancavel) { this.metaAlcancavel = metaAlcancavel; }
}


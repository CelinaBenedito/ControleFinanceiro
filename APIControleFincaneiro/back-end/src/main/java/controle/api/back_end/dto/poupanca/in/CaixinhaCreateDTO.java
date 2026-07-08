package controle.api.back_end.dto.poupanca.in;

import controle.api.back_end.model.poupanca.TipoRendimento;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Payload para criar uma nova caixinha. */
public class CaixinhaCreateDTO {

    @NotNull
    private UUID usuarioId;

    @NotBlank
    @Size(max = 100)
    private String nome;

    @Size(max = 500)
    private String descricao;

    // ── Meta ─────────────────────────────────────────────────────────────────
    @Positive
    private BigDecimal valorMeta;

    /** Data-limite para atingir a meta. Opcional. */
    private LocalDate dataPrazo;

    // ── Rendimento ───────────────────────────────────────────────────────────
    @NotNull
    private TipoRendimento tipoRendimento;

    /**
     * Percentual do CDI/SELIC que rende (ex.: 100.0 = 100% do CDI).
     * Obrigatório se tipoRendimento = CDI ou SELIC.
     */
    @PositiveOrZero
    private Double percentualRendimento;

    /**
     * Taxa anual fixa (ex.: 12.5 = 12,5% a.a.).
     * Obrigatório se tipoRendimento = PREFIXADO ou PERSONALIZADO.
     */
    @PositiveOrZero
    private Double taxaAnualPersonalizada;

    /**
     * Taxa de referência atual do indexador em % a.a. (ex.: 10.40 para CDI de 10,40%).
     * Necessária para que a API calcule projeções.
     */
    @PositiveOrZero
    private Double taxaReferenciaAtual;

    // ── Instituições ─────────────────────────────────────────────────────────
    /**
     * IDs das InstituicoesUsuario vinculadas.
     * Uma caixinha simples tem 1 instituição.
     * Uma meta compartilhada tem 2 ou mais.
     */
    @NotEmpty
    private List<Integer> instituicaoUsuarioIds;

    /** Se true, a meta é compartilhada entre todas as instituições informadas. */
    private Boolean isCompartilhada = false;

    // ── Getters e Setters ────────────────────────────────────────────────────
    public UUID getUsuarioId() { return usuarioId; }
    public void setUsuarioId(UUID usuarioId) { this.usuarioId = usuarioId; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

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

    public List<Integer> getInstituicaoUsuarioIds() { return instituicaoUsuarioIds; }
    public void setInstituicaoUsuarioIds(List<Integer> instituicaoUsuarioIds) { this.instituicaoUsuarioIds = instituicaoUsuarioIds; }

    public Boolean getIsCompartilhada() { return isCompartilhada; }
    public void setIsCompartilhada(Boolean isCompartilhada) { this.isCompartilhada = isCompartilhada; }
}


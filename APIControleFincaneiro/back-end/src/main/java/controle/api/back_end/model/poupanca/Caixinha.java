package controle.api.back_end.model.poupanca;

import controle.api.back_end.model.usuario.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Representa uma "caixinha" (caixinha/poupança separada) do usuário.
 *
 * <p>Uma caixinha é um objetivo de poupança com:
 * <ul>
 *   <li>Nome e descrição livre (ex.: "Viagem para o Japão", "Notebook novo")</li>
 *   <li>Meta de valor e prazo para alcançá-la</li>
 *   <li>Rendimento configurável (CDI, SELIC, Poupança, Prefixado ou Personalizado)</li>
 *   <li>Vinculo a uma ou mais instituições (suporte a meta compartilhada)</li>
 * </ul>
 *
 * <p>Os aportes são registrados como {@code EventoFinanceiro} do tipo {@code Poupanca}
 * referenciando esta caixinha.
 */
@Entity
public class Caixinha {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @NotNull
    private Usuario usuario;

    @NotBlank
    @Size(max = 100)
    private String nome;

    @Size(max = 500)
    private String descricao;

    // ── Meta ─────────────────────────────────────────────────────────────────

    /** Valor que o usuário deseja acumular nesta caixinha. */
    @Positive
    private BigDecimal valorMeta;

    /** Data-limite para atingir a meta. Nulo = sem prazo definido. */
    private LocalDate dataPrazo;

    // ── Rendimento ───────────────────────────────────────────────────────────

    /**
     * Tipo de indexador do rendimento.
     * Para CDI e SELIC, usa-se {@link #percentualRendimento} (ex.: 100 % do CDI).
     * Para PREFIXADO e PERSONALIZADO, usa-se {@link #taxaAnualPersonalizada}.
     */
    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false)
    private TipoRendimento tipoRendimento;

    /**
     * Percentual do indexador (CDI ou SELIC) que a caixinha rende.
     * Ex.: 100.0 = 100 % do CDI, 110.5 = 110,5 % do CDI.
     * Ignorado para PREFIXADO e PERSONALIZADO.
     */
    @PositiveOrZero
    private Double percentualRendimento;

    /**
     * Taxa anual em % informada pelo usuário.
     * Usada quando {@link #tipoRendimento} = PREFIXADO ou PERSONALIZADO.
     * Ex.: 12.0 = 12 % a.a.
     */
    @PositiveOrZero
    private Double taxaAnualPersonalizada;

    /**
     * Taxa de referência atual do indexador (CDI ou SELIC) em % a.a.,
     * informada manualmente pelo usuário.
     * Ex.: 10.40 = taxa CDI/SELIC de 10,40 % a.a. na data do último ajuste.
     */
    @PositiveOrZero
    private Double taxaReferenciaAtual;

    // ── Configurações gerais ─────────────────────────────────────────────────

    /**
     * Quando {@code true}, a meta é distribuída entre múltiplas instituições
     * (a soma dos aportes em todas elas forma o saldo total da caixinha).
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean isCompartilhada = false;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean isAtiva = true;

    @Column(nullable = false)
    private LocalDate dataCriacao;

    /** Preenchido quando a caixinha é encerrada (meta atingida ou pelo usuário). */
    private LocalDate dataEncerramento;

    // ── Associações ──────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "caixinha", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CaixinhaInstituicao> caixinhaInstituicoes;

    // ── Getters e Setters ────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

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

    public Boolean getIsCompartilhada() { return isCompartilhada; }
    public void setIsCompartilhada(Boolean isCompartilhada) { this.isCompartilhada = isCompartilhada; }

    public Boolean getIsAtiva() { return isAtiva; }
    public void setIsAtiva(Boolean isAtiva) { this.isAtiva = isAtiva; }

    public LocalDate getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDate dataCriacao) { this.dataCriacao = dataCriacao; }

    public LocalDate getDataEncerramento() { return dataEncerramento; }
    public void setDataEncerramento(LocalDate dataEncerramento) { this.dataEncerramento = dataEncerramento; }

    public List<CaixinhaInstituicao> getCaixinhaInstituicoes() { return caixinhaInstituicoes; }
    public void setCaixinhaInstituicoes(List<CaixinhaInstituicao> caixinhaInstituicoes) { this.caixinhaInstituicoes = caixinhaInstituicoes; }
}


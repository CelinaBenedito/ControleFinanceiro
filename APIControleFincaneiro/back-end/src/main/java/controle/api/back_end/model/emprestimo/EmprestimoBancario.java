package controle.api.back_end.model.emprestimo;
import controle.api.back_end.model.usuario.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
@Entity
@Table(name = "emprestimo_bancario")
public class EmprestimoBancario {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(optional = false)
    @NotNull
    private Usuario usuario;
    @NotBlank @Size(max = 150)
    @Column(nullable = false)
    private String bancoNome;
    @Enumerated(EnumType.STRING)
    @NotNull @Column(nullable = false)
    private ModalidadeEmprestimoBancario modalidade;
    @NotNull @Positive
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorPrincipal;
    /** Taxa de juros ao mes em % (ex: 1.99 = 1,99% a.m.) */
    @NotNull @PositiveOrZero
    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal taxaJurosMensal;
    @NotNull @Min(1)
    @Column(nullable = false)
    private Integer totalParcelas;
    @Column(nullable = false)
    private Integer parcelasPagas = 0;
    /** Valor de cada parcela calculado via tabela Price */
    @NotNull @Positive
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorParcela;
    @Column
    private LocalDate dataContratacao;
    @Column
    private LocalDate dataPrimeiraParcela;
    @Column
    private Integer instituicaoUsuarioId;
    @Size(max = 1000)
    private String observacoes;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusEmprestimoBancario status = StatusEmprestimoBancario.ATIVO;
    @Column(nullable = false)
    private LocalDateTime dataCriacao;
    @Column
    private LocalDateTime dataQuitacao;
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getBancoNome() { return bancoNome; }
    public void setBancoNome(String bancoNome) { this.bancoNome = bancoNome; }
    public ModalidadeEmprestimoBancario getModalidade() { return modalidade; }
    public void setModalidade(ModalidadeEmprestimoBancario modalidade) { this.modalidade = modalidade; }
    public BigDecimal getValorPrincipal() { return valorPrincipal; }
    public void setValorPrincipal(BigDecimal valorPrincipal) { this.valorPrincipal = valorPrincipal; }
    public BigDecimal getTaxaJurosMensal() { return taxaJurosMensal; }
    public void setTaxaJurosMensal(BigDecimal taxaJurosMensal) { this.taxaJurosMensal = taxaJurosMensal; }
    public Integer getTotalParcelas() { return totalParcelas; }
    public void setTotalParcelas(Integer totalParcelas) { this.totalParcelas = totalParcelas; }
    public Integer getParcelasPagas() { return parcelasPagas; }
    public void setParcelasPagas(Integer parcelasPagas) { this.parcelasPagas = parcelasPagas; }
    public BigDecimal getValorParcela() { return valorParcela; }
    public void setValorParcela(BigDecimal valorParcela) { this.valorParcela = valorParcela; }
    public LocalDate getDataContratacao() { return dataContratacao; }
    public void setDataContratacao(LocalDate dataContratacao) { this.dataContratacao = dataContratacao; }
    public LocalDate getDataPrimeiraParcela() { return dataPrimeiraParcela; }
    public void setDataPrimeiraParcela(LocalDate dataPrimeiraParcela) { this.dataPrimeiraParcela = dataPrimeiraParcela; }
    public Integer getInstituicaoUsuarioId() { return instituicaoUsuarioId; }
    public void setInstituicaoUsuarioId(Integer instituicaoUsuarioId) { this.instituicaoUsuarioId = instituicaoUsuarioId; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public StatusEmprestimoBancario getStatus() { return status; }
    public void setStatus(StatusEmprestimoBancario status) { this.status = status; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
    public LocalDateTime getDataQuitacao() { return dataQuitacao; }
    public void setDataQuitacao(LocalDateTime dataQuitacao) { this.dataQuitacao = dataQuitacao; }
}
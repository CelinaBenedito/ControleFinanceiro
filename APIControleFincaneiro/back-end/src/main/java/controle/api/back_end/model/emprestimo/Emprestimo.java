package controle.api.back_end.model.emprestimo;

import controle.api.back_end.model.usuario.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa um empréstimo feito pelo usuário ou recebido por ele.
 *
 * <p>Um empréstimo pode ser:
 * <ul>
 *   <li>Dinheiro ou cartão de crédito emprestado para alguém (EMPRESTEI)</li>
 *   <li>Dinheiro ou cartão pedido emprestado de alguém (PEDI_EMPRESTADO)</li>
 * </ul>
 *
 * <p>O sistema permite controlar:
 * <ul>
 *   <li>Nome da pessoa ou grupo envolvido</li>
 *   <li>Valor total do empréstimo</li>
 *   <li>Valor já pago (para pagamentos parciais)</li>
 *   <li>Data de previsão de pagamento</li>
 *   <li>Notas/observações adicionais</li>
 * </ul>
 */
@Entity
public class Emprestimo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @NotNull
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false)
    private TipoEmprestimo tipo;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false)
    private StatusEmprestimo status = StatusEmprestimo.PENDENTE;

    /**
     * Nome da pessoa ou grupo envolvido no empréstimo.
     * Ex: "João Silva", "Grupo da faculdade", "Maria e Pedro"
     */
    @NotBlank
    @Size(max = 150)
    @Column(nullable = false)
    private String pessoaOuGrupo;

    /**
     * Valor total do empréstimo.
     */
    @NotNull
    @Positive
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorTotal;

    /**
     * Valor já pago/recebido até o momento.
     */
    @PositiveOrZero
    @Column(precision = 15, scale = 2, columnDefinition = "DECIMAL(15,2) DEFAULT 0.00")
    private BigDecimal valorPago = BigDecimal.ZERO;

    /**
     * Data em que o empréstimo foi realizado (pode ser retroativa).
     */
    private LocalDate dataEmprestimo;

    /**
     * Data prevista para pagamento/recebimento.
     */
    private LocalDate dataPrevisao;

    /**
     * Data em que o empréstimo foi registrado.
     */
    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    /**
     * Data em que o empréstimo foi quitado.
     */
    private LocalDateTime dataQuitacao;

    /**
     * Notas/observações sobre o empréstimo.
     */
    @Size(max = 1000)
    private String observacoes;

    /**
     * ID da InstituicaoUsuario vinculada ao criar o empréstimo.
     * Usada como padrão na quitação caso não seja informada outra.
     */
    @Column
    private Integer instituicaoUsuarioId;

    // ── Getters e Setters ────────────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public TipoEmprestimo getTipo() {
        return tipo;
    }

    public void setTipo(TipoEmprestimo tipo) {
        this.tipo = tipo;
    }

    public StatusEmprestimo getStatus() {
        return status;
    }

    public void setStatus(StatusEmprestimo status) {
        this.status = status;
    }

    public String getPessoaOuGrupo() {
        return pessoaOuGrupo;
    }

    public void setPessoaOuGrupo(String pessoaOuGrupo) {
        this.pessoaOuGrupo = pessoaOuGrupo;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public BigDecimal getValorPago() {
        return valorPago;
    }

    public void setValorPago(BigDecimal valorPago) {
        this.valorPago = valorPago;
    }

    public LocalDate getDataPrevisao() {
        return dataPrevisao;
    }

    public void setDataPrevisao(LocalDate dataPrevisao) {
        this.dataPrevisao = dataPrevisao;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataQuitacao() {
        return dataQuitacao;
    }

    public void setDataQuitacao(LocalDateTime dataQuitacao) {
        this.dataQuitacao = dataQuitacao;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public Integer getInstituicaoUsuarioId() {
        return instituicaoUsuarioId;
    }

    public void setInstituicaoUsuarioId(Integer instituicaoUsuarioId) {
        this.instituicaoUsuarioId = instituicaoUsuarioId;
    }

    public LocalDate getDataEmprestimo() {
        return dataEmprestimo;
    }

    public void setDataEmprestimo(LocalDate dataEmprestimo) {
        this.dataEmprestimo = dataEmprestimo;
    }
}


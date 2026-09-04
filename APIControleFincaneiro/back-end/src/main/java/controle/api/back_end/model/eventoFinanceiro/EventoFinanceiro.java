package controle.api.back_end.model.eventoFinanceiro;

import controle.api.back_end.model.emprestimo.EmprestimoBancario;
import controle.api.back_end.model.poupanca.Caixinha;
import controle.api.back_end.model.usuario.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
public class EventoFinanceiro {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @NotNull
    private Usuario usuario;

    /**
     * Mapeado explicitamente como VARCHAR (e não como ENUM nativo do banco) para que
     * novos valores adicionados ao enum {@link Tipo} (ex.: Resgate) não exijam uma
     * migração manual do tipo de coluna no MariaDB/MySQL.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 30)
    @NotNull
    private Tipo tipo;

    @NotNull
    @PositiveOrZero
    private Double valor;

    @Size(max = 500)
    private String descricao;

    @NotNull
    private LocalDate dataEvento;

    private Double taxaRendimento;

    private Integer tempoAplicacao;

    private Integer tempoProjecao;

    @OneToMany(mappedBy = "eventoFinanceiro", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventoInstituicao> eventoInstituicoes;

    @OneToOne(mappedBy = "eventoFinanceiro", cascade = CascadeType.ALL, orphanRemoval = true)
    private EventoDetalhe eventoDetalhe;

    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime dataRegistro;

    /**
     * Empréstimo bancário ao qual este evento pertence.
     * Preenchido para eventos gerados por um empréstimo bancário.
     */
    @ManyToOne
    private EmprestimoBancario emprestimoBancario;

    /**
     * Caixinha de poupança à qual este evento pertence.
     * Preenchido apenas para eventos do tipo {@link Tipo#Poupanca}.
     * Nulo = evento avulso (não vinculado a nenhuma caixinha).
     */
    @ManyToOne
    private Caixinha caixinha;

    /**
     * Vincula o par saída/recebimento de uma Transferência interna.
     * Preenchido apenas para eventos gerados pela TransferenciaEvento entre
     * instituições do mesmo usuário. Nulo para os demais tipos, para transferências
     * externas ou para transferências criadas antes desta funcionalidade existir.
     */
    @ManyToOne
    @JoinColumn(name = "transferencia_vinculada_id")
    private EventoFinanceiro transferenciaVinculada;

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario fkUsuario) {
        this.usuario = fkUsuario;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public LocalDate getDataEvento() {
        return dataEvento;
    }

    public void setDataEvento(LocalDate dataEvento) {
        this.dataEvento = dataEvento;
    }

    public Double getTaxaRendimento() {
        return taxaRendimento;
    }

    public void setTaxaRendimento(Double taxaRendimento) {
        this.taxaRendimento = taxaRendimento;
    }

    public Integer getTempoAplicacao() {
        return tempoAplicacao;
    }

    public void setTempoAplicacao(Integer tempoAplicacao) {
        this.tempoAplicacao = tempoAplicacao;
    }

    public Integer getTempoProjecao() {
        return tempoProjecao;
    }

    public void setTempoProjecao(Integer tempoProjecao) {
        this.tempoProjecao = tempoProjecao;
    }

    public LocalDateTime getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(LocalDateTime dataRegistro) {
        this.dataRegistro = dataRegistro;
    }

    public List<EventoInstituicao> getEventoInstituicoes() {
        return eventoInstituicoes;
    }

    public void setEventoInstituicoes(List<EventoInstituicao> eventoInstituicoes) {
        this.eventoInstituicoes = eventoInstituicoes;
    }

    public EventoDetalhe getGastoDetalhe() {
        return eventoDetalhe;
    }

    public void setGastoDetalhe(EventoDetalhe eventoDetalhe) {
        this.eventoDetalhe = eventoDetalhe;
    }

    public EmprestimoBancario getEmprestimoBancario() { return emprestimoBancario; }
    public void setEmprestimoBancario(EmprestimoBancario emprestimoBancario) { this.emprestimoBancario = emprestimoBancario; }

    public Caixinha getCaixinha() { return caixinha; }
    public void setCaixinha(Caixinha caixinha) { this.caixinha = caixinha; }

    public EventoFinanceiro getTransferenciaVinculada() { return transferenciaVinculada; }
    public void setTransferenciaVinculada(EventoFinanceiro transferenciaVinculada) { this.transferenciaVinculada = transferenciaVinculada; }
}

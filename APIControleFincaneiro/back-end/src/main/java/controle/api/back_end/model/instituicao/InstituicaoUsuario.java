package controle.api.back_end.model.instituicao;

import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.usuario.Usuario;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
public class InstituicaoUsuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "instituicao_id", nullable = false)
    private Instituicao instituicao;

    @Column(name = "is_ativo", nullable = false)
    private Boolean isAtivo = Boolean.TRUE;

    @OneToMany
    private List<EventoInstituicao> eventoInstituicao;

    private LocalDateTime ultimaModificacao;

    /** Limite de crédito configurado pelo usuário. Default 0. */
    @Column(precision = 15, scale = 2)
    private BigDecimal limiteCredito = BigDecimal.ZERO;

    /** Taxa de juros personalizada pelo usuário (% a.m.). Nullable. */
    private Double taxaJuros;

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario fkUsuario) { this.usuario = fkUsuario; }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Instituicao getInstituicao() { return instituicao; }
    public void setInstituicao(Instituicao fkInstituicao) { this.instituicao = fkInstituicao; }

    public Boolean getIsAtivo() { return isAtivo; }
    public void setIsAtivo(Boolean ativo) { isAtivo = ativo; }

    public LocalDateTime getUltimaModificacao() { return ultimaModificacao; }
    public void setUltimaModificacao(LocalDateTime ultimaModificacao) { this.ultimaModificacao = ultimaModificacao; }

    public BigDecimal getLimiteCredito() { return limiteCredito; }
    public void setLimiteCredito(BigDecimal limiteCredito) { this.limiteCredito = limiteCredito; }

    public Double getTaxaJuros() { return taxaJuros; }
    public void setTaxaJuros(Double taxaJuros) { this.taxaJuros = taxaJuros; }
}

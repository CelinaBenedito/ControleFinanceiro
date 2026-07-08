package controle.api.back_end.model.configuracoes;

import controle.api.back_end.model.usuario.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
public class Configuracoes {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @NotNull
    private Usuario usuario;

    @OneToMany
    private List<LimitePorCategoria> limitePorCategoria;

    @OneToMany
    private List<LimitePorInstituicao> limitePorInstituicao;

    @Column(columnDefinition = "integer default 1")
    private Integer inicioMesFiscal;

    @PastOrPresent
    private LocalDate ultimaAtualizacao;

    @Positive
    private Double limiteDesejadoMensal;

    // ── Preferências de e-mail ────────────────────────────────────────────────

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "configuracoes_alertas_email",
                     joinColumns = @JoinColumn(name = "configuracoes_id"))
    @Column(name = "tipo_alerta")
    private Set<TipoAlertaEmail> alertasEmailAtivos = new HashSet<>();

    /** Percentual do limite mensal que dispara o alerta (1-100). Padrao: 80. */
    @Column(columnDefinition = "integer default 80")
    private Integer percentualAlertaGasto = 80;

    /** Percentual da meta de poupanca que dispara o alerta (1-100). Padrao: 90. */
    @Column(columnDefinition = "integer default 90")
    private Integer percentualAlertaMeta = 90;

    /** Controle interno para nao enviar alerta de gasto mais de uma vez por mes. */
    private LocalDate ultimoAlertaGastoEnviado;

    /** Controle interno para nao enviar alerta de meta mais de uma vez por mes. */
    private LocalDate ultimoAlertaMetaEnviado;

    // ── Getters e Setters ────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario fkUsuario) { this.usuario = fkUsuario; }

    public Integer getInicioMesFiscal() { return inicioMesFiscal; }
    public void setInicioMesFiscal(Integer inicioMesFiscal) { this.inicioMesFiscal = inicioMesFiscal; }

    public LocalDate getUltimaAtualizacao() { return ultimaAtualizacao; }
    public void setUltimaAtualizacao(LocalDate ultimaAtualizacao) { this.ultimaAtualizacao = ultimaAtualizacao; }

    public Double getLimiteDesejadoMensal() { return limiteDesejadoMensal; }
    public void setLimiteDesejadoMensal(Double limiteDesejadoMensal) { this.limiteDesejadoMensal = limiteDesejadoMensal; }

    public List<LimitePorCategoria> getLimitePorCategoria() { return limitePorCategoria; }
    public void setLimitePorCategoria(List<LimitePorCategoria> limitePorCategoria) { this.limitePorCategoria = limitePorCategoria; }

    public List<LimitePorInstituicao> getLimitePorInstituicao() { return limitePorInstituicao; }
    public void setLimitePorInstituicao(List<LimitePorInstituicao> limitePorInstituicao) { this.limitePorInstituicao = limitePorInstituicao; }

    public Set<TipoAlertaEmail> getAlertasEmailAtivos() { return alertasEmailAtivos; }
    public void setAlertasEmailAtivos(Set<TipoAlertaEmail> alertasEmailAtivos) { this.alertasEmailAtivos = alertasEmailAtivos; }

    public Integer getPercentualAlertaGasto() { return percentualAlertaGasto; }
    public void setPercentualAlertaGasto(Integer percentualAlertaGasto) { this.percentualAlertaGasto = percentualAlertaGasto; }

    public Integer getPercentualAlertaMeta() { return percentualAlertaMeta; }
    public void setPercentualAlertaMeta(Integer percentualAlertaMeta) { this.percentualAlertaMeta = percentualAlertaMeta; }

    public LocalDate getUltimoAlertaGastoEnviado() { return ultimoAlertaGastoEnviado; }
    public void setUltimoAlertaGastoEnviado(LocalDate ultimoAlertaGastoEnviado) { this.ultimoAlertaGastoEnviado = ultimoAlertaGastoEnviado; }

    public LocalDate getUltimoAlertaMetaEnviado() { return ultimoAlertaMetaEnviado; }
    public void setUltimoAlertaMetaEnviado(LocalDate ultimoAlertaMetaEnviado) { this.ultimoAlertaMetaEnviado = ultimoAlertaMetaEnviado; }
}

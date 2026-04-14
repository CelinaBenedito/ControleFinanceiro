package controle.api.back_end.model.configuracoes;

import controle.api.back_end.model.usuario.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;
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
    @Column(columnDefinition = "integer default 30")
    private Integer finalMesFiscal;

    @PastOrPresent
    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDate ultimaAtualizacao;

    @Positive
    private Double limiteDesejadoMensal;

    public Configuracoes() {
    }

    public Configuracoes(UUID id, Usuario usuario, Integer inicioMesFiscal, Integer finalMesFiscal, LocalDate ultimaAtualizacao, Double limiteDesejadoMensal) {
        this.id = id;
        this.usuario = usuario;
        this.inicioMesFiscal = inicioMesFiscal;
        this.finalMesFiscal = finalMesFiscal;
        this.ultimaAtualizacao = ultimaAtualizacao;
        this.limiteDesejadoMensal = limiteDesejadoMensal;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario fkUsuario) {
        this.usuario = fkUsuario;
    }

    public Integer getInicioMesFiscal() {
        return inicioMesFiscal;
    }

    public void setInicioMesFiscal(Integer inicioMesFiscal) {
        this.inicioMesFiscal = inicioMesFiscal;
    }

    public Integer getFinalMesFiscal() {
        return finalMesFiscal;
    }

    public void setFinalMesFiscal(Integer finalMesFiscal) {
        this.finalMesFiscal = finalMesFiscal;
    }

    public LocalDate getUltimaAtualizacao() {
        return ultimaAtualizacao;
    }

    public void setUltimaAtualizacao(LocalDate ultimaAtualizacao) {
        this.ultimaAtualizacao = ultimaAtualizacao;
    }

    public Double getLimiteDesejadoMensal() {
        return limiteDesejadoMensal;
    }

    public void setLimiteDesejadoMensal(Double limiteDesejadoMensal) {
        this.limiteDesejadoMensal = limiteDesejadoMensal;
    }

    public List<LimitePorCategoria> getLimitePorCategoria() {
        return limitePorCategoria;
    }

    public void setLimitePorCategoria(List<LimitePorCategoria> limitePorCategoria) {
        this.limitePorCategoria = limitePorCategoria;
    }

    public List<LimitePorInstituicao> getLimitePorInstituicao() {
        return limitePorInstituicao;
    }

    public void setLimitePorInstituicao(List<LimitePorInstituicao> limitePorInstituicao) {
        this.limitePorInstituicao = limitePorInstituicao;
    }
}

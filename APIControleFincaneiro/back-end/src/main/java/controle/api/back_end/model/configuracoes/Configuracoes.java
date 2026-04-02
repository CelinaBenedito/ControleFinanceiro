package controle.api.back_end.model.configuracoes;

import controle.api.back_end.model.usuario.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

@Entity
public class Configuracoes {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "fkUsuario")
    @NotNull
    private Usuario fkUsuario;

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

    public Configuracoes(UUID id, Usuario fkUsuario, Integer inicioMesFiscal, Integer finalMesFiscal, LocalDate ultimaAtualizacao, Double limiteDesejadoMensal) {
        this.id = id;
        this.fkUsuario = fkUsuario;
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

    public Usuario getFkUsuario() {
        return fkUsuario;
    }

    public void setFkUsuario(Usuario fkUsuario) {
        this.fkUsuario = fkUsuario;
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
}

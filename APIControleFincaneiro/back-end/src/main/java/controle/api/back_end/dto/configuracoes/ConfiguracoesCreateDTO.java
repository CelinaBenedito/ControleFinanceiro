package controle.api.back_end.dto.configuracoes;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

public class ConfiguracoesCreateDTO {
    @NotNull
    @Schema(description="Representa o id do usuario associado a configuração")
    private UUID fkUsuario;

    @Positive
    @Schema(example = "01", description="Representa o inicio do mês fiscal do usuario")
    private Integer inicioMesFiscal;

    @Positive
    @Schema(example = "30", description="Representa o final do mês fiscal do usuario")
    private Integer finalMesFiscal;

    @PastOrPresent
    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDate ultimaAtualizacao;

    @Positive
    private Double limiteDesejadoMensal;

    public ConfiguracoesCreateDTO(UUID fkUsuario, Integer inicioMesFiscal, Integer finalMesFiscal, LocalDate ultimaAtualizacao, Double limiteDesejadoMensal) {
        this.fkUsuario = fkUsuario;
        this.inicioMesFiscal = inicioMesFiscal;
        this.finalMesFiscal = finalMesFiscal;
        this.ultimaAtualizacao = ultimaAtualizacao;
        this.limiteDesejadoMensal = limiteDesejadoMensal;
    }

    public ConfiguracoesCreateDTO() {
    }

    public UUID getFkUsuario() {
        return fkUsuario;
    }

    public void setFkUsuario(UUID fkUsuario) {
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

package controle.api.back_end.dto.configuracoes;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public class ConfiguracoesCreateDTO {
    @NotNull
    @Schema(example="21eb5d2f-3fd8-439e-b647-5cc1f753ae58", description="Representa o id do usuario associado a configuração")
    private UUID fkUsuario;

    @Positive
    @Schema(example = "01", description="Representa o inicio do mês fiscal do usuario")
    private Integer inicioMesFiscal;

    @Positive
    @Schema(example = "1150.0", description="Representa limite total que o usuario deseja gastar por mês, sem especificar o quanto vai ser por instituição")
    private Double limiteDesejadoMensal;

    public ConfiguracoesCreateDTO(UUID fkUsuario, Integer inicioMesFiscal, Double limiteDesejadoMensal) {
        this.fkUsuario = fkUsuario;
        this.inicioMesFiscal = inicioMesFiscal;
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

    public Double getLimiteDesejadoMensal() {
        return limiteDesejadoMensal;
    }

    public void setLimiteDesejadoMensal(Double limiteDesejadoMensal) {
        this.limiteDesejadoMensal = limiteDesejadoMensal;
    }
}

package controle.api.back_end.dto.configuracoes;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public class ConfiguracaoEditDTO {
     private UUID id;

    @Positive
    @Schema(example = "01", description="Representa o inicio do mês fiscal do usuario")
    private Integer inicioMesFiscal;


    @Positive
    @Schema(example = "1150.0", description="Representa limite total que o usuario deseja gastar por mês, sem especificar o quanto vai ser por instituição")
    private Double limiteDesejadoMensal;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

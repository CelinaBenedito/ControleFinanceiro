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

    @Positive
    @Schema(example = "1", description="Representa o id da instituicao associada ao usuario para ser definido o limite")
    private Integer instituicaoUsuario_id;

    @Positive
    @Schema(example = "1150.0", description="Representa limite total que o usuario deseja gastar por mês por instituição")
    private Double limiteInstituicao;

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

    public Integer getInstituicaoUsuario_id() {
        return instituicaoUsuario_id;
    }

    public void setInstituicaoUsuario_id(Integer instituicaoUsuario_id) {
        this.instituicaoUsuario_id = instituicaoUsuario_id;
    }

    public Double getLimiteInstituicao() {
        return limiteInstituicao;
    }

    public void setLimiteInstituicao(Double limiteInstituicao) {
        this.limiteInstituicao = limiteInstituicao;
    }
}

package controle.api.back_end.dto.configuracoes;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public class ConfiguracaoEditDTO {
    @NotNull
    @Schema(example = "1", description= "Representa o id das configurações.")
    private UUID id;

    @Positive
    @Schema(example = "01", description="Representa o inicio do mês fiscal do usuário")
    private Integer inicioMesFiscal;

    @Positive
    @Schema(example = "1150.0", description="Representa limite total que o usuário deseja gastar por mês, sem especificar o quanto vai ser por instituição")
    private Double limiteDesejadoMensal;

    @Positive
    @Schema(example = "1", description= "Representa o id da instituição associada ao usuário para ser definido o limite")
    private Integer instituicao_id;

    @Positive
    @Schema(example = "1150.0", description= "Representa limite total que o usuário deseja gastar por mês por instituição")
    private Double limiteInstituicao;

    @Positive
    @Schema(example = "1", description= "Representa o id da categoria associada ao usuário para ser definido o limite")
    private Integer categoria_id;

    @Positive
    @Schema(example = "1000.0", description= "Representa limite total que o usuário deseja gastar por mês por instituição")
    private Double limiteCategoria;

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

    public Integer getInstituicao_id() {
        return instituicao_id;
    }

    public void setInstituicao_id(Integer instituicao_id) {
        this.instituicao_id = instituicao_id;
    }

    public Double getLimiteInstituicao() {
        return limiteInstituicao;
    }

    public void setLimiteInstituicao(Double limiteInstituicao) {
        this.limiteInstituicao = limiteInstituicao;
    }

    public Integer getCategoria_id() {
        return categoria_id;
    }

    public void setCategoria_id(Integer categoriaUsuario_id) {
        this.categoria_id = categoriaUsuario_id;
    }

    public Double getLimiteCategoria() {
        return limiteCategoria;
    }

    public void setLimiteCategoria(Double limiteCategoria) {
        this.limiteCategoria = limiteCategoria;
    }
}

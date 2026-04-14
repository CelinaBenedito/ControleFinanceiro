package controle.api.back_end.dto.configuracoes;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public class ConfiguracoesCreateDTO {
    @NotNull
    @Schema(example="21eb5d2f-3fd8-439e-b647-5cc1f753ae58", description="Representa o id do usuario associado a configuração")
    private UUID fkUsuario;

    @Positive
    @Schema(example = "1", description="Representa o inicio do mês fiscal do usuario")
    private Integer inicioMesFiscal;

    @Positive
    @Schema(example = "1150.0", description="Representa limite total que o usuario deseja gastar por mês, sem especificar o quanto vai ser por instituição")
    private Double limiteDesejadoMensal;

    private List<LimiteInstituicaoCreateDTO> limitesInstituicao;
    private List<LimiteCategoriaCreateDTO> limitesCategoria;

    public static class LimiteInstituicaoCreateDTO{
        @Schema(example = "1", description= "Representa o id da instituição")
        private Integer instituicaoId;

        @Positive
        @Schema(example = "100.00", description = "Representa o valor do limite que será atribuido a instituição.")
        private Double valor;

        public Integer getInstituicaoId() {
            return instituicaoId;
        }

        public void setInstituicaoId(Integer instituicaoId) {
            this.instituicaoId = instituicaoId;
        }

        public Double getValor() {
            return valor;
        }

        public void setValor(Double valor) {
            this.valor = valor;
        }
    }

    public static class LimiteCategoriaCreateDTO{
        @Schema(example = "1", description = "Representa o id da categoria")
        private Integer categoriaId;

        @Schema(example = "100.00", description = "Representa o valor que será definido como limite de gasto mensal para a categoria selecionada")
        @Positive
        private Double valor;

        public Integer getCategoriaId() {
            return categoriaId;
        }

        public void setCategoriaId(Integer categoriaId) {
            this.categoriaId = categoriaId;
        }

        public Double getValor() {
            return valor;
        }

        public void setValor(Double valor) {
            this.valor = valor;
        }
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

    public List<LimiteInstituicaoCreateDTO> getLimitesInstituicao() {
        return limitesInstituicao;
    }

    public void setLimitesInstituicao(List<LimiteInstituicaoCreateDTO> limitesInstituicao) {
        this.limitesInstituicao = limitesInstituicao;
    }

    public List<LimiteCategoriaCreateDTO> getLimitesCategoria() {
        return limitesCategoria;
    }

    public void setLimitesCategoria(List<LimiteCategoriaCreateDTO> limitesCategoria) {
        this.limitesCategoria = limitesCategoria;
    }
}

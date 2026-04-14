package controle.api.back_end.dto.configuracoes;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public class ConfiguracaoEditDTO {
    @Positive
    @Schema(example = "01", description="Representa o inicio do mês fiscal do usuário")
    private Integer inicioMesFiscal;

    @Positive
    @Schema(example = "1150.0", description="Representa limite total que o usuário deseja gastar por mês, sem especificar o quanto vai ser por instituição")
    private Double limiteDesejadoMensal;

    private List<LimiteInstituicaoEditDTO> limitesInstituicao;
    private List<LimiteCategoriaEditDTO> limitesCategoria;

    public static class LimiteInstituicaoEditDTO{
        private Integer instituicaoId;
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

    public static class LimiteCategoriaEditDTO{
        private Integer categoriaId;
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

    public List<LimiteInstituicaoEditDTO> getLimitesInstituicao() {
        return limitesInstituicao;
    }

    public void setLimitesInstituicao(List<LimiteInstituicaoEditDTO> limitesInstituicao) {
        this.limitesInstituicao = limitesInstituicao;
    }

    public List<LimiteCategoriaEditDTO> getLimitesCategoria() {
        return limitesCategoria;
    }

    public void setLimitesCategoria(List<LimiteCategoriaEditDTO> limitesCategoria) {
        this.limitesCategoria = limitesCategoria;
    }
}

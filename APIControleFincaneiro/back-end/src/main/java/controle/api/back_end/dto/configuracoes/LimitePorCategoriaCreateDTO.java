package controle.api.back_end.dto.configuracoes;

import io.swagger.v3.oas.annotations.media.Schema;

public class LimitePorCategoriaCreateDTO {
    @Schema(example = "1", description = "Representa o id que conecta a categoria e o usuario ao limite desejado mensal")
    private Integer categoriaUsuario_id;
    @Schema(example = "100.00", description = "Representa o valor desejado para o limite mensal")
    private Double limiteDesejado;

    public Integer getCategoriaUsuario_id() {
        return categoriaUsuario_id;
    }

    public void setCategoriaUsuario_id(Integer categoriaUsuario_id) {
        this.categoriaUsuario_id = categoriaUsuario_id;
    }

    public Double getLimiteDesejado() {
        return limiteDesejado;
    }

    public void setLimiteDesejado(Double limiteDesejado) {
        this.limiteDesejado = limiteDesejado;
    }
}

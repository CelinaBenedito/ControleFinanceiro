package controle.api.back_end.dto.configuracoes.in;

import io.swagger.v3.oas.annotations.media.Schema;

public class LimitePorInstitucaoCreateDTO {
    @Schema(example = "1", description = "Representa o id que conecta a instituição e o usuario ao limite desejado mensal")
    private Integer institucaoUsuario_id;
    @Schema(example = "100.00", description = "Representa o valor desejado para o limite mensal")
    private Double limiteDesejado;

    public Integer getInstitucaoUsuario_id() {
        return institucaoUsuario_id;
    }

    public void setInstitucaoUsuario_id(Integer institucaoUsuario_id) {
        this.institucaoUsuario_id = institucaoUsuario_id;
    }

    public Double getLimiteDesejado() {
        return limiteDesejado;
    }

    public void setLimiteDesejado(Double limiteDesejado) {
        this.limiteDesejado = limiteDesejado;
    }
}


package controle.api.back_end.dto.registros.in;

import controle.api.back_end.model.eventoFinanceiro.TipoMovimento;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public class EventoInstituicaoCreateDto {
    @Schema(example = "2", description = "Representa a instituição a qual pertence a movimentação.")
    @NotNull
    private Integer instituicaoUsuario_id;
    @Schema(example = "Credito", description = "Representa qual foi o tipo de movimento.")
    @NotNull
    private TipoMovimento tipoMovimento;
    @NotNull
    @Positive
    @Schema(example = "125.50", description = "Representa o valor da movimentação.")
    private Double valor;
    @NotNull
    @Schema(example = "2", description = "Representa a quantidade de parcelas da movimentação")
    private Integer parcelas = 1;

    public Integer getInstituicaoUsuario_id() {
        return instituicaoUsuario_id;
    }

    public void setInstituicaoUsuario_id(Integer instituicaoUsuario_id) {
        this.instituicaoUsuario_id = instituicaoUsuario_id;
    }

    public TipoMovimento getTipoMovimento() {
        return tipoMovimento;
    }

    public void setTipoMovimento(TipoMovimento tipoMovimento) {
        this.tipoMovimento = tipoMovimento;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Integer getParcelas() {
        return parcelas;
    }

    public void setParcelas(Integer parcelas) {
        this.parcelas = parcelas;
    }
}



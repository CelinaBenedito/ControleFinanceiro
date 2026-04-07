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
    @Schema(example = "30", description="Representa o final do mês fiscal do usuario")
    private Integer finalMesFiscal;

    @Positive
    @Schema(example = "1150.0", description="Representa limite total que o usuario deseja gastar por mês, sem especificar o quanto vai ser por instituição")
    private Double limiteDesejadoMensal;
}

package controle.api.back_end.dto.instituicao;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class InstituicaoCreateDTO {
    @Size(max = 50)
    @Schema(example = "Itau", description = "Representa o nome do banco que o usuario utiliza")
    @NotBlank
    private String nome;

    public InstituicaoCreateDTO(String nome) {
        this.nome = nome;
    }

    public InstituicaoCreateDTO() {
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}

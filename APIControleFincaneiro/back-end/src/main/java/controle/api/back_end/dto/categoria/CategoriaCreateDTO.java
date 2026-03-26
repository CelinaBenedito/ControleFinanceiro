package controle.api.back_end.dto.categoria;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoriaCreateDTO {
    @NotBlank
    @Size(max = 30)
    @Schema(example = "Roupa", description = "Representa o nome da categoria")
    private String titulo;

    public CategoriaCreateDTO() {
    }

    public CategoriaCreateDTO(String titulo) {
        this.titulo = titulo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }
}

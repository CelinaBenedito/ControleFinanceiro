package controle.api.back_end.dto.categoria;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Random;
import java.util.UUID;
import java.util.random.RandomGenerator;

public class CategoriaCreateDTO {
    @NotBlank
    @Size(max = 30)
    @Schema(example = "Eletrônicos", description = "Representa o nome da categoria")
    private String titulo;
    @NotNull
    @Schema(example = "21eb5d2f-3fd8-439e-b647-5cc1f753a58e", description = "Representa o id do usuario a quem pertence essa classe")
    private UUID fkUsuario;

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

    public UUID getFkUsuario() {
        return fkUsuario;
    }

    public void setFkUsuario(UUID fkUsuario) {
        this.fkUsuario = fkUsuario;
    }
}

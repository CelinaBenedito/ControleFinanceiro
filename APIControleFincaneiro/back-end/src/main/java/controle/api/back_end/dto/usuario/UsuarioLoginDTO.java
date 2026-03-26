package controle.api.back_end.dto.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UsuarioLoginDTO {
    @Size(max = 150, min = 10)
    @NotBlank
    @Schema(example = "joaosilva@gmail.com", description = "Representa o email do usuario")
    private String email;

    @Size(max=25, min = 6)
    @NotBlank
    @Schema(example = "Senha123!?", description = "Representa a senha do usuário")
    private String senha;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }
}

package controle.api.back_end.dto.usuario.in;

import controle.api.back_end.model.usuario.GeneroUsuario;
import controle.api.back_end.model.usuario.Pronome;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class UsuarioCreateDTO {

    @Size(max = 50)
    @NotBlank
    @Schema(example = "João", description = "Representa o nome do usuário")
    private String nome;

    @Size(max = 100)
    @NotBlank
    @Schema(example = "Da Silva Santos", description = "Representa o sobrenome do usuário")
    private String sobrenome;

    @Past
    @NotNull
    @Schema(example = "2006-07-13", description = "Representa a data de nascimento do usuário")
    private LocalDate dataNascimento;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Schema(example = "HOMEM_CIS", description = "Representa o gênero do usuário")
    private GeneroUsuario genero;

    @Enumerated(EnumType.STRING)
    @Schema(description = "Pronome de tratamento preferido pelo usuário")
    private Pronome pronome;

    @Size(max = 50)
    @Schema(description = "Texto livre do pronome quando 'Personalizado' for escolhido")
    private String pronomePersonalizado;

    @Size(max = 150, min = 10)
    @NotBlank
    @Email
    @Schema(example = "joaosilva@gmail.com", description = "Representa o email do usuario")
    private String email;

    @Size(max=25, min = 6)
    @NotBlank
    @Schema(example = "Senha123!?", description = "Representa a senha do usuário")
    private String senha;

    public UsuarioCreateDTO() {
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getSobrenome() { return sobrenome; }
    public void setSobrenome(String sobrenome) { this.sobrenome = sobrenome; }

    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }

    public GeneroUsuario getGenero() { return genero; }
    public void setGenero(GeneroUsuario genero) { this.genero = genero; }

    public Pronome getPronome() { return pronome; }
    public void setPronome(Pronome pronome) { this.pronome = pronome; }

    public String getPronomePersonalizado() { return pronomePersonalizado; }
    public void setPronomePersonalizado(String pronomePersonalizado) { this.pronomePersonalizado = pronomePersonalizado; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
}

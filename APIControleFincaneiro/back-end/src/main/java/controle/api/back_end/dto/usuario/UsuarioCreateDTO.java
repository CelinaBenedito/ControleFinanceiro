package controle.api.back_end.dto.usuario;

import controle.api.back_end.model.UsuarioSexo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class UsuarioCreateDTO {

    @Size(max = 50)
    @NotBlank
    @Schema(example = "João", description = "Representa o nome do usuário")
    private String nome;

    @Size(max = 100)
    @NotBlank
    @Column(nullable = false)
    @Schema(example = "Da Silva Santos", description = "Representa o sobrenome do usuário")
    private String sobrenome;

    @Past
    @NotNull
    @Column(nullable = false)
    @Schema(example = "07-13-2006", description = "Representa a data de nascimento do usuário")
    private LocalDate dataNascimento;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(example = "07-13-2006", description = "Representa a data de nascimento do usuário")
    private UsuarioSexo sexo;

    @Size(max=25, min = 6)
    @NotBlank
    @Schema(example = "Senha123!?", description = "Representa a senha do usuário")
    private String senha;

    public UsuarioCreateDTO() {
    }

    public UsuarioCreateDTO(String nome, String sobrenome, LocalDate dataNascimento, UsuarioSexo sexo, String senha) {
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.dataNascimento = dataNascimento;
        this.sexo = sexo;
        this.senha = senha;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSobrenome() {
        return sobrenome;
    }

    public void setSobrenome(String sobrenome) {
        this.sobrenome = sobrenome;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public UsuarioSexo getSexo() {
        return sexo;
    }

    public void setSexo(UsuarioSexo sexo) {
        this.sexo = sexo;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }
}

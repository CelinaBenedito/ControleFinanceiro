package controle.api.back_end.dto.usuario;

import controle.api.back_end.model.UsuarioSexo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class UsuarioEditDTO {

    @Size(max = 50)
    @Schema(example = "José", description = "Representa o nome do usuario")
    private String nome;

    @Size(max = 100)
    @Schema(example = "Da Silva Pereira", description = "Representa o sobrenome do usuario")
    private String sobrenome;

    @Past
    @Schema(example = "2006-07-15", description = "Representa a data de nascimento do usuario")
    private LocalDate dataNascimento;

    @Enumerated(EnumType.STRING)
    @Schema(example = "1", description = "Representa o sexo do usuario")
    private UsuarioSexo sexo;

    @Size(max=500)
    private String imagem;

    @Size(max = 150, min = 10)
    @Email
    @Schema(example = "joaosilva@gmail.com", description = "Representa o email do usuario")
    private String email;


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

    public String getImagem() {
        return imagem;
    }

    public void setImagem(String imagem) {
        this.imagem = imagem;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

package controle.api.back_end.dto.usuario.out;

import controle.api.back_end.model.usuario.GeneroUsuario;
import controle.api.back_end.model.usuario.Pronome;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;
import java.util.UUID;

public class UsuarioResponseDTO {
    private UUID id;
    @NotBlank
    @Schema(example = "Jhonas")
    private String nome;
    @NotBlank
    @Schema(example = "Junior da Silva")
    private String sobrenome;
    @Past
    @Schema(example = "1985-10-05")
    private LocalDate dataNascimento;
    @Enumerated(EnumType.STRING)
    @Schema(example = "HOMEM_CIS")
    private GeneroUsuario genero;
    @Enumerated(EnumType.STRING)
    private Pronome pronome;
    private String pronomePersonalizado;
    @Schema(example = "jhonas.silva@email.com")
    private String email;
    private String imagem;

    public UsuarioResponseDTO() {
    }

    public UsuarioResponseDTO(UUID id, String nome, String sobrenome, LocalDate dataNascimento, GeneroUsuario genero, String imagem) {
        this.id = id;
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.dataNascimento = dataNascimento;
        this.genero = genero;
        this.imagem = imagem;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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

    public String getImagem() { return imagem; }
    public void setImagem(String imagem) { this.imagem = imagem; }
}

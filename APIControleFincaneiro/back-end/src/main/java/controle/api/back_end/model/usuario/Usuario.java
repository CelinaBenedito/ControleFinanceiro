package controle.api.back_end.model.usuario;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Size(max = 50)
    @NotBlank
    private String nome;

    @Size(max = 100)
    @NotBlank
    @Column(nullable = false)
    private String sobrenome;

    @Past
    @NotNull
    @Column(nullable = false)
    private LocalDate dataNascimento;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UsuarioSexo sexo;

    @Size(max=500)
    private String imagem;

    @NotBlank
    @Size(max = 150)
    private String email;

    @NotBlank
    @Size(max=25)
    private String senha;


    private Boolean isAtivo = true;

    public Usuario() {
    }

    public Usuario(UUID id, String nome, String sobrenome, LocalDate dataNascimento, UsuarioSexo sexo, String imagem, String email, String senha) {
        this.id = id;
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.dataNascimento = dataNascimento;
        this.sexo = sexo;
        this.imagem = imagem;
        this.email = email;
        this.senha = senha;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public Boolean getIsAtivo() {
        return isAtivo;
    }

    public void setIsAtivo(Boolean ativo) {
        isAtivo = ativo;
    }
}

package controle.api.back_end.adapters.outbound.entitys;

import controle.api.back_end.domain.usuario.Usuario;
import controle.api.back_end.domain.usuario.UsuarioSexo;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JpaUsuarioEntity {
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

    public JpaUsuarioEntity(Usuario usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.dataNascimento = usuario.getDataNascimento();
        this.nome = usuario.getNome();
        this.imagem = usuario.getImagem();
        this.isAtivo = usuario.getIsAtivo();
        this.senha = usuario.getSenha();
        this.sexo = usuario.getSexo();
    }
}

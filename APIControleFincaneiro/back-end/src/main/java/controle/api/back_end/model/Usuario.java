package controle.api.back_end.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

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
    @Column(nullable = false)
    private LocalDate dataNascimento;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UsuarioSexo sexo;
    @Size(max=500)
    private String imagem;
    @Size(max=25)
    private String senha;

}

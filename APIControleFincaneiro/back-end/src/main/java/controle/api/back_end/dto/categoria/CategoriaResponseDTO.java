package controle.api.back_end.dto.categoria;

import controle.api.back_end.model.Usuario;
import controle.api.back_end.model.UsuarioSexo;

import java.time.LocalDate;
import java.util.UUID;

public class CategoriaResponseDTO {
    private Integer id;

    private String titulo;

    private UsuarioCategoriaDTO usuario;

    public static class UsuarioCategoriaDTO{
        private UUID id;
        private String nome;
        private String sobrenome;
        private LocalDate dataNascimento;
        private UsuarioSexo sexo;
        private String email;

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

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public UsuarioCategoriaDTO getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioCategoriaDTO usuario) {
        this.usuario = usuario;
    }
}

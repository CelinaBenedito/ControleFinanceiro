package controle.api.back_end.dto.categoria;


import controle.api.back_end.model.usuario.UsuarioSexo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class CategoriaUsuarioResponseDTO {
    private Integer id;
    private UsuarioDTO usuario;
    private CategoriaDTO categoria;
    private Boolean isAtivo;
    private LocalDateTime ultimaAtualizacao;

    public static class UsuarioDTO{
        private UUID id;
        private String nome;
        private String sobrenome;
        private LocalDate dataNascimento;
        private UsuarioSexo sexo;

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
    }
    public static class CategoriaDTO{
        private Integer id;
        private String titulo;

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
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public UsuarioDTO getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioDTO usuario) {
        this.usuario = usuario;
    }

    public CategoriaDTO getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaDTO categoria) {
        this.categoria = categoria;
    }

    public Boolean getAtivo() {
        return isAtivo;
    }

    public void setAtivo(Boolean ativo) {
        isAtivo = ativo;
    }

    public LocalDateTime getUltimaAtualizacao() {
        return ultimaAtualizacao;
    }

    public void setUltimaAtualizacao(LocalDateTime ultimaAtualizacao) {
        this.ultimaAtualizacao = ultimaAtualizacao;
    }
}

package controle.api.back_end.dto.instituicao;

import controle.api.back_end.model.usuario.UsuarioSexo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class InstituicaoUsuarioResponseDTO {

    private Integer id;
    private UsuarioInstituicaoDTO usuario;
    private InstituicaoUsuarioDTO intituicao;
    private Boolean isAtivo;
    private LocalDateTime ultimaAtualizacao;

    public static class UsuarioInstituicaoDTO{
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

    public static class InstituicaoUsuarioDTO{
        private Integer id;
        private String nome;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }
    }

    public InstituicaoUsuarioResponseDTO(Integer id, UsuarioInstituicaoDTO usuario, InstituicaoUsuarioDTO intituicao) {
        this.id = id;
        this.usuario = usuario;
        this.intituicao = intituicao;
    }

    public InstituicaoUsuarioResponseDTO() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public UsuarioInstituicaoDTO getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioInstituicaoDTO usuario) {
        this.usuario = usuario;
    }

    public InstituicaoUsuarioDTO getIntituicao() {
        return intituicao;
    }

    public void setIntituicao(InstituicaoUsuarioDTO intituicao) {
        this.intituicao = intituicao;
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

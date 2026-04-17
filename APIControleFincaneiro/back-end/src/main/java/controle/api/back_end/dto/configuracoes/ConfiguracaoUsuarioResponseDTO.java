package controle.api.back_end.dto.configuracoes;

import controle.api.back_end.model.usuario.UsuarioSexo;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

//DTO que retorna as configurações em conjunto com o usuário e informações mais detalhadas.
public class ConfiguracaoUsuarioResponseDTO {
    private UUID id;
    private ConfiguracaoUsuarioDTO usuario;
    private Integer inicioMesFiscal;
    private LocalDate ultimaAtualizacao;
    private Double limiteDesejadoMensal;
    private List<LimitePorInstituicaoResponseDto> limiteInstituicao;
    private List<LimitePorCategoriaResponseDto> limitePorCategoria;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ConfiguracaoUsuarioDTO getUsuario() {
        return usuario;
    }

    public void setUsuario(ConfiguracaoUsuarioDTO usuario) {
        this.usuario = usuario;
    }

    public Integer getInicioMesFiscal() {
        return inicioMesFiscal;
    }

    public void setInicioMesFiscal(Integer inicioMesFiscal) {
        this.inicioMesFiscal = inicioMesFiscal;
    }

    public LocalDate getUltimaAtualizacao() {
        return ultimaAtualizacao;
    }

    public void setUltimaAtualizacao(LocalDate ultimaAtualizacao) {
        this.ultimaAtualizacao = ultimaAtualizacao;
    }

    public Double getLimiteDesejadoMensal() {
        return limiteDesejadoMensal;
    }

    public void setLimiteDesejadoMensal(Double limiteDesejadoMensal) {
        this.limiteDesejadoMensal = limiteDesejadoMensal;
    }

    public List<LimitePorCategoriaResponseDto> getLimitePorCategoria() {
        return limitePorCategoria;
    }

    public void setLimitePorCategoria(List<LimitePorCategoriaResponseDto> limitePorCategoria) {
        this.limitePorCategoria = limitePorCategoria;
    }

    public List<LimitePorInstituicaoResponseDto> getLimiteInstituicao() {
        return limiteInstituicao;
    }

    public void setLimiteInstituicao(List<LimitePorInstituicaoResponseDto> limiteInstituicao) {
        this.limiteInstituicao = limiteInstituicao;
    }

    public static class ConfiguracaoUsuarioDTO {
        private UUID id;
        private String nome;
        private String sobrenome;
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

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

    }
}

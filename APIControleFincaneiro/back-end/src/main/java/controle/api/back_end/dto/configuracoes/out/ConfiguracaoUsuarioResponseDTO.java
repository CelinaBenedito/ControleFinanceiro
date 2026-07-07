package controle.api.back_end.dto.configuracoes.out;

import controle.api.back_end.model.configuracoes.TipoAlertaEmail;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ConfiguracaoUsuarioResponseDTO {

    private UUID id;
    private ConfiguracaoUsuarioDTO usuario;
    private Integer inicioMesFiscal;
    private LocalDate ultimaAtualizacao;
    private Double limiteDesejadoMensal;
    private List<LimitePorInstituicaoResponseDto> limiteInstituicao;
    private List<LimitePorCategoriaResponseDto> limitePorCategoria;
    private Set<TipoAlertaEmail> alertasEmailAtivos;
    private Integer percentualAlertaGasto;
    private Integer percentualAlertaMeta;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ConfiguracaoUsuarioDTO getUsuario() { return usuario; }
    public void setUsuario(ConfiguracaoUsuarioDTO usuario) { this.usuario = usuario; }

    public Integer getInicioMesFiscal() { return inicioMesFiscal; }
    public void setInicioMesFiscal(Integer inicioMesFiscal) { this.inicioMesFiscal = inicioMesFiscal; }

    public LocalDate getUltimaAtualizacao() { return ultimaAtualizacao; }
    public void setUltimaAtualizacao(LocalDate ultimaAtualizacao) { this.ultimaAtualizacao = ultimaAtualizacao; }

    public Double getLimiteDesejadoMensal() { return limiteDesejadoMensal; }
    public void setLimiteDesejadoMensal(Double limiteDesejadoMensal) { this.limiteDesejadoMensal = limiteDesejadoMensal; }

    public List<LimitePorCategoriaResponseDto> getLimitePorCategoria() { return limitePorCategoria; }
    public void setLimitePorCategoria(List<LimitePorCategoriaResponseDto> limitePorCategoria) { this.limitePorCategoria = limitePorCategoria; }

    public List<LimitePorInstituicaoResponseDto> getLimiteInstituicao() { return limiteInstituicao; }
    public void setLimiteInstituicao(List<LimitePorInstituicaoResponseDto> limiteInstituicao) { this.limiteInstituicao = limiteInstituicao; }

    public Set<TipoAlertaEmail> getAlertasEmailAtivos() { return alertasEmailAtivos; }
    public void setAlertasEmailAtivos(Set<TipoAlertaEmail> alertasEmailAtivos) { this.alertasEmailAtivos = alertasEmailAtivos; }

    public Integer getPercentualAlertaGasto() { return percentualAlertaGasto; }
    public void setPercentualAlertaGasto(Integer percentualAlertaGasto) { this.percentualAlertaGasto = percentualAlertaGasto; }

    public Integer getPercentualAlertaMeta() { return percentualAlertaMeta; }
    public void setPercentualAlertaMeta(Integer percentualAlertaMeta) { this.percentualAlertaMeta = percentualAlertaMeta; }

    public static class ConfiguracaoUsuarioDTO {
        private UUID id;
        private String nome;
        private String sobrenome;
        private String email;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getSobrenome() { return sobrenome; }
        public void setSobrenome(String sobrenome) { this.sobrenome = sobrenome; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}

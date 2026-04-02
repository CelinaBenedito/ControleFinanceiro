package controle.api.back_end.dto.configuracoes;

import java.time.LocalDate;
import java.util.UUID;

public class ConfiguracaoUsuarioResponseDTO {
    private UUID id;
    private ConfiguracaoUsuarioDTO usuario;
    private Integer inicioMesFiscal;
    private Integer finalMesFiscal;
    private LocalDate ultimaAtualizacao;
    private Double limiteDesejadoMensal;

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

    public Integer getFinalMesFiscal() {
        return finalMesFiscal;
    }

    public void setFinalMesFiscal(Integer finalMesFiscal) {
        this.finalMesFiscal = finalMesFiscal;
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

    public static class ConfiguracaoUsuarioDTO{
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

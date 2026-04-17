package controle.api.back_end.dto.configuracoes;

import java.util.UUID;

public class LimitePorInstituicaoResponseDto {
    private UUID id;
    private InstituicaoDTO instituicao;
    private Double limiteDesejado;

    public static class InstituicaoDTO{
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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public InstituicaoDTO getInstituicao() {
        return instituicao;
    }

    public void setInstituicao(InstituicaoDTO instituicao) {
        this.instituicao = instituicao;
    }

    public Double getLimiteDesejado() {
        return limiteDesejado;
    }

    public void setLimiteDesejado(Double limiteDesejado) {
        this.limiteDesejado = limiteDesejado;
    }
}

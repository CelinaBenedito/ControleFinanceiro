package controle.api.back_end.dto.configuracoes.in;

import controle.api.back_end.model.configuracoes.TipoAlertaEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.Set;

public class ConfiguracaoEditDTO {

    @Positive
    @Schema(example = "01", description = "Inicio do mes fiscal do usuario")
    private Integer inicioMesFiscal;

    @Positive
    @Schema(example = "1150.0", description = "Limite total de gastos mensais")
    private Double limiteDesejadoMensal;

    private List<LimiteInstituicaoEditDTO> limitesInstituicao;
    private List<LimiteCategoriaEditDTO> limitesCategoria;

    @Schema(description = "Alertas de e-mail ativos. Opcoes: ANIVERSARIO, ALERTA_LIMITE_MENSAL, ALERTA_META_POUPANCA, RELATORIO_MENSAL, LEMBRETE_APORTE")
    private Set<TipoAlertaEmail> alertasEmailAtivos;

    @Min(1) @Max(100)
    @Schema(description = "Percentual do limite mensal que dispara alerta. Padrao: 80", example = "80")
    private Integer percentualAlertaGasto;

    @Min(1) @Max(100)
    @Schema(description = "Percentual da meta de poupanca que dispara alerta. Padrao: 90", example = "90")
    private Integer percentualAlertaMeta;

    public Integer getInicioMesFiscal() { return inicioMesFiscal; }
    public void setInicioMesFiscal(Integer inicioMesFiscal) { this.inicioMesFiscal = inicioMesFiscal; }

    public Double getLimiteDesejadoMensal() { return limiteDesejadoMensal; }
    public void setLimiteDesejadoMensal(Double limiteDesejadoMensal) { this.limiteDesejadoMensal = limiteDesejadoMensal; }

    public List<LimiteInstituicaoEditDTO> getLimitesInstituicao() { return limitesInstituicao; }
    public void setLimitesInstituicao(List<LimiteInstituicaoEditDTO> limitesInstituicao) { this.limitesInstituicao = limitesInstituicao; }

    public List<LimiteCategoriaEditDTO> getLimitesCategoria() { return limitesCategoria; }
    public void setLimitesCategoria(List<LimiteCategoriaEditDTO> limitesCategoria) { this.limitesCategoria = limitesCategoria; }

    public Set<TipoAlertaEmail> getAlertasEmailAtivos() { return alertasEmailAtivos; }
    public void setAlertasEmailAtivos(Set<TipoAlertaEmail> alertasEmailAtivos) { this.alertasEmailAtivos = alertasEmailAtivos; }

    public Integer getPercentualAlertaGasto() { return percentualAlertaGasto; }
    public void setPercentualAlertaGasto(Integer percentualAlertaGasto) { this.percentualAlertaGasto = percentualAlertaGasto; }

    public Integer getPercentualAlertaMeta() { return percentualAlertaMeta; }
    public void setPercentualAlertaMeta(Integer percentualAlertaMeta) { this.percentualAlertaMeta = percentualAlertaMeta; }

    public static class LimiteInstituicaoEditDTO {
        private Integer instituicaoId;
        private Double valor;
        public Integer getInstituicaoId() { return instituicaoId; }
        public void setInstituicaoId(Integer instituicaoId) { this.instituicaoId = instituicaoId; }
        public Double getValor() { return valor; }
        public void setValor(Double valor) { this.valor = valor; }
    }

    public static class LimiteCategoriaEditDTO {
        private Integer categoriaId;
        private Double valor;
        public Integer getCategoriaId() { return categoriaId; }
        public void setCategoriaId(Integer categoriaId) { this.categoriaId = categoriaId; }
        public Double getValor() { return valor; }
        public void setValor(Double valor) { this.valor = valor; }
    }
}

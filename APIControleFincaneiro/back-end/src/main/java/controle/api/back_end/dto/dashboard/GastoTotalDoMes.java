package controle.api.back_end.dto.dashboard;

import java.math.BigDecimal;

public class GastoTotalDoMes {
    private BigDecimal valor;
    private Integer porcentagem;

    public GastoTotalDoMes(BigDecimal valor, Integer porcentagem) {
        this.valor = valor;
        this.porcentagem = porcentagem;
    }

    public GastoTotalDoMes() {
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public Integer getPorcentagem() {
        return porcentagem;
    }

    public void setPorcentagem(Integer porcantagem) {
        this.porcentagem = porcantagem;
    }
}

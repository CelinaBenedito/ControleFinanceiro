package controle.api.back_end.dto.instituicao.in;

import controle.api.back_end.model.eventoFinanceiro.TipoMovimento;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Configuração inicial opcional aplicada ao vincular uma instituição a um usuário.
 * Permite já definir quais tipos de movimento a instituição aceita (débito, crédito,
 * pix, boleto, dinheiro, voucher) e, quando aceitar crédito, o limite e o dia de
 * vencimento da fatura.
 */
public class VincularInstituicaoDto {

    private Set<TipoMovimento> tiposAceitos;
    private BigDecimal limiteCredito;
    private Double taxaJuros;
    private Integer diaVencimentoFatura;

    public VincularInstituicaoDto() {
    }

    public Set<TipoMovimento> getTiposAceitos() {
        return tiposAceitos;
    }

    public void setTiposAceitos(Set<TipoMovimento> tiposAceitos) {
        this.tiposAceitos = tiposAceitos;
    }

    public BigDecimal getLimiteCredito() {
        return limiteCredito;
    }

    public void setLimiteCredito(BigDecimal limiteCredito) {
        this.limiteCredito = limiteCredito;
    }

    public Double getTaxaJuros() {
        return taxaJuros;
    }

    public void setTaxaJuros(Double taxaJuros) {
        this.taxaJuros = taxaJuros;
    }

    public Integer getDiaVencimentoFatura() {
        return diaVencimentoFatura;
    }

    public void setDiaVencimentoFatura(Integer diaVencimentoFatura) {
        this.diaVencimentoFatura = diaVencimentoFatura;
    }
}

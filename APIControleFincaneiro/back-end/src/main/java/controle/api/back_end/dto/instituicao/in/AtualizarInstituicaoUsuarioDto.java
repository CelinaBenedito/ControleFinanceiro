package controle.api.back_end.dto.instituicao.in;

import controle.api.back_end.model.eventoFinanceiro.TipoMovimento;

import java.math.BigDecimal;
import java.util.Set;

public class AtualizarInstituicaoUsuarioDto {

    private BigDecimal limiteCredito;
    private Double taxaJuros;
    private Set<TipoMovimento> tiposAceitos;
    private Integer diaVencimentoFatura;

    public AtualizarInstituicaoUsuarioDto() {}

    public BigDecimal getLimiteCredito() { return limiteCredito; }
    public void setLimiteCredito(BigDecimal limiteCredito) { this.limiteCredito = limiteCredito; }

    public Double getTaxaJuros() { return taxaJuros; }
    public void setTaxaJuros(Double taxaJuros) { this.taxaJuros = taxaJuros; }

    public Set<TipoMovimento> getTiposAceitos() { return tiposAceitos; }
    public void setTiposAceitos(Set<TipoMovimento> tiposAceitos) { this.tiposAceitos = tiposAceitos; }

    public Integer getDiaVencimentoFatura() { return diaVencimentoFatura; }
    public void setDiaVencimentoFatura(Integer diaVencimentoFatura) { this.diaVencimentoFatura = diaVencimentoFatura; }
}


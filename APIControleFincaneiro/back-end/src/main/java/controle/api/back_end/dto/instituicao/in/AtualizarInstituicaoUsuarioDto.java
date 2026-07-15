package controle.api.back_end.dto.instituicao.in;

import java.math.BigDecimal;

public class AtualizarInstituicaoUsuarioDto {

    private BigDecimal limiteCredito;
    private Double taxaJuros;

    public AtualizarInstituicaoUsuarioDto() {}

    public BigDecimal getLimiteCredito() { return limiteCredito; }
    public void setLimiteCredito(BigDecimal limiteCredito) { this.limiteCredito = limiteCredito; }

    public Double getTaxaJuros() { return taxaJuros; }
    public void setTaxaJuros(Double taxaJuros) { this.taxaJuros = taxaJuros; }
}


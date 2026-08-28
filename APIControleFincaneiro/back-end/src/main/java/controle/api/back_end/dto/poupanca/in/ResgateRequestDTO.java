package controle.api.back_end.dto.poupanca.in;

import controle.api.back_end.model.eventoFinanceiro.TipoMovimento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Payload para resgatar (retirar) um valor de uma caixinha de poupança. */
public class ResgateRequestDTO {

    /** Valor a ser resgatado. Deve ser positivo e não pode superar o saldo disponível. */
    @NotNull
    @Positive
    private BigDecimal valor;

    /** Descrição livre opcional (ex.: "Compra do notebook"). */
    private String descricao;

    /** Data do resgate. Se nulo, usa a data de hoje. */
    private LocalDate dataResgate;

    /**
     * ID da InstituicaoUsuario para onde o dinheiro volta (conta corrente, carteira etc.).
     * Se não informado, usa a primeira instituição vinculada à caixinha.
     */
    private Integer instituicaoUsuarioId;

    /**
     * Forma de movimento do dinheiro na instituição de destino.
     * Se não informado, usa Debito por padrão.
     */
    private TipoMovimento tipoMovimento;

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public LocalDate getDataResgate() { return dataResgate; }
    public void setDataResgate(LocalDate dataResgate) { this.dataResgate = dataResgate; }

    public Integer getInstituicaoUsuarioId() { return instituicaoUsuarioId; }
    public void setInstituicaoUsuarioId(Integer instituicaoUsuarioId) { this.instituicaoUsuarioId = instituicaoUsuarioId; }

    public TipoMovimento getTipoMovimento() { return tipoMovimento; }
    public void setTipoMovimento(TipoMovimento tipoMovimento) { this.tipoMovimento = tipoMovimento; }
}


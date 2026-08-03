package controle.api.back_end.model.emprestimo;

/**
 * Status de pagamento do empréstimo.
 */
public enum StatusEmprestimo {
    /**
     * Empréstimo ainda não foi pago.
     */
    PENDENTE,

    /**
     * Foi pago parcialmente (com valor de quanto já foi pago).
     */
    PAGO_PARCIAL,

    /**
     * Empréstimo foi quitado completamente.
     */
    QUITADO
}


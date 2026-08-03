package controle.api.back_end.model.emprestimo;

/**
 * Define se o usuário emprestou dinheiro para alguém ou pediu emprestado.
 */
public enum TipoEmprestimo {
    /**
     * O usuário emprestou dinheiro/cartão para outra pessoa ou grupo.
     */
    EMPRESTEI,

    /**
     * O usuário pegou dinheiro emprestado de outra pessoa ou grupo.
     */
    PEDI_EMPRESTADO
}


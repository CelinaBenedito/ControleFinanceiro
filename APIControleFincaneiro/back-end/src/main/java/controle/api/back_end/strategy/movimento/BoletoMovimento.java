package controle.api.back_end.strategy.movimento;

import controle.api.back_end.exception.InstituicaoInativaException;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;


public class BoletoMovimento implements MovimentoStrategy{
    private final int parcelas;

    public BoletoMovimento(int parcelas) {
        this.parcelas = parcelas;
    }

    @Override
    public void validar(InstituicaoUsuario instituicao) {
        if (Boolean.FALSE.equals(instituicao.getIsAtivo())) {
            throw new InstituicaoInativaException(
                    "Instituição %s inativa, não é possível utilizá-la."
                            .formatted(instituicao.getInstituicao().getNome())
            );
        }
    }

    @Override
    public MovimentoResultado processar(EventoInstituicao evento) {
        double valorParcela = evento.getValor() / parcelas;
        MovimentoResultado movimentoResultado = new MovimentoResultado();

        movimentoResultado.setParcelas(parcelas);
        movimentoResultado.setValorParcela(valorParcela);
        movimentoResultado.setEvento(evento);

        return movimentoResultado;
    }
}

package controle.api.back_end.strategy.movimento;

import controle.api.back_end.exception.InstituicaoInativaException;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;

public class PixMovimento implements MovimentoStrategy {
    @Override
    public void validar(InstituicaoUsuario instituicao) {
        if (!instituicao.getIsAtivo()) {
            throw new InstituicaoInativaException(
                    "Instituição %s inativa, não é possível utiliza-lá."
                            .formatted(
                                    instituicao
                                            .getInstituicao()
                                            .getNome()
                            )
            );
        }
        // TODO: validar se instituição suporta Pix
    }

    @Override
    public MovimentoResultado processar(EventoInstituicao evento) {
        return new MovimentoResultado(evento);
    }
}


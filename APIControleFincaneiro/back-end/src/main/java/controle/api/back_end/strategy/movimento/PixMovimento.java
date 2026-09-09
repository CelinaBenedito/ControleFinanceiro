package controle.api.back_end.strategy.movimento;

import controle.api.back_end.exception.InstituicaoInativaException;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.eventoFinanceiro.TipoMovimento;
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
        if (instituicao.getTiposAceitos() == null || !instituicao.getTiposAceitos().contains(TipoMovimento.Pix)) {
            throw new IllegalArgumentException(
                    "Instituição %s não aceita movimentação em Pix."
                            .formatted(instituicao.getInstituicao().getNome())
            );
        }
    }

    @Override
    public MovimentoResultado processar(EventoInstituicao evento) {
        return new MovimentoResultado(evento);
    }
}


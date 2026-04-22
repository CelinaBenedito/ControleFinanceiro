package controle.api.back_end.strategy.movimento;

import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;

public interface MovimentoStrategy {
    void validar(InstituicaoUsuario instituicao);
    MovimentoResultado processar(EventoInstituicao evento);
}


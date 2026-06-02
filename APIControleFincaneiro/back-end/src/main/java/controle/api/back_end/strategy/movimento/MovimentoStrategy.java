package controle.api.back_end.strategy.movimento;

import controle.api.back_end.domain.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.domain.instituicao.InstituicaoUsuario;

public interface MovimentoStrategy {
    void validar(InstituicaoUsuario instituicao);
    MovimentoResultado processar(EventoInstituicao evento);
}


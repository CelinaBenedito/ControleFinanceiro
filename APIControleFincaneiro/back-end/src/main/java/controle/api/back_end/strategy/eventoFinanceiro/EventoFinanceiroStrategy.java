package controle.api.back_end.strategy.eventoFinanceiro;

import controle.api.back_end.domain.eventoFinanceiro.EventoDetalhe;
import controle.api.back_end.domain.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.domain.eventoFinanceiro.EventoInstituicao;

import java.util.List;

public interface EventoFinanceiroStrategy {
    Registro processar(EventoFinanceiro evento, List<EventoInstituicao> instituicoes, EventoDetalhe detalhe);
}


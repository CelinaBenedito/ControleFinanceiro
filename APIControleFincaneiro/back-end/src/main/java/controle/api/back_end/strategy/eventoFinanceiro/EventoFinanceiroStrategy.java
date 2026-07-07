package controle.api.back_end.strategy.eventoFinanceiro;

import controle.api.back_end.model.eventoFinanceiro.EventoDetalhe;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;

import java.util.List;

public interface EventoFinanceiroStrategy {
    Registro processar(EventoFinanceiro evento, List<EventoInstituicao> instituicoes, EventoDetalhe detalhe);
}


package controle.api.back_end.strategy.eventoFinanceiro;

import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;

public interface EventoFinanceiroStrategy {
    void processar(EventoFinanceiro evento);
}

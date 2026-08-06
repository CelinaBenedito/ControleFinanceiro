package controle.api.back_end.factory;

import controle.api.back_end.model.eventoFinanceiro.Tipo;
import controle.api.back_end.strategy.eventoFinanceiro.*;
import org.springframework.stereotype.Component;

@Component
public class EventoFinanceiroFactory {

    private final GastoEvento gastoEvento;
    private final RecebimentoEvento recebimentoEvento;
    private final TransferenciaEvento transferenciaEvento;
    private final PoupancaEvento poupancaEvento;
    private final EmprestimoEvento emprestimoEvento;

    public EventoFinanceiroFactory(GastoEvento gastoEvento,
                                  RecebimentoEvento recebimentoEvento,
                                  TransferenciaEvento transferenciaEvento,
                                  PoupancaEvento poupancaEvento,
                                  EmprestimoEvento emprestimoEvento) {
        this.gastoEvento = gastoEvento;
        this.recebimentoEvento = recebimentoEvento;
        this.transferenciaEvento = transferenciaEvento;
        this.poupancaEvento = poupancaEvento;
        this.emprestimoEvento = emprestimoEvento;
    }

    public EventoFinanceiroStrategy getStrategy(Tipo tipo){
        return switch (tipo){
            case Gasto -> gastoEvento;
            case Recebimento -> recebimentoEvento;
            case Transferencia -> transferenciaEvento;
            case Poupanca -> poupancaEvento;
            case Emprestimo -> emprestimoEvento;
        };
    }

}

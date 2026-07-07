package controle.api.back_end.factory;

import controle.api.back_end.model.eventoFinanceiro.Tipo;
import controle.api.back_end.strategy.eventoFinanceiro.*;
import org.springframework.stereotype.Component;

@Component
    public class EventoFinanceiroFactory {

        public EventoFinanceiroStrategy getStrategy(Tipo tipo){
            return switch (tipo){
                case Gasto -> new GastoEvento();
                case Recebimento -> new RecebimentoEvento();
                case Transferencia -> new TransferenciaEvento();
                case Poupanca -> new PoupancaEvento();
                case Emprestimo -> new EmprestimoEvento();
            };
        }

    }

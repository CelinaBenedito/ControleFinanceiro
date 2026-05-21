package controle.api.back_end.factory;

import controle.api.back_end.model.eventoFinanceiro.recorrenciaFinanceira.Periodicidade;
import controle.api.back_end.strategy.recorrenciaFinanceira.*;
import org.springframework.stereotype.Component;

@Component
public class RecorrenciaFactory {

    public RecorrenciaStrategy getStrategy(Periodicidade periodicidade){
        return switch (periodicidade){
            case Diario -> new RecorrenciaDiaria();
            case Semanal -> new RecorrenciaSemanal();
            case Mensal -> new RecorrenciaMensal();
            case Anual -> new RecorrenciaAnual();
        };
    }
}

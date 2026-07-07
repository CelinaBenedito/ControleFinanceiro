package controle.api.back_end.strategy.recorrenciaFinanceira;

import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.recorrenciaFinanceira.RecorrenciaFinanceira;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public interface RecorrenciaStrategy {
    List<EventoFinanceiro> gerarEventos(RecorrenciaFinanceira recorrencia, LocalDate limite);
}


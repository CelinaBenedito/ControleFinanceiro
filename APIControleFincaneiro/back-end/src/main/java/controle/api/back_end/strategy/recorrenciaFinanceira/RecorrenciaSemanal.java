package controle.api.back_end.strategy.recorrenciaFinanceira;

import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.recorrenciaFinanceira.RecorrenciaFinanceira;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RecorrenciaSemanal implements RecorrenciaStrategy{
    @Override
    public List<EventoFinanceiro> gerarEventos(RecorrenciaFinanceira recorrencia, LocalDate limite) {
        List<EventoFinanceiro> eventos = new ArrayList<>();
        LocalDate data = recorrencia.getDataInicio();

        while (!data.isAfter(limite)) {
            if (recorrencia.getDiasDaSemana().contains(data.getDayOfWeek())) {
                EventoFinanceiro evento = new EventoFinanceiro();
                evento.setUsuario(recorrencia.getUsuario());
                evento.setTipo(recorrencia.getTipo());
                evento.setValor(recorrencia.getValor());
                evento.setDescricao(recorrencia.getDescricao());
                evento.setDataEvento(data);
                evento.setDataRegistro(LocalDateTime.now());
                eventos.add(evento);
            }
            data = data.plusDays(1);
        }
        return eventos;
    }
}

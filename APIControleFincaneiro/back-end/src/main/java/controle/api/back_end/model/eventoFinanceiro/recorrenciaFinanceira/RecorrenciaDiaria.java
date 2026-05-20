package controle.api.back_end.model.eventoFinanceiro.recorrenciaFinanceira;

import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RecorrenciaDiaria extends RecorrenciaFinanceira{
    private Integer intervaloDias = 1;
    @Override
    public List<EventoFinanceiro> gerarEventos(LocalDate limite) {
        List<EventoFinanceiro> eventos = new ArrayList<>();
        LocalDate data = getDataInicio();

        while (!data.isAfter(limite)) {
            eventos.add(criarEvento(data));
            data = data.plusDays(intervaloDias);
        }
        return eventos;
    }

    private EventoFinanceiro criarEvento(LocalDate data) {
        EventoFinanceiro evento = new EventoFinanceiro();
        evento.setUsuario(getUsuario());
        evento.setTipo(getTipo());
        evento.setValor(getValor());
        evento.setDescricao(getDescricao());
        evento.setDataEvento(data);
        evento.setDataRegistro(LocalDateTime.now());
        return evento;
    }
}

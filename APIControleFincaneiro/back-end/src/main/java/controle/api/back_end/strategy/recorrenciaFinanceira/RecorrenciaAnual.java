package controle.api.back_end.strategy.recorrenciaFinanceira;

import controle.api.back_end.domain.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.domain.eventoFinanceiro.recorrenciaFinanceira.RecorrenciaFinanceira;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RecorrenciaAnual implements RecorrenciaStrategy{

    @Override
    public List<EventoFinanceiro> gerarEventos(RecorrenciaFinanceira recorrencia, LocalDate limite) {
        List<EventoFinanceiro> eventos = new ArrayList<>();
        LocalDate dataInicial = recorrencia.getDataInicio();

        int intervalo = recorrencia.getIntervalo() != null ? recorrencia.getIntervalo() : 1;
        int dia = recorrencia.getDia() != null ? recorrencia.getDia() : dataInicial.getDayOfMonth();

        while (!dataInicial.isAfter(limite)) {
            LocalDate dataEvento = dataInicial.withDayOfMonth(Math.min(dia, dataInicial.lengthOfMonth()));

            EventoFinanceiro evento = new EventoFinanceiro();
            evento.setUsuario(recorrencia.getUsuario());
            evento.setTipo(recorrencia.getTipo());
            evento.setValor(recorrencia.getValor());
            evento.setDescricao(recorrencia.getDescricao());
            evento.setDataEvento(dataEvento);
            evento.setDataRegistro(LocalDateTime.now());

            eventos.add(evento);
            dataInicial = dataInicial.plusYears(intervalo);
        }
        return eventos;
    }
}

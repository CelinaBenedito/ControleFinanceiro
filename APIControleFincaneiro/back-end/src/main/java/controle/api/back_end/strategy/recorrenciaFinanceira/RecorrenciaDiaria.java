    package controle.api.back_end.strategy.recorrenciaFinanceira;

    import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
    import controle.api.back_end.model.eventoFinanceiro.recorrenciaFinanceira.RecorrenciaFinanceira;

    import java.time.LocalDate;
    import java.time.LocalDateTime;
    import java.util.ArrayList;
    import java.util.List;

    public class RecorrenciaDiaria implements RecorrenciaStrategy{
        @Override
        public List<EventoFinanceiro> gerarEventos(RecorrenciaFinanceira recorrencia, LocalDate limite) {
            List<EventoFinanceiro> eventos = new ArrayList<>();
            LocalDate dataInicial = recorrencia.getDataInicio();
            int intervalo = recorrencia.getIntervalo() != null ? recorrencia.getIntervalo() : 1;

            while (!dataInicial.isAfter(limite)) {
                EventoFinanceiro evento = new EventoFinanceiro();
                evento.setUsuario(recorrencia.getUsuario());
                evento.setTipo(recorrencia.getTipo());
                evento.setValor(recorrencia.getValor());
                evento.setDescricao(recorrencia.getDescricao());
                evento.setDataEvento(dataInicial);
                evento.setDataRegistro(LocalDateTime.now());

                eventos.add(evento);
                dataInicial = dataInicial.plusDays(intervalo);
            }
            return eventos;
        }
    }

package controle.api.back_end.strategy.eventoFinanceiro;

import controle.api.back_end.model.eventoFinanceiro.EventoDetalhe;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;

import java.util.List;

public class Registro {
    private List<EventoFinanceiro> eventosFinanceiros;
    private List<EventoInstituicao> eventosInstituicoes;
    private EventoDetalhe eventoDetalhe;

    public Registro(List<EventoFinanceiro> eventosFinanceiros,
                    List<EventoInstituicao> eventosInstituicoes,
                    EventoDetalhe eventoDetalhe) {
        this.eventosFinanceiros = eventosFinanceiros;
        this.eventosInstituicoes = eventosInstituicoes;
        this.eventoDetalhe = eventoDetalhe;
    }

    public Registro(List<EventoFinanceiro> eventosFinanceiros,
                    List<EventoInstituicao> eventosInstituicoes) {
        this(eventosFinanceiros, eventosInstituicoes, null);
    }

    public Registro() {
    }

    public List<EventoFinanceiro> getEventosFinanceiros() { return eventosFinanceiros; }
    public List<EventoInstituicao> getEventosInstituicoes() { return eventosInstituicoes; }
    public EventoDetalhe getEventoDetalhe() { return eventoDetalhe; }

    public void setEventosFinanceiros(List<EventoFinanceiro> eventosFinanceiros) {
        this.eventosFinanceiros = eventosFinanceiros;
    }

    public void setEventosInstituicoes(List<EventoInstituicao> eventosInstituicoes) {
        this.eventosInstituicoes = eventosInstituicoes;
    }

    public void setEventoDetalhe(EventoDetalhe eventoDetalhe) {
        this.eventoDetalhe = eventoDetalhe;
    }
}

package controle.api.back_end.strategy.eventoFinanceiro;

import controle.api.back_end.model.eventoFinanceiro.EventoDetalhe;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;

import java.util.List;
import java.util.Map;

public class Registro {
    private List<EventoFinanceiro> eventosFinanceiros;
    private Map<EventoFinanceiro, List<EventoInstituicao>> instituicoesPorEvento;
    private Map<EventoFinanceiro, EventoDetalhe> detalhePorEvento;

    public Registro(List<EventoFinanceiro> eventosFinanceiros,
                    Map<EventoFinanceiro, List<EventoInstituicao>> instituicoesPorEvento,
                    Map<EventoFinanceiro, EventoDetalhe> detalhePorEvento) {
        this.eventosFinanceiros = eventosFinanceiros;
        this.instituicoesPorEvento = instituicoesPorEvento;
        this.detalhePorEvento = detalhePorEvento;
    }

    public List<EventoFinanceiro> getEventosFinanceiros() {
        return eventosFinanceiros;
    }

    public Map<EventoFinanceiro, List<EventoInstituicao>> getInstituicoesPorEvento() {
        return instituicoesPorEvento;
    }

    public Map<EventoFinanceiro, EventoDetalhe> getDetalhePorEvento() {
        return detalhePorEvento;
    }
}

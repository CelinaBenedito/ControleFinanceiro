package controle.api.back_end.strategy.eventoFinanceiro;

import controle.api.back_end.model.eventoFinanceiro.EventoDetalhe;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.eventoFinanceiro.Tipo;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GastoEvento implements EventoFinanceiroStrategy {

    @Override
    public Registro processar(EventoFinanceiro evento,
                              List<EventoInstituicao> eventoInstituicoes,
                              EventoDetalhe eventoDetalhe) {

        List<EventoFinanceiro> eventos = new ArrayList<>();
        Map<EventoFinanceiro, List<EventoInstituicao>> instituicoesPorEvento = new HashMap<>();
        Map<EventoFinanceiro, EventoDetalhe> detalhePorEvento = new HashMap<>();

        // Criar evento principal de gasto
        EventoFinanceiro gasto = new EventoFinanceiro();
        gasto.setUsuario(evento.getUsuario());
        gasto.setTipo(Tipo.Gasto);
        gasto.setValor(evento.getValor());
        gasto.setDescricao(evento.getDescricao());
        gasto.setDataEvento(evento.getDataEvento());
        gasto.setDataRegistro(LocalDateTime.now());
        eventos.add(gasto);

        // Vincular instituições ao gasto
        List<EventoInstituicao> insts = new ArrayList<>();
        for (EventoInstituicao inst : eventoInstituicoes) {
            inst.setEventoFinanceiro(gasto);
            insts.add(inst);
        }
        instituicoesPorEvento.put(gasto, insts);

        // Vincular detalhe único ao gasto
        if (eventoDetalhe != null) {
            eventoDetalhe.setEventoFinanceiro(gasto);
            detalhePorEvento.put(gasto, eventoDetalhe);
        }

        return new Registro(eventos, instituicoesPorEvento, detalhePorEvento);
    }
}
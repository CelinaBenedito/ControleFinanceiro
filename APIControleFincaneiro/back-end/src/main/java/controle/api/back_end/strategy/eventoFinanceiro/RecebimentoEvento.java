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
public class RecebimentoEvento implements EventoFinanceiroStrategy {

    @Override
    public Registro processar(EventoFinanceiro evento,
                              List<EventoInstituicao> eventoInstituicoes,
                              EventoDetalhe eventoDetalhe) {

        List<EventoFinanceiro> eventos = new ArrayList<>();
        Map<EventoFinanceiro, List<EventoInstituicao>> instituicoesPorEvento = new HashMap<>();
        Map<EventoFinanceiro, EventoDetalhe> detalhePorEvento = new HashMap<>();

        // 1. Evento principal (recebimento)
        EventoFinanceiro recebimento = new EventoFinanceiro();
        recebimento.setUsuario(evento.getUsuario());
        recebimento.setTipo(Tipo.Recebimento);
        recebimento.setValor(evento.getValor());
        recebimento.setDescricao(evento.getDescricao());
        recebimento.setDataEvento(evento.getDataEvento());
        recebimento.setDataRegistro(LocalDateTime.now());
        eventos.add(recebimento);

        // 2. Vincular instituições ao recebimento
        List<EventoInstituicao> insts = new ArrayList<>();
        for (EventoInstituicao inst : eventoInstituicoes) {
            inst.setEventoFinanceiro(recebimento);
            insts.add(inst);
        }
        instituicoesPorEvento.put(recebimento, insts);

        // 3. Vincular detalhe único ao recebimento
        if (eventoDetalhe != null) {
            eventoDetalhe.setEventoFinanceiro(recebimento);
            detalhePorEvento.put(recebimento, eventoDetalhe);
        }

        // 4. Retornar registro completo
        return new Registro(eventos, instituicoesPorEvento, detalhePorEvento);
    }
}



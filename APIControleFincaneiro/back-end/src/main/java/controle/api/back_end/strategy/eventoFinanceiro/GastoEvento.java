package controle.api.back_end.strategy.eventoFinanceiro;

import controle.api.back_end.model.eventoFinanceiro.EventoDetalhe;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.eventoFinanceiro.Tipo;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class GastoEvento implements EventoFinanceiroStrategy {

    @Override
    public Registro processar(EventoFinanceiro evento,
                              List<EventoInstituicao> eventoInstituicoes,
                              EventoDetalhe eventoDetalhe) {

        List<EventoFinanceiro> eventos = new ArrayList<>();
        List<EventoInstituicao> instituicoes = new ArrayList<>();

        EventoFinanceiro gasto = new EventoFinanceiro();
        gasto.setUsuario(evento.getUsuario());
        gasto.setTipo(Tipo.Gasto);
        gasto.setValor(evento.getValor());
        gasto.setDescricao(evento.getDescricao());
        gasto.setDataEvento(evento.getDataEvento());
        gasto.setDataRegistro(LocalDateTime.now());
        eventos.add(gasto);

        for (EventoInstituicao inst : eventoInstituicoes) {
            inst.setEventoFinanceiro(gasto);
            instituicoes.add(inst);
        }

        return new Registro(eventos, instituicoes, eventoDetalhe);
    }
}


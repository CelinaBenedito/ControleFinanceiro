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
public class PoupancaEvento implements EventoFinanceiroStrategy {

    @Override
    public Registro processar(EventoFinanceiro evento,
                              List<EventoInstituicao> eventoInstituicoes,
                              EventoDetalhe eventoDetalhe) {

        List<EventoFinanceiro> eventos = new ArrayList<>();
        Map<EventoFinanceiro, List<EventoInstituicao>> instituicoesPorEvento = new HashMap<>();
        Map<EventoFinanceiro, EventoDetalhe> detalhePorEvento = new HashMap<>();

        // 1. Evento inicial (aplicação)
        EventoFinanceiro aplicacao = new EventoFinanceiro();
        aplicacao.setUsuario(evento.getUsuario());
        aplicacao.setTipo(Tipo.Poupanca);
        aplicacao.setValor(evento.getValor());
        aplicacao.setDescricao("Aplicação em poupança");
        aplicacao.setDataEvento(evento.getDataEvento());
        aplicacao.setDataRegistro(LocalDateTime.now());
        eventos.add(aplicacao);

        // Instituições vinculadas à aplicação
        List<EventoInstituicao> instsAplicacao = new ArrayList<>();
        if (!eventoInstituicoes.isEmpty()) {
            EventoInstituicao instAplicacao = eventoInstituicoes.getFirst();
            instAplicacao.setEventoFinanceiro(aplicacao);
            instAplicacao.setValor(evento.getValor());
            instAplicacao.setParcelas(1);
            instsAplicacao.add(instAplicacao);
        }
        instituicoesPorEvento.put(aplicacao, instsAplicacao);

        // Detalhe único vinculado à aplicação
        if (eventoDetalhe != null) {
            eventoDetalhe.setEventoFinanceiro(aplicacao);
            detalhePorEvento.put(aplicacao, eventoDetalhe);
        }

        // 2. Projeção de rendimento
        double principal = evento.getValor();
        double taxa = evento.getTaxaRendimento(); // informado pelo usuário
        int tempoProjecao = evento.getTempoProjecao();   // em meses

        for (int mes = 1; mes <= tempoProjecao; mes++) {
            double montante = principal * Math.pow(1 + taxa, mes);
            double rendimento = montante - principal;

            EventoFinanceiro rendimentoEvento = new EventoFinanceiro();
            rendimentoEvento.setUsuario(evento.getUsuario());
            rendimentoEvento.setTipo(Tipo.Recebimento);
            rendimentoEvento.setValor(rendimento);
            rendimentoEvento.setDescricao("Rendimento projetado mês " + mes);
            rendimentoEvento.setDataEvento(evento.getDataEvento().plusMonths(mes));
            rendimentoEvento.setDataRegistro(LocalDateTime.now());
            eventos.add(rendimentoEvento);

            // Instituições vinculadas ao rendimento
            List<EventoInstituicao> instsRendimento = new ArrayList<>();
            if (!eventoInstituicoes.isEmpty()) {
                EventoInstituicao instRendimento = new EventoInstituicao();
                instRendimento.setEventoFinanceiro(rendimentoEvento);
                instRendimento.setInstituicaoUsuario(eventoInstituicoes.getFirst().getInstituicaoUsuario());
                instRendimento.setValor(rendimento);
                instRendimento.setParcelas(1);
                instsRendimento.add(instRendimento);
            }
            instituicoesPorEvento.put(rendimentoEvento, instsRendimento);

            // Detalhe único vinculado ao rendimento (se necessário)
            if (eventoDetalhe != null) {
                detalhePorEvento.put(rendimentoEvento, eventoDetalhe);
            }
        }

        // 3. Retornar registro completo
        return new Registro(eventos, instituicoesPorEvento, detalhePorEvento);
    }
}
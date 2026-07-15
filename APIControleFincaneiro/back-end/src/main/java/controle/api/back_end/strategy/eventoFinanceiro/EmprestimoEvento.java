package controle.api.back_end.strategy.eventoFinanceiro;

import controle.api.back_end.model.eventoFinanceiro.EventoDetalhe;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.eventoFinanceiro.Tipo;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EmprestimoEvento implements EventoFinanceiroStrategy {

    @Override
    public Registro processar(EventoFinanceiro evento,
                              List<EventoInstituicao> eventoInstituicoes,
                              EventoDetalhe eventoDetalhe) {

        List<EventoFinanceiro> eventos = new ArrayList<>();
        Map<EventoFinanceiro, List<EventoInstituicao>> instituicoesPorEvento = new HashMap<>();
        Map<EventoFinanceiro, EventoDetalhe> detalhePorEvento = new HashMap<>();

        // Crédito principal
        EventoFinanceiro credito = new EventoFinanceiro();
        credito.setUsuario(evento.getUsuario());
        credito.setTipo(Tipo.Recebimento);
        credito.setValor(evento.getValor());
        credito.setDescricao("Crédito de empréstimo recebido");
        credito.setDataEvento(evento.getDataEvento());
        credito.setDataRegistro(LocalDateTime.now());
        eventos.add(credito);

        List<EventoInstituicao> instsCredito = new ArrayList<>();
        if (!eventoInstituicoes.isEmpty()) {
            EventoInstituicao instCredito = new EventoInstituicao();
            instCredito.setEventoFinanceiro(credito);
            instCredito.setInstituicaoUsuario(eventoInstituicoes.getFirst().getInstituicaoUsuario());
            instCredito.setTipoMovimento(eventoInstituicoes.getFirst().getTipoMovimento());
            instCredito.setValor(evento.getValor());
            instCredito.setParcelas(1);
            instsCredito.add(instCredito);

            instituicoesPorEvento.put(credito, instsCredito);
        }

        // Detalhe único vinculado ao crédito
        if (eventoDetalhe != null) {
            detalhePorEvento.put(credito, eventoDetalhe);
        }

        // Parcelas de débito — calcula valor total a pagar levando em conta a taxa
        if (!eventoInstituicoes.isEmpty()) {
            int parcelas = eventoInstituicoes.getFirst().getParcelas();
            // taxaRendimento = taxa de juros total em % (ex: 33.33 significa pagar 33,33% a mais)
            double taxaPercentual = evento.getTaxaRendimento() != null ? evento.getTaxaRendimento() : 0.0;
            double valorTotal = evento.getValor() * (1.0 + taxaPercentual / 100.0);
            BigDecimal valorParcela = BigDecimal.valueOf(valorTotal)
                    .divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_UP);

            for (int i = 1; i <= parcelas; i++) {
                EventoFinanceiro debito = new EventoFinanceiro();
                debito.setUsuario(evento.getUsuario());
                debito.setTipo(Tipo.Gasto);
                debito.setValor(valorParcela.doubleValue());
                debito.setDescricao("Parcela " + i + "/" + parcelas + " do empréstimo");
                debito.setDataEvento(evento.getDataEvento().plusMonths(i));
                debito.setDataRegistro(LocalDateTime.now());
                eventos.add(debito);

                List<EventoInstituicao> instsDebito = new ArrayList<>();
                EventoInstituicao instDebito = new EventoInstituicao();
                instDebito.setEventoFinanceiro(debito);
                instDebito.setInstituicaoUsuario(eventoInstituicoes.get(0).getInstituicaoUsuario());
                instDebito.setTipoMovimento(eventoInstituicoes.get(0).getTipoMovimento());
                instDebito.setValor(valorParcela.doubleValue());
                instDebito.setParcelas(i);
                instsDebito.add(instDebito);

                instituicoesPorEvento.put(debito, instsDebito);

                if (eventoDetalhe != null) {
                    detalhePorEvento.put(debito, eventoDetalhe);
                }
            }
        }
        return new Registro(eventos, instituicoesPorEvento, detalhePorEvento);
    }
}


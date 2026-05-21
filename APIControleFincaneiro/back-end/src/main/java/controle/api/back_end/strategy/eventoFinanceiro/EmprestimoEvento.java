package controle.api.back_end.strategy.eventoFinanceiro;

import controle.api.back_end.model.eventoFinanceiro.EventoDetalhe;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.eventoFinanceiro.Tipo;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class EmprestimoEvento implements EventoFinanceiroStrategy{

    public Registro processar(EventoFinanceiro evento, List<EventoInstituicao> eventoInstituicoes, EventoDetalhe eventoDetalhe) {
        List<EventoFinanceiro> eventos = new ArrayList<>();
        List<EventoInstituicao> instituicoes = new ArrayList<>();

        EventoFinanceiro credito = new EventoFinanceiro();
        credito.setUsuario(evento.getUsuario());
        credito.setTipo(Tipo.Recebimento);
        credito.setValor(evento.getValor());
        credito.setDescricao("Crédito de empréstimo recebido");
        credito.setDataEvento(evento.getDataEvento());
        credito.setDataRegistro(LocalDateTime.now());
        eventos.add(credito);

        if (!eventoInstituicoes.isEmpty()) {
            EventoInstituicao instCredito = new EventoInstituicao();
            instCredito.setEventoFinanceiro(credito);
            instCredito.setInstituicaoUsuario(eventoInstituicoes.getFirst().getInstituicaoUsuario());
            instCredito.setTipoMovimento(eventoInstituicoes.getFirst().getTipoMovimento());
            instCredito.setValor(evento.getValor());
            instCredito.setParcelas(1);
            instituicoes.add(instCredito);
            
            int parcelas = eventoInstituicoes.getFirst().getParcelas();
            BigDecimal valorParcela = BigDecimal.valueOf(evento.getValor())
                    .divide(BigDecimal.valueOf(parcelas));


            for (int i = 1; i <= parcelas; i++) {
                EventoFinanceiro debito = new EventoFinanceiro();
                debito.setUsuario(evento.getUsuario());
                debito.setTipo(Tipo.Gasto);
                debito.setValor(valorParcela.doubleValue());
                debito.setDescricao("Parcela " + i + " do empréstimo");
                debito.setDataEvento(evento.getDataEvento().plusMonths(i));
                debito.setDataRegistro(LocalDateTime.now());
                eventos.add(debito);

                EventoInstituicao instDebito = new EventoInstituicao();
                instDebito.setEventoFinanceiro(debito);
                instDebito.setInstituicaoUsuario(eventoInstituicoes.get(0).getInstituicaoUsuario());
                instDebito.setTipoMovimento(eventoInstituicoes.get(0).getTipoMovimento());
                instDebito.setValor(valorParcela.doubleValue());
                instDebito.setParcelas(i);
                instituicoes.add(instDebito);
            }
        }

        return new Registro(eventos, instituicoes, eventoDetalhe);
    }
}

package controle.api.back_end.strategy.eventoFinanceiro;

import controle.api.back_end.dto.registros.in.TransferenciaDTO;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.eventoFinanceiro.Tipo;
import controle.api.back_end.model.eventoFinanceiro.TipoMovimento;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class TransferenciaEvento implements EventoFinanceiroStrategy {

    @Override
    public Registro processar(EventoFinanceiro evento, EventoInstituicao antigaInstituicao, InstituicaoUsuario destino) {
//        if (destino.getUsuario().equals(evento.getUsuario())) {
//            List<EventoFinanceiro> recebimento = new ArrayList<>();
//            recebimento.add(new EventoFinanceiro());
//
//            List<EventoInstituicao> recebimentoInstituicao = new ArrayList<>();
//            recebimentoInstituicao.setInstituicaoUsuario(destino);
//            recebimentoInstituicao.setParcelas(1);
//            recebimentoInstituicao.setEventoFinanceiro(evento);
//            recebimentoInstituicao.setValor(evento.getValor());
//            recebimentoInstituicao.setTipoMovimento(antigaInstituicao.getTipoMovimento());
//
//            recebimento.getFirst().setUsuario(destino.getUsuario());
//            recebimento.getFirst().setDataEvento(evento.getDataEvento());
//            recebimento.getFirst().setDataRegistro(LocalDateTime.now());
//            recebimento.getFirst().setTipo(Tipo.Recebimento);
//
//            recebimento.getFirst().setValor(evento.getValor());
//
//            recebimento.getFirst().setDescricao("Transferência recebida da instituição %s"
//                    .formatted(antigaInstituicao.getInstituicaoUsuario()
//                            .getInstituicao()
//                            .getNome()
//                    )
//            );
//            System.out.println("Transferência interna registrada como recebimento.");
//            return new Registro(recebimento,recebimentoInstituicao);
//        } else {
//            System.out.println("Transferência externa para outro usuário.");
//            return new Registro();
//        }
        return new Registro();
    }
}

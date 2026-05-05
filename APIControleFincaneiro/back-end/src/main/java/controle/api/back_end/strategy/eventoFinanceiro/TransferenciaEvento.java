package controle.api.back_end.strategy.eventoFinanceiro;

import controle.api.back_end.dto.registros.in.TransferenciaDTO;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.eventoFinanceiro.Tipo;
import controle.api.back_end.model.eventoFinanceiro.TipoMovimento;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import org.springframework.stereotype.Component;

@Component
public class TransferenciaEvento implements EventoFinanceiroStrategy {

    @Override
    public TransferenciaDTO processar(EventoFinanceiro evento, EventoInstituicao antigaInstituicao, InstituicaoUsuario destino) {
        if (destino.getUsuario().equals(evento.getUsuario())) {
            EventoFinanceiro recebimento = new EventoFinanceiro();

            EventoInstituicao recebimentoInstituicao = new EventoInstituicao();
            recebimentoInstituicao.setInstituicaoUsuario(destino);
            recebimentoInstituicao.setParcelas(1);
            recebimentoInstituicao.setEventoFinanceiro(evento);
            recebimentoInstituicao.setValor(evento.getValor());
            recebimentoInstituicao.setTipoMovimento(antigaInstituicao.getTipoMovimento());

            recebimento.setUsuario(destino.getUsuario());

            recebimento.setTipo(Tipo.Recebimento);

            recebimento.setValor(evento.getValor());

            recebimento.setDescricao("Transferência recebida da instituição %s"
                    .formatted(antigaInstituicao.getInstituicaoUsuario()
                            .getInstituicao()
                            .getNome()
                    )
            );
            System.out.println("Transferência interna registrada como recebimento.");
            return new TransferenciaDTO(recebimentoInstituicao,recebimento);
        } else {
            System.out.println("Transferência externa para outro usuário.");
            return new TransferenciaDTO();
        }
    }
}

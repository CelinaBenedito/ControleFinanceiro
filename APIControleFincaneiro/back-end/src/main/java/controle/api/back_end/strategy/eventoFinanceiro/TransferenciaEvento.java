package controle.api.back_end.strategy.eventoFinanceiro;

import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.Tipo;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;

public class TransferenciaEvento implements EventoFinanceiroStrategy {

    private InstituicaoUsuario destino;

    public TransferenciaEvento(InstituicaoUsuario destino) {
        this.destino = destino;
    }

    @Override
    public void processar(EventoFinanceiro evento) {
        // se destino for do mesmo usuário → criar recebimento automático
        if (destino.getUsuario().equals(evento.getUsuario())) {
            EventoFinanceiro recebimento = new EventoFinanceiro();
            recebimento.setUsuario(destino.getUsuario());
            recebimento.setTipo(Tipo.Recebimento);
            recebimento.setValor(evento.getValor());
//            recebimento.setDescricao("Transferência recebida da instituição "
//                    + evento.getEventoInstituicao().getInstituicaoUsuario().getInstituicao().getNome());
            System.out.println("Transferência interna registrada como recebimento.");
        } else {
            System.out.println("Transferência externa para outro usuário.");
        }
    }
}

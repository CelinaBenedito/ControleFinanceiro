package controle.api.back_end.strategy.eventoFinanceiro;

import controle.api.back_end.dto.registros.in.TransferenciaDTO;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;

public interface EventoFinanceiroStrategy {
    TransferenciaDTO processar(EventoFinanceiro evento, EventoInstituicao eventoInstituicao, InstituicaoUsuario destino);
}

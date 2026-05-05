package controle.api.back_end.dto.registros.in;

import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;

public class TransferenciaDTO {
    private EventoInstituicao eventoInstituicao;
    private EventoFinanceiro eventoFinanceiro;

    public TransferenciaDTO(EventoInstituicao eventoInstituicao, EventoFinanceiro eventoFinanceiro) {
        this.eventoInstituicao = eventoInstituicao;
        this.eventoFinanceiro = eventoFinanceiro;
    }

    public TransferenciaDTO() {
    }

    public EventoInstituicao getEventoInstituicao() {
        return eventoInstituicao;
    }

    public void setEventoInstituicao(EventoInstituicao eventoInstituicao) {
        this.eventoInstituicao = eventoInstituicao;
    }

    public EventoFinanceiro getEventoFinanceiro() {
        return eventoFinanceiro;
    }

    public void setEventoFinanceiro(EventoFinanceiro eventoFinanceiro) {
        this.eventoFinanceiro = eventoFinanceiro;
    }
}

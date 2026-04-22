package controle.api.back_end.strategy.movimento;

import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;

public class MovimentoResultado {
    private EventoInstituicao evento;
    private int parcelas;
    private double valorParcela;

    public MovimentoResultado(EventoInstituicao evento) {
        this(evento, 1, evento.getValor());
    }

    public MovimentoResultado(EventoInstituicao evento, int parcelas, double valorParcela) {
        this.evento = evento;
        this.parcelas = parcelas;
        this.valorParcela = valorParcela;
    }

    public EventoInstituicao getEvento() {
        return evento;
    }

    public void setEvento(EventoInstituicao evento) {
        this.evento = evento;
    }

    public int getParcelas() {
        return parcelas;
    }

    public void setParcelas(int parcelas) {
        this.parcelas = parcelas;
    }

    public double getValorParcela() {
        return valorParcela;
    }

    public void setValorParcela(double valorParcela) {
        this.valorParcela = valorParcela;
    }
}

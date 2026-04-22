package controle.api.back_end.strategy.movimento;

import controle.api.back_end.exception.InstituicaoInativaException;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;

public class BoletoMovimento implements MovimentoStrategy{
    private int parcelas;

    public BoletoMovimento(int parcelas) {
        this.parcelas = parcelas;
    }

    @Override
    public void validar(InstituicaoUsuario instituicao) {
        if (!instituicao.getAtivo()) {
            throw new InstituicaoInativaException("Instituição %s inativa não pode realizar pagamento de boleto."
                    .formatted(instituicao.getInstituicao().getNome()));
        }
    }

    @Override
    public MovimentoResultado processar(EventoInstituicao evento) {
        double valorParcela = evento.getValor() / parcelas;
        System.out.println(STR."Processando boleto em \{parcelas} parcelas de R$ \{valorParcela}");
    }
}

package controle.api.back_end.strategy.movimento;

import controle.api.back_end.exception.InstituicaoInativaException;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;

import java.util.List;

public class VoucherMovimento implements MovimentoStrategy{
    private static final List<String> INSTITUICOES_VALIDAS =
            List.of("Alelo", "Pluxee", "Ticket", "Vale Refeição", "Vale Alimentação");

    @Override
    public void validar(InstituicaoUsuario instituicao) {
        if (!INSTITUICOES_VALIDAS.contains(instituicao.getInstituicao().getNome())) {
            throw new IllegalArgumentException("Instituição não permite uso de Voucher.");
        }
        if (!instituicao.getIsAtivo()) {
            throw new InstituicaoInativaException(
                    "Instituição %s inativa, não é possível utiliza-lá."
                            .formatted(
                                    instituicao
                                            .getInstituicao()
                                            .getNome()
                            )
            );
        }
    }

    @Override
    public MovimentoResultado processar(EventoInstituicao evento) {
        return new MovimentoResultado(evento);
    }
}

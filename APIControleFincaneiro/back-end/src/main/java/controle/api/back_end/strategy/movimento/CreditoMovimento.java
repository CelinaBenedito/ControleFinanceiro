package controle.api.back_end.strategy.movimento;

import controle.api.back_end.exception.InstituicaoInativaException;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;

    public class CreditoMovimento implements MovimentoStrategy {

        private int parcelas;

        public CreditoMovimento(int parcelas) {
            this.parcelas = parcelas;
        }

        @Override
        public void validar(InstituicaoUsuario instituicao) {
            if (!instituicao.getAtivo()) {
                throw new InstituicaoInativaException("Instituição %s inativa não pode realizar crédito.".formatted(instituicao.getInstituicao().getNome()));
            }
        }

        @Override
        public MovimentoResultado processar(EventoInstituicao evento) {
            double valorParcela = evento.getValor() / parcelas;
            System.out.println("Processando crédito em " + parcelas + " parcelas de R$ " + valorParcela);
            return new MovimentoResultado(evento);
        }
    }

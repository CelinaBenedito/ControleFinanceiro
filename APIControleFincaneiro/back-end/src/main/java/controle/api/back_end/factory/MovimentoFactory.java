package controle.api.back_end.factory;

import controle.api.back_end.domain.eventoFinanceiro.TipoMovimento;
import controle.api.back_end.strategy.movimento.*;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MovimentoFactory {

        public MovimentoStrategy getStrategy(TipoMovimento tipoMovimento,
                                             Map<String, Object> params) {
            return switch (tipoMovimento) {
                case Debito -> new DebitoMovimento();
                case Credito -> new CreditoMovimento((Integer) params.getOrDefault("parcelas", 1));
                case Pix -> new PixMovimento();
                case Boleto -> new BoletoMovimento((Integer) params.getOrDefault("parcelas", 1));
                case Voucher -> new VoucherMovimento();
                case Dinheiro -> new DinheiroMovimento();
            };
        }
}

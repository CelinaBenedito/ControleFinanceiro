package controle.api.back_end.utils;

import controle.api.back_end.model.eventoFinanceiro.TipoMovimento;
import controle.api.back_end.model.instituicao.Instituicao;

import java.util.HashSet;
import java.util.Set;

/**
 * Calcula os tipos de movimento aceitos por padrão para uma instituição, usado tanto ao
 * vincular uma nova instituição a um usuário quanto para preencher (backfill) vínculos
 * antigos que ainda não possuem essa configuração.
 * <p>
 * Instituições de voucher/benefício (Alelo, Pluxee, Ticket, VR, VA, etc) aceitam apenas
 * Voucher; as demais (bancos tradicionais) aceitam Débito, Crédito, Pix, Boleto e Dinheiro.
 */
public final class TiposAceitosPadraoUtil {

    private TiposAceitosPadraoUtil() {
    }

    public static Set<TipoMovimento> calcular(Instituicao instituicao) {
        if (instituicao != null && Boolean.TRUE.equals(instituicao.getIsVoucher())) {
            return new HashSet<>(Set.of(TipoMovimento.Voucher));
        }
        return new HashSet<>(Set.of(
                TipoMovimento.Debito, TipoMovimento.Credito, TipoMovimento.Pix,
                TipoMovimento.Boleto, TipoMovimento.Dinheiro));
    }
}

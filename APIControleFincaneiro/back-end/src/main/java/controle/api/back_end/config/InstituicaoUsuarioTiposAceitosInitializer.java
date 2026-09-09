package controle.api.back_end.config;

import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.repository.instituicao.InstituicaoUsuarioRepository;
import controle.api.back_end.utils.TiposAceitosPadraoUtil;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Preenche (backfill) os tipos de movimento aceitos ({@code tiposAceitos}) para vínculos
 * usuário-instituição já existentes, criados antes da introdução dessa configuração.
 * <p>
 * Sem este backfill, instituições vinculadas antes da feature ficariam sem nenhum tipo de
 * movimento aceito, e todas as strategies de movimento (Débito, Crédito, Pix, Boleto,
 * Dinheiro, Voucher) passariam a rejeitar novos lançamentos para essas instituições.
 * <p>
 * Roda uma única vez por startup e é idempotente: só atualiza registros com o conjunto vazio.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class InstituicaoUsuarioTiposAceitosInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final InstituicaoUsuarioRepository instituicaoUsuarioRepository;

    public InstituicaoUsuarioTiposAceitosInitializer(InstituicaoUsuarioRepository instituicaoUsuarioRepository) {
        this.instituicaoUsuarioRepository = instituicaoUsuarioRepository;
    }

    @Override
    @Transactional
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        try {
            List<InstituicaoUsuario> todos = instituicaoUsuarioRepository.findAll();
            int atualizados = 0;
            for (InstituicaoUsuario iu : todos) {
                if (iu.getTiposAceitos() == null || iu.getTiposAceitos().isEmpty()) {
                    iu.setTiposAceitos(TiposAceitosPadraoUtil.calcular(iu.getInstituicao()));
                    instituicaoUsuarioRepository.save(iu);
                    atualizados++;
                }
            }
            if (atualizados > 0) {
                System.out.println("[InstituicaoUsuarioTiposAceitosInitializer] " + atualizados
                        + " vínculo(s) usuário-instituição atualizados com tipos de movimento padrão.");
            }
        } catch (Exception e) {
            System.err.println("[InstituicaoUsuarioTiposAceitosInitializer] Erro ao preencher tipos aceitos: " + e.getMessage());
        }
    }
}

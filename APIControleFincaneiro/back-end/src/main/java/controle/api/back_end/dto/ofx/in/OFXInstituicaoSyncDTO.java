package controle.api.back_end.dto.ofx.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Dados de uma instituição financeira para sincronização OFX.
 *
 * <p>As credenciais NÃO devem ser salvas em banco de dados.
 * Devem vir do lado do cliente (ex: localStorage) e são passadas
 * apenas em memória via navigationSteps.
 */
public record OFXInstituicaoSyncDTO(
        @NotNull Integer instituicaoUsuarioId,
        @NotBlank String bankUrl,

        /**
         * Passos de navegação que o browser executará automaticamente.
         *
         * Se null, o browser abrirá em modo MANUAL — o usuário deverá
         * fazer o login e o download do OFX manualmente dentro do timeout.
         *
         * Acoes disponíveis: click | fill | wait | wait_for_selector | navigate | download
         */
        List<NavigationStepDTO> navigationSteps,

        String descricao
) {}


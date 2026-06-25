package controle.api.back_end.dto.ofx.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Payload enviado pelo frontend para disparar a captura de OFX.
 *
 * IMPORTANTE: As credenciais podem ser embutidas dentro de navigationSteps
 * (como texto nos steps do tipo "fill"). Elas trafegam apenas em memória e
 * NUNCA são persistidas — nem aqui, nem no Python.
 */
public record OFXSyncRequestDTO(
        @NotNull UUID userId,
        @NotNull Integer instituicaoUsuarioId,

        @NotBlank String bankUrl,

        /**
         * Passos de navegação que o Python executará no browser.
         * Se null, o browser abrirá a página de login e aguardará o usuário fazer
         * o login manualmente (modo interativo — recomendado para bancos com MFA).
         */
        List<NavigationStepDTO> navigationSteps,

        String description
) {}


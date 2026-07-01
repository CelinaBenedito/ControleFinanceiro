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
         * Endpoint Python a chamar. Padrao: /capture.
         * Alelo usa /capture/alelo, Nubank usa /capture/nubank/sync.
         * Para Alelo/Nubank, os navigationSteps devem usar selector={{CPF}} e selector={{SENHA}}
         * com os valores reais no campo text.
         */
        String pythonEndpoint,

        /**
         * Passos de navegação que o Python executará no browser.
         * Se null, o browser abrirá a página de login e aguardará o usuário fazer
         * o login manualmente (modo interativo — recomendado para bancos com MFA).
         *
         * Para Alelo e Nubank (pythonEndpoint especializado):
         *   { "action": "fill", "selector": "{{CPF}}",   "text": "12345678900" }
         *   { "action": "fill", "selector": "{{SENHA}}", "text": "minhasenha" }
         *
         * Para bancos genéricos (/capture):
         *   { "action": "fill", "selector": "input[name='cpf']", "text": "12345678900" }
         */
        List<NavigationStepDTO> navigationSteps,

        String description
) {}


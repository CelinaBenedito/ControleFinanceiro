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
         * Endpoint Python a chamar. Padrao: /capture.
         * Alelo usa /capture/alelo, Nubank usa /capture/nubank/sync.
         * O JS busca este valor via GET /instituicoes/{id}/ofx-config.
         */
        String pythonEndpoint,

        /**
         * Passos de navegacao. null = modo manual.
         * Para Alelo/Nubank com endpoint proprio: os steps carregam cpf/senha via fill.
         */
        List<NavigationStepDTO> navigationSteps,

        String descricao
) {}


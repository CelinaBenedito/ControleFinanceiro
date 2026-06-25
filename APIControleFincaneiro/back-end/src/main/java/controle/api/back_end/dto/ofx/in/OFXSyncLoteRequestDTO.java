package controle.api.back_end.dto.ofx.in;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Payload para sincronizar OFX de TODAS as instituições de um usuário de uma vez.
 *
 * -----------------------------------------------------------------------
 * RESPONSABILIDADE DO TIME DE FRONT-END / JS:
 * -----------------------------------------------------------------------
 * 1. Após o usuário selecionar/manter o perfil ativo, aguardar o debounce
 *    configurado (ex: 5 segundos) antes de chamar este endpoint.
 *
 * 2. Buscar as credenciais bancárias de cada instituição do localStorage
 *    (ou outro mecanismo seguro do lado do cliente).
 *
 * 3. Para cada instituição, montar o array navigationSteps com os passos
 *    de login + navegação até o botão de download do OFX.
 *    - Se navigationSteps for null, o browser abrirá em modo manual
 *      e aguardará o usuário fazer login.
 *
 * 4. Chamar POST /ofx/sync/usuario/{userId} com este payload.
 * -----------------------------------------------------------------------
 *
 * Exemplo de chamada JS:
 * <pre>
 * const payload = {
 *   instituicoes: [
 *     {
 *       instituicaoUsuarioId: 1,
 *       bankUrl: "https://banco.com.br/login",
 *       navigationSteps: [
 *         { action: "fill", selector: "input[name='cpf']",  text: credenciais.cpf },
 *         { action: "fill", selector: "input[name='senha']", text: credenciais.senha },
 *         { action: "click", selector: "button[type='submit']" },
 *         { action: "wait_for_selector", selector: "a[href*='ofx']", timeout: 30000 },
 *         { action: "click", selector: "a[href*='ofx']" }
 *       ]
 *     }
 *   ]
 * };
 * await fetch(`/ofx/sync/usuario/${userId}`, {
 *   method: 'POST',
 *   headers: { 'Content-Type': 'application/json' },
 *   body: JSON.stringify(payload)
 * });
 * </pre>
 */
public record OFXSyncLoteRequestDTO(
        @NotNull @NotEmpty List<OFXInstituicaoSyncDTO> instituicoes
) {}


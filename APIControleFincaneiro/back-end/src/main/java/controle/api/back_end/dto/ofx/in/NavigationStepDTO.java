package controle.api.back_end.dto.ofx.in;

/**
 * Representa um passo de navegação do browser (espelha o NavigationStepDTO do Python).
 *
 * Ações suportadas:
 *   click, fill, wait, wait_for_selector, navigate, download
 */
public record NavigationStepDTO(
        String action,
        String selector,
        String text,
        Integer timeout
) {}


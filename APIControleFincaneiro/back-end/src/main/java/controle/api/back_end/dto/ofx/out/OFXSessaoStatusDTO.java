package controle.api.back_end.dto.ofx.out;

/**
 * Status atual do processo Python (scraper).
 */
public record OFXSessaoStatusDTO(
        boolean pythonRodando,
        String mensagem,
        String pythonUrl
) {}


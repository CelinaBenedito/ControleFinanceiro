package controle.api.back_end.dto.emprestimo.bancario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ParcelaEmprestimoBancarioDTO(
        UUID eventoId,
        int numeroParcela,
        int totalParcelas,
        BigDecimal valor,
        LocalDate dataVencimento,
        String status, // "PAGA", "ATRASADA", "A_VENCER"
        String descricao,
        boolean podeSerPaga
) {}

